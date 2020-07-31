package com.arize;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import com.arize.protocol.Public.Label;
import com.arize.protocol.Public.Value;

import org.junit.Before;
import org.junit.Test;

public class RecordUtilTest {

    protected Label binaryLabel, categoricalLabel, numericLabel;
    protected Map<String, Value> stringMap, intMap, doubleMap, longMap;

    @Before
    public void setup() {
        binaryLabel = Label.newBuilder().setBinary(true).build();
        categoricalLabel = Label.newBuilder().setCategorical("value").build();
        numericLabel = Label.newBuilder().setNumeric(20.20).build();

        stringMap = new HashMap<>();
        intMap = new HashMap<>();
        doubleMap = new HashMap<>();
        longMap = new HashMap<>();

        stringMap.put("key", Value.newBuilder().setString("value").build());
        intMap.put("key", Value.newBuilder().setInt(2020).build());
        doubleMap.put("key", Value.newBuilder().setDouble(20.20).build());
        longMap.put("key", Value.newBuilder().setInt((long) 2020).build());
    }

    @Test
    public void testConvertLabel() {
        assertEquals(binaryLabel, RecordUtil.convertLabel(true));
        assertEquals(categoricalLabel, RecordUtil.convertLabel("value"));
        assertEquals(numericLabel, RecordUtil.convertLabel(20.20));
    }

    @Test
    public void testConvertFeatures() {

        Map<String, Integer> intFeatures = new HashMap<>();
        Map<String, Long> longFeatures = new HashMap<>();
        Map<String, String> stringFeatures = new HashMap<>();
        Map<String, Double> doubleFeatures = new HashMap<>();
        intFeatures.put("key", 2020);
        longFeatures.put("key", (long) 2020);
        stringFeatures.put("key", "value");
        doubleFeatures.put("key", 20.20);

        assertEquals(stringMap, RecordUtil.convertFeatures(stringFeatures));
        assertEquals(intMap, RecordUtil.convertFeatures(intFeatures));
        assertEquals(doubleMap, RecordUtil.convertFeatures(doubleFeatures));
        assertEquals(longMap, RecordUtil.convertFeatures(longFeatures));
    }
}