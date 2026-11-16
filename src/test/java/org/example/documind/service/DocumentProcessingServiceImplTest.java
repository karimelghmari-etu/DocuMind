package org.example.documind.service;

import org.example.documind.dao.DocumentChunkRepository;
import org.example.documind.dao.DocumentRepository;
import org.example.documind.dao.ProcessingJobRepository;
import org.example.documind.dao.UserRepository;
import org.example.documind.domain.entity.Document;
import org.example.documind.domain.entity.DocumentChunk;
import org.example.documind.domain.entity.ProcessingJob;
import org.example.documind.domain.entity.User;
import org.example.documind.domain.enums.DocumentStatus;
import org.example.documind.domain.enums.ProcessingStatus;
import org.example.documind.service.interfaces.DocumentProcessingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: real Spring context, real (in-memory H2) database, real files on disk.
 *
 * Deliberately NOT @Transactional: the service opens and commits its own transactions
 * (start / finish / fail). A test-level transaction would swallow them and hide bugs,
 * so the test cleans up after itself in @AfterEach instead.
 */
@SpringBootTest
class DocumentProcessingServiceImplTest {

    @Autowired private DocumentProcessingService processingService;
    @Autowired private ProcessingJobRepository jobRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentChunkRepository chunkRepository;
    @Autowired private UserRepository userRepository;

    @TempDir
    Path tempDir;

    private User owner;

    /** The ids a test needs to look things up again after processing. */
    private record Ids(Long jobId, Long documentId) {}

    @BeforeEach
    void createOwner() {
        String name = "proc-" + UUID.randomUUID().toString().substring(0, 8);
        owner = userRepository.save(User.builder()
                .username(name)
                .email(name + "@example.com")
                .password("irrelevant")
                .build());
    }

    @AfterEach
    void cleanUp() {
        chunkRepository.deleteAll();
        jobRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.delete(owner);
    }

    // ---------- helpers ----------

    /** Saves a Document (PENDING) and a ProcessingJob with the given status, like an upload does. */
    private Ids createJob(String filePath, ProcessingStatus jobStatus) {
        Document document = documentRepository.save(Document.builder()
                .title("Test doc")
                .filename("test.txt")
                .filePath(filePath)
                .contentType("text/plain")
                .fileSize(1L)
                .owner(owner)
                .build());

        ProcessingJob job = jobRepository.save(ProcessingJob.builder()
                .document(document)
                .status(jobStatus)
                .build());

        return new Ids(job.getId(), document.getId());
    }

    private Path writeFile(String name, String content) throws IOException {
        return Files.writeString(tempDir.resolve(name), content);
    }

    /** About 6,000 characters of unique words, so the default size (1000) gives several chunks. */
    private static String longText() {
        return IntStream.range(0, 1200).mapToObj(i -> "w" + i).collect(Collectors.joining(" "));
    }

    private ProcessingJob reloadJob(Ids ids) {
        return jobRepository.findById(ids.jobId()).orElseThrow();
    }

    private Document reloadDocument(Ids ids) {
        return documentRepository.findById(ids.documentId()).orElseThrow();
    }

    private List<DocumentChunk> chunksOf(Ids ids) {
        return chunkRepository.findByDocumentId(ids.documentId()).stream()
                .sorted(Comparator.comparing(DocumentChunk::getChunkIndex))
                .toList();
    }

    // ---------- success ----------

    @Test
    void processesFileIntoChunksAndMarksDone() throws IOException {
        Ids ids = createJob(writeFile("long.txt", longText()).toString(), ProcessingStatus.QUEUED);

        processingService.processJob(ids.jobId());

        ProcessingJob job = reloadJob(ids);
        assertEquals(ProcessingStatus.COMPLETED, job.getStatus());
        assertNull(job.getErrorMessage());
        assertEquals(DocumentStatus.INDEXED, reloadDocument(ids).getStatus());

        List<DocumentChunk> chunks = chunksOf(ids);
        assertTrue(chunks.size() > 1, "expected several chunks, got " + chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).getChunkIndex(), "chunk indexes should be 0, 1, 2, ...");
        }
        assertTrue(chunks.get(0).getContent().startsWith("w0 "));
        assertTrue(chunks.get(chunks.size() - 1).getContent().endsWith("w1199"));
    }

    @Test
    void shortFileGivesExactlyOneChunk() throws IOException {
        Ids ids = createJob(writeFile("short.txt", "Just one short sentence.").toString(), ProcessingStatus.QUEUED);

        processingService.processJob(ids.jobId());

        List<DocumentChunk> chunks = chunksOf(ids);
        assertEquals(1, chunks.size());
        assertEquals("Just one short sentence.", chunks.get(0).getContent());
    }

    @Test
    void reprocessingDoesNotDuplicateChunks() throws IOException {
        Ids ids = createJob(writeFile("long.txt", longText()).toString(), ProcessingStatus.QUEUED);
        processingService.processJob(ids.jobId());
        int firstCount = chunksOf(ids).size();

        // put the job back in the queue and run it again
        ProcessingJob job = reloadJob(ids);
        job.setStatus(ProcessingStatus.QUEUED);
        jobRepository.save(job);
        processingService.processJob(ids.jobId());

        assertEquals(firstCount, chunksOf(ids).size());
    }

    // ---------- failures ----------

    @Test
    void missingFileMarksJobAndDocumentFailed() {
        Ids ids = createJob(tempDir.resolve("deleted.pdf").toString(), ProcessingStatus.QUEUED);

        processingService.processJob(ids.jobId());   // must not throw

        ProcessingJob job = reloadJob(ids);
        assertEquals(ProcessingStatus.FAILED, job.getStatus());
        assertNotNull(job.getErrorMessage());
        assertFalse(job.getErrorMessage().isBlank());
        assertEquals(DocumentStatus.FAILED, reloadDocument(ids).getStatus());
        assertTrue(chunksOf(ids).isEmpty());
    }

    @Test
    void fileWithoutTextStoresTheReason() throws IOException {
        Ids ids = createJob(writeFile("blank.txt", "   \n\n  ").toString(), ProcessingStatus.QUEUED);

        processingService.processJob(ids.jobId());

        ProcessingJob job = reloadJob(ids);
        assertEquals(ProcessingStatus.FAILED, job.getStatus());
        assertEquals("No text could be extracted", job.getErrorMessage());
        assertEquals(DocumentStatus.FAILED, reloadDocument(ids).getStatus());
    }

    // ---------- guard rails ----------

    @Test
    void jobThatIsNotQueuedIsLeftAlone() throws IOException {
        Ids ids = createJob(writeFile("long.txt", longText()).toString(), ProcessingStatus.COMPLETED);

        processingService.processJob(ids.jobId());

        assertEquals(ProcessingStatus.COMPLETED, reloadJob(ids).getStatus());
        assertEquals(DocumentStatus.PENDING, reloadDocument(ids).getStatus(), "document must not be touched");
        assertTrue(chunksOf(ids).isEmpty(), "no chunks should be created");
    }

    @Test
    void unknownJobIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> processingService.processJob(999_999L));
    }
}
