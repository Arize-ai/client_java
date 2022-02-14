package com.arize;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arize.protocol.Public.ScoreCategorical;
import com.arize.protocol.Public.Label;
import com.arize.protocol.Public.MultiValue;
import com.arize.protocol.Public.Value;

import org.junit.Before;
import org.junit.Test;

public class RecordUtilTest {

    protected Label binaryLabel, categoricalLabel, numericLabel, scoreCategoricalLabel;
    protected Map<String, Value> stringMap, intMap, doubleMap, longMap, multiMap, floatMap;

    @Before
    public void setup() {
        binaryLabel = Label.newBuilder().setBinary(true).build();
        categoricalLabel = Label.newBuilder().setCategorical("value").build();
        numericLabel = Label.newBuilder().setNumeric(20.20).build();

        ScoreCategorical.ScoreCategory.Builder scb = ScoreCategorical.ScoreCategory.newBuilder();
        scb.setCategory("apple");
        scb.setScore(3.14);
        scb.addAllNumericSequence(Arrays.asList(0.12, 0.23, 0.34));
        scoreCategoricalLabel = Label.newBuilder()
            .setScoreCategorical(ScoreCategorical.newBuilder().setScoreCategory(scb)).build();

        stringMap = new HashMap<>();
        intMap = new HashMap<>();
        doubleMap = new HashMap<>();
        longMap = new HashMap<>();
        multiMap = new HashMap<>();
        floatMap = new HashMap<>();

        stringMap.put("key", Value.newBuilder().setString("value").build());
        intMap.put("key", Value.newBuilder().setInt(2020).build());
        doubleMap.put("key", Value.newBuilder().setDouble(20.20d).build());
        longMap.put("key", Value.newBuilder().setInt(2020l).build());
        List<String> asList = Arrays.asList("first", "second");
        multiMap.put("key", Value.newBuilder().setMultiValue(MultiValue.newBuilder()
            .addAllValues(asList).build()).build());
        floatMap.put("key", Value.newBuilder().setDouble(20.2f).build());
    }

    @Test
    public void testConvertLabel() {
        assertEquals(binaryLabel, RecordUtil.convertLabel(true));
        assertEquals(categoricalLabel, RecordUtil.convertLabel("value"));
        assertEquals(numericLabel, RecordUtil.convertLabel(20.20));
        assertEquals(scoreCategoricalLabel, RecordUtil
            .convertLabel(new ArizeClient.ScoredCategorical("apple", 3.14, Arrays.asList(0.12, 0.23, 0.34))));
    }

    @Test
    public void testConvertFeatures() {

        Map<String, Integer> intFeatures = new HashMap<>();
        Map<String, Long> longFeatures = new HashMap<>();
        Map<String, String> stringFeatures = new HashMap<>();
        Map<String, Double> doubleFeatures = new HashMap<>();
        Map<String, Float> floatFeatures = new HashMap<>();
        intFeatures.put("key", 2020);
        longFeatures.put("key", 2020l);
        stringFeatures.put("key", "value");
        doubleFeatures.put("key", 20.20d);
        floatFeatures.put("key", 20.2f);

        assertEquals(stringMap, RecordUtil.convertFeatures(stringFeatures));
        assertEquals(intMap, RecordUtil.convertFeatures(intFeatures));
        assertEquals(longMap, RecordUtil.convertFeatures(longFeatures));
        assertEquals(doubleMap, RecordUtil.convertFeatures(doubleFeatures));
        assertEquals(floatMap, RecordUtil.convertFeatures(floatFeatures));
    }

    @Test
    public void testMultiValueFeatures() {

        Map<String, Integer> intFeatures = new HashMap<>();
        Map<String, Long> longFeatures = new HashMap<>();
        Map<String, String> stringFeatures = new HashMap<>();
        Map<String, Double> doubleFeatures = new HashMap<>();
        Map<String, List<String>> multivalueFeature = new HashMap<>();
        intFeatures.put("key", 2020);
        longFeatures.put("key", 2020l);
        stringFeatures.put("key", "value");
        doubleFeatures.put("key", 20.20d);
        multivalueFeature.put("key", Arrays.asList("first", "second"));

        assertEquals(stringMap, RecordUtil.convertFeatures(stringFeatures));
        assertEquals(intMap, RecordUtil.convertFeatures(intFeatures));
        assertEquals(doubleMap, RecordUtil.convertFeatures(doubleFeatures));
        assertEquals(longMap, RecordUtil.convertFeatures(longFeatures));
        assertEquals(multiMap, RecordUtil.convertFeatures(multivalueFeature));
    }
}