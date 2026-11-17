package org.example.documind.domain.converter;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Plain unit tests: the converter is a simple class, no Spring or database needed. */
class FloatArrayConverterTest {

    private final FloatArrayConverter converter = new FloatArrayConverter();

    /** Save to the database format and load it back. */
    private float[] roundTrip(float[] vector) {
        return converter.convertToEntityAttribute(converter.convertToDatabaseColumn(vector));
    }

    // ---------- saving ----------

    @Test
    void writesNumbersSeparatedByCommas() {
        assertEquals("0.5,-1.25,3.0", converter.convertToDatabaseColumn(new float[]{0.5f, -1.25f, 3.0f}));
    }

    @Test
    void singleNumberHasNoComma() {
        assertEquals("42.0", converter.convertToDatabaseColumn(new float[]{42f}));
    }

    // ---------- loading ----------

    @Test
    void readsNumbersBack() {
        assertArrayEquals(new float[]{0.5f, -1.25f, 3.0f}, converter.convertToEntityAttribute("0.5,-1.25,3.0"));
    }

    // ---------- round trip ----------

    @Test
    void roundTripIsExactForARealSizedVector() {
        // 384 random numbers, like a real embedding; exact equality (no delta) on purpose
        Random random = new Random(7);
        float[] vector = new float[384];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = random.nextFloat() * 2 - 1;
        }

        assertArrayEquals(vector, roundTrip(vector));
    }

    @Test
    void roundTripKeepsTinyAndHugeNumbers() {
        // Float.toString writes these in scientific notation (e.g. 1.0E-8), which must still parse back
        float[] vector = {1e-8f, -3.4e38f, Float.MIN_VALUE, 0.0f, -0.0f};

        assertArrayEquals(vector, roundTrip(vector));
    }

    // ---------- null and empty ----------

    @Test
    void nullVectorIsStoredAsNull() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    void nullOrBlankColumnIsLoadedAsNull() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute(""));
        assertNull(converter.convertToEntityAttribute("   "));
    }
}
