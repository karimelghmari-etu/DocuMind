package org.example.documind.evaluation;

import org.example.documind.service.impl.ChunkingServiceImpl;
import org.example.documind.service.impl.EmbeddingServiceImpl;
import org.example.documind.service.interfaces.ChunkingService;
import org.example.documind.service.interfaces.EmbeddingService;
import org.example.documind.service.util.VectorMath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SearchEvaluationTest {
    private static final Path EVAL_DIR = Path.of("src/test/resources/eval");
    private static final EmbeddingService embeddingService = new EmbeddingServiceImpl();

    private record Question(String text, String document, String answer){}
    private record IndexedChunk(String document, String content, float[] vector){}
    private record Result(double recallAt1, double recallAt5){}

    @Value("${app.chunking.size}")
    private int size;

    @Value("${app.chunking.overlap}")
    private int overlap;

    private static List<Question> loadQuestions() throws IOException {
        List<Question> questions = new ArrayList<>();

        for (String line : Files.readAllLines(EVAL_DIR.resolve("questions.txt"))){
            if (line.isBlank() || line.startsWith("#")){
                continue;
            }
            String[] parts = line.split("\\|");

            questions.add(new Question(parts[0].strip(), parts[1].strip(), parts[2].strip()));
        }

        return questions;
    }

    private static List<IndexedChunk> buildIndex(List<Question> questions, int size, int overlap) throws IOException {
        ChunkingService chunkingService = new ChunkingServiceImpl(size, overlap);
        List<IndexedChunk> index = new ArrayList<>();

        List<String> documents = questions.stream().map(Question::document).distinct().toList();

        for (String document : documents){
            String text = Files.readString(EVAL_DIR.resolve(document));
            List<String> chunks = chunkingService.chunk(text);
            List<float[]> vectors = embeddingService.embedAll(chunks);

            for (int i = 0; i < chunks.size(); i++){
                index.add(new IndexedChunk(document, chunks.get(i), vectors.get(i)));
            }
        }
        return index;
    }

    private static int rankOf(Question question, List<IndexedChunk> index){
        float[] queryVector = embeddingService.embed(question.text());

        List<IndexedChunk> top5 = index.stream()
                .sorted(Comparator.comparingDouble((IndexedChunk chunk) -> VectorMath.cosineSimilarity(queryVector, chunk.vector())).reversed())
                .limit(5)
                .toList();

        for (int i = 0; i < top5.size(); i++){
            if (top5.get(i).document().equals(question.document()) && top5.get(i).content().contains(question.answer())){
                return i+1;
            }
        }
        return -1;
    }

    private static Result evaluate(int size, int overlap, boolean printDetails) throws IOException{
        List<Question> questions = loadQuestions();
        List<IndexedChunk> index = buildIndex(questions, size, overlap);

        int top1 = 0;
        int top5 = 0;

        for (Question question : questions){
            int rank = rankOf(question, index);
            if (rank == 1){
                top1++;
            }
            if (rank >= 1){
                top5++;
                if (printDetails && rank != 1) {
                    System.out.println("rank " + rank + ": " + question.text());
                }
            } else if (printDetails){
                System.out.println("Missed: " + question.text());
            }
        }
        return new Result((double) top1 / questions.size(), (double) top5 / questions.size());
    }

    @Test
    void compareChunkSizes() throws IOException {
        int[][] settings = { {1500, 300}, {1000, 200}, {750, 150}, {500, 100}, {300, 50} };

        for (int[] s : settings) {
            Result r = evaluate(s[0], s[1], false);
            System.out.printf("size %4d / overlap %3d → recall@1: %3.0f%%  recall@5: %3.0f%%%n",
                    s[0], s[1], r.recallAt1() * 100, r.recallAt5() * 100);
        }
    }

    @Test
    void searchQualityDoesNotRegress() throws IOException {
        Result result = evaluate(size, overlap, true);
        System.out.printf("size " + size + " / overlap " + overlap + " → recall@1: %.0f%%  recall@5: %.0f%%%n",
                result.recallAt1() * 100, result.recallAt5() * 100);

        assertTrue(result.recallAt5() >= 0.95, "recall@5 dropped to " + result.recallAt5());
        assertTrue(result.recallAt1() >= 0.80, "recall@1 dropped to " + result.recallAt1());
    }
}
