package org.example.documind.service.impl;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.example.documind.service.interfaces.TextExtractionService;
import org.example.documind.web.exception.TextExtractionException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class TextExtractionServiceImpl implements TextExtractionService {
    private final Tika tika;

    public TextExtractionServiceImpl(){
        this.tika = new Tika();
        this.tika.setMaxStringLength(-1);
    }

    @Override
    public String extractText(String filePath) {
        String text;
        try{
            text = tika.parseToString(Path.of(filePath));
        } catch (IOException | TikaException e){
            throw new TextExtractionException("Text Extraction failed", e);
        }

        if (text == null || text.isBlank()){
            throw new TextExtractionException("No text could be extracted");
        }

        return text.strip();
    }
}
