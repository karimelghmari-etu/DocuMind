package org.example.documind.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.example.documind.service.impl.TextExtractionServiceImpl;
import org.example.documind.service.interfaces.TextExtractionService;
import org.example.documind.web.exception.TextExtractionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plain unit tests: no Spring context, the service is created directly.
 * Test files are generated into a temporary folder that JUnit deletes afterwards.
 */
class TextExtractionServiceImplTest {

    private final TextExtractionService service = new TextExtractionServiceImpl();

    @TempDir
    Path tempDir;

    // ---------- supported formats ----------

    @Test
    void extractsTextFromPlainTextFile() {
        String text = service.extractText("postman/fixtures/sample.txt");

        assertTrue(text.contains("DocuMind sample document"));
    }

    @Test
    void extractsTextFromPdf() throws IOException {
        Path pdf = tempDir.resolve("sample.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("Hello from a PDF");
                content.endText();
            }
            document.save(pdf.toFile());
        }

        String text = service.extractText(pdf.toString());

        assertTrue(text.contains("Hello from a PDF"), "got: " + text);
    }

    @Test
    void extractsTextFromDocx() throws IOException {
        Path docx = tempDir.resolve("sample.docx");
        try (XWPFDocument document = new XWPFDocument();
             OutputStream out = Files.newOutputStream(docx)) {
            document.createParagraph().createRun().setText("Hello from a DOCX");
            document.write(out);
        }

        String text = service.extractText(docx.toString());

        assertTrue(text.contains("Hello from a DOCX"), "got: " + text);
    }

    // ---------- output cleanup ----------

    @Test
    void stripsSurroundingWhitespace() throws IOException {
        Path file = Files.writeString(tempDir.resolve("padded.txt"), "\n\n   hello world   \n\n");

        assertEquals("hello world", service.extractText(file.toString()));
    }

    // ---------- failures ----------

    @Test
    void emptyFileThrows() throws IOException {
        // Tika itself rejects a 0-byte file (ZeroByteFileException), so this goes through the catch block.
        Path empty = Files.createFile(tempDir.resolve("empty.txt"));

        assertThrows(TextExtractionException.class, () -> service.extractText(empty.toString()));
    }

    @Test
    void whitespaceOnlyFileThrowsNoTextMessage() throws IOException {
        // Tika parses this fine but returns only whitespace, so this hits the isBlank() check.
        Path blank = Files.writeString(tempDir.resolve("blank.txt"), "   \n\n\t  ");

        TextExtractionException e = assertThrows(TextExtractionException.class,
                () -> service.extractText(blank.toString()));

        assertEquals("No text could be extracted", e.getMessage());
    }

    @Test
    void missingFileThrowsAndKeepsCause() {
        TextExtractionException e = assertThrows(TextExtractionException.class,
                () -> service.extractText(tempDir.resolve("does-not-exist.pdf").toString()));

        assertNotNull(e.getCause(), "the original IOException should be kept as the cause");
    }
}
