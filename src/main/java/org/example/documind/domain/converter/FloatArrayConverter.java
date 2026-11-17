package org.example.documind.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.StringJoiner;

@Converter
public class FloatArrayConverter implements AttributeConverter<float[], String> {
    @Override
    public String convertToDatabaseColumn(float[] vector) {
        if (vector == null){
            return null;
        }
        StringJoiner joiner =  new StringJoiner(",");
        for (float val : vector){
            joiner.add(String.valueOf(val));
        }
        return joiner.toString();
    }

    @Override
    public float[] convertToEntityAttribute(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        String[] parts = text.split(",");

        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }
}
