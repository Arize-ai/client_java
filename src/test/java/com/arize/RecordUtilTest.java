package com.arize;

import com.arize.protocol.Public.Label;
import com.arize.protocol.Public.MultiValue;
import com.arize.protocol.Public.ScoreCategorical;
import com.arize.protocol.Public.Value;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RecordUtilTest {

    protected Label binaryLabel, categoricalLabel, numericLabel, scoreCategoricalLabel;
    protected Map<String, Value> objMap;

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

        List<String> asList = Arrays.asList("first", "second");

        objMap = new HashMap<>();
        objMap.put("string", Value.newBuilder().setString("value").build());
        objMap.put("int", Value.newBuilder().setInt(2020).build());
        objMap.put("double", Value.newBuilder().setDouble(20.20d).build());
        objMap.put("long", Value.newBuilder().setInt(2020L).build());
        objMap.put("list", Value.newBuilder().setMultiValue(MultiValue.newBuilder()
                .addAllValues(asList).build()).build());
        objMap.put("float", Value.newBuilder().setDouble(20.2f).build());
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
        Map<String, Object> features = getDimensionMap();
        assertEquals(objMap, RecordUtil.convertFeatures(features));
    }

    @Test
    public void testConvertTags() {
        Map<String, Object> tags = getDimensionMap();
        assertEquals(objMap, RecordUtil.convertTags(tags));
    }

    private static Map<String, Object> getDimensionMap() {
        Map<String, Object> dims = new HashMap<>();
        dims.put("int", 2020);
        dims.put("long", 2020L);
        dims.put("string", "value");
        dims.put("double", 20.20d);
        dims.put("float", 20.2f);
        dims.put("list", Arrays.asList("first", "second"));
        return dims;
    }
}
