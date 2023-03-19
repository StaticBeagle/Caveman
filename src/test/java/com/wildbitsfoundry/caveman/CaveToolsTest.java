package com.wildbitsfoundry.caveman;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CaveToolsTest {

    @Test
    public void testDoubleToString() {
        String result = CaveTools.ToString(5);
        assertEquals("5", result);
    }

    @Test
    public void testDoubleToStringPositiveInfinity() {
        String result = CaveTools.ToString(Double.POSITIVE_INFINITY);
        assertEquals("Infinity", result);
    }

    @Test
    public void testDoubleToStringNegativeInfinity() {
        String result = CaveTools.ToString(Double.NEGATIVE_INFINITY);
        assertEquals("-Infinity", result);
    }

    @Test
    public void testDoubleToStringNaN() {
        String result = CaveTools.ToString(Double.NaN);
        assertEquals("NaN", result);
    }
}
