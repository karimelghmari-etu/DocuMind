package org.example.documind.service;

import org.example.documind.service.impl.ChunkingServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain unit tests with small numbers so the chunks are easy to reason about.
 * OVERLAP is deliberately NOT a multiple of the word length ("w123 " = 5 chars),
 * otherwise "end - overlap" always lands on a space by luck and a cut-word bug stays hidden.
 */
class ChunkingServiceImplTest {

    private static final int SIZE = 100;
    private static final int OVERLAP = 23;

    private final ChunkingServiceImpl service = new ChunkingServiceImpl(SIZE, OVERLAP);

    /** "w0 w1 w2 ... w{n-1}" - every word is unique, so a cut word is easy to detect. */
    private static String numberedWords(int count) {
        return IntStream.range(0, count).mapToObj(i -> "w" + i).collect(Collectors.joining(" "));
    }

    private static Set<String> wordsOf(String text) {
        return new HashSet<>(Arrays.asList(text.split("\\s+")));
    }

    // ---------- trivial input ----------

    @Test
    void nullTextGivesNoChunks() {
        assertTrue(service.chunk(null).isEmpty());
    }

    @Test
    void blankTextGivesNoChunks() {
        assertTrue(service.chunk("   \n\t ").isEmpty());
    }

    @Test
    void shortTextIsOneChunk() {
        String text = "A short document.";

        assertEquals(List.of(text), service.chunk(text));
    }

    // ---------- long input ----------

    @Test
    void longTextIsSplitIntoSeveralChunks() {
        assertTrue(service.chunk(numberedWords(300)).size() > 1);
    }

    @Test
    void noChunkIsLongerThanChunkSize() {
        for (String chunk : service.chunk(numberedWords(300))) {
            assertTrue(chunk.length() <= SIZE, "chunk too long (" + chunk.length() + "): " + chunk);
        }
    }

    @Test
    void noChunkIsBlank() {
        for (String chunk : service.chunk(numberedWords(300))) {
            assertFalse(chunk.isBlank());
        }
    }

    @Test
    void noWordIsCutInHalf() {
        String text = numberedWords(300);
        Set<String> validWords = wordsOf(text);

        for (String chunk : service.chunk(text)) {
            for (String word : chunk.split("\\s+")) {
                assertTrue(validWords.contains(word), "cut word '" + word + "' in chunk: " + chunk);
            }
        }
    }

    @Test
    void noTextIsLost() {
        String text = numberedWords(300);
        Set<String> seen = new HashSet<>();
        service.chunk(text).forEach(chunk -> seen.addAll(wordsOf(chunk)));

        Set<String> missing = new HashSet<>(wordsOf(text));
        missing.removeAll(seen);
        assertTrue(missing.isEmpty(), "words missing from every chunk: " + missing);
    }

    @Test
    void consecutiveChunksOverlap() {
        List<String> chunks = service.chunk(numberedWords(300));

        for (int i = 0; i < chunks.size() - 1; i++) {
            String firstWordOfNext = chunks.get(i + 1).split("\\s+")[0];
            assertTrue(wordsOf(chunks.get(i)).contains(firstWordOfNext),
                    "chunk " + (i + 1) + " should start with text repeated from chunk " + i);
        }
    }

    @Test
    void wordLongerThanChunkSizeIsStillChunked() {
        String text = "x".repeat(250);

        List<String> chunks = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> service.chunk(text));

        assertTrue(chunks.size() > 1);
        chunks.forEach(chunk -> assertTrue(chunk.length() <= SIZE));
    }

    @Test
    void alwaysTerminatesWithLargeOverlap() {
        // Words of 40 characters with overlap 80 used to make "start" stop moving forward.
        ChunkingServiceImpl largeOverlap = new ChunkingServiceImpl(100, 80);
        String text = ("x".repeat(40) + " ").repeat(20);

        List<String> chunks = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> largeOverlap.chunk(text));

        assertFalse(chunks.isEmpty());
    }

    // ---------- whitespace cleanup ----------

    @Test
    void runsOfBlankLinesAreCollapsed() {
        assertEquals(List.of("first\n\nsecond"), service.chunk("first\n\n\n\n\nsecond"));
    }

    // ---------- configuration checks ----------

    @Test
    void overlapEqualToSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkingServiceImpl(100, 100));
    }

    @Test
    void overlapLargerThanSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkingServiceImpl(100, 150));
    }

    @Test
    void negativeOverlapIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkingServiceImpl(100, -1));
    }

    @Test
    void zeroSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkingServiceImpl(0, 0));
    }
}
