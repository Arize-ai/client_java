package com.arize;

import com.arize.protocol.Public;
import com.arize.protocol.Public.Embedding;
import com.arize.protocol.Public.Label;
import com.arize.protocol.Public.PredictionLabel;
import com.arize.protocol.Public.ActualLabel;
import com.arize.protocol.Public.MultiValue;
import com.arize.protocol.Public.ScoreCategorical;
import com.arize.protocol.Public.Value;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RecordUtilTest {

    protected PredictionLabel binaryPredLabel, categoricalPredLabel, numericPredLabel, scoreCategoricalPredLabel;
    protected ActualLabel binaryActLabel, categoricalActLabel, numericActLabel, scoreCategoricalActLabel;
    protected Map<String, Value> objMap;
    protected Map<String, Value> embeddingMap, embeddingMapNullRawData, embeddingMapNullLinkToData;

    @Before
    public void setup() {
        binaryPredLabel = PredictionLabel.newBuilder().setBinary(true).build();
        ScoreCategorical.ScoreCategory.Builder stringScb = ScoreCategorical.ScoreCategory.newBuilder();
        stringScb.setCategory("prediction");
        categoricalPredLabel = PredictionLabel.newBuilder().setScoreCategorical(ScoreCategorical.newBuilder().setScoreCategory(stringScb)).build();
        numericPredLabel = PredictionLabel.newBuilder().setNumeric(20.20).build();

        ScoreCategorical.ScoreCategory.Builder scb = ScoreCategorical.ScoreCategory.newBuilder();
        scb.setCategory("apple");
        scb.setScore(3.14);
        scb.addAllNumericSequence(Arrays.asList(0.12, 0.23, 0.34));
        scoreCategoricalPredLabel =
                PredictionLabel.newBuilder()
                        .setScoreCategorical(ScoreCategorical.newBuilder().setScoreCategory(scb))
                        .build();

        binaryActLabel = ActualLabel.newBuilder().setBinary(false).build();
        ScoreCategorical.ScoreCategory.Builder stringActScb = ScoreCategorical.ScoreCategory.newBuilder();
        stringActScb.setCategory("actual");
        categoricalActLabel = ActualLabel.newBuilder().setScoreCategorical(ScoreCategorical.newBuilder().setScoreCategory(stringActScb)).build();
        numericActLabel = ActualLabel.newBuilder().setNumeric(20.20).build();

        ScoreCategorical.ScoreCategory.Builder scbAct = ScoreCategorical.ScoreCategory.newBuilder();
        scbAct.setCategory("pear");
        scbAct.setScore(4.17);
        scbAct.addAllNumericSequence(Arrays.asList(0.12, 0.23, 0.34));
        scoreCategoricalActLabel =
                ActualLabel.newBuilder()
                        .setScoreCategorical(ScoreCategorical.newBuilder().setScoreCategory(scbAct))
                        .build();

        List<String> asList = Arrays.asList("first", "second");

        objMap = new HashMap<>();
        objMap.put("string", Value.newBuilder().setString("value").build());
        objMap.put("int", Value.newBuilder().setInt(2020).build());
        objMap.put("double", Value.newBuilder().setDouble(20.20d).build());
        objMap.put("long", Value.newBuilder().setInt(2020L).build());
        objMap.put(
                "list",
                Value.newBuilder()
                        .setMultiValue(MultiValue.newBuilder().addAllValues(asList).build())
                        .build());
        objMap.put("float", Value.newBuilder().setDouble(20.2f).build());

        embeddingMap = new HashMap<>();
        List<Double> vectors = new ArrayList<Double>();
        vectors.add(1.0);
        vectors.add(2.0);
        List<String> rawData = new ArrayList<String>();
        rawData.add("test");
        rawData.add("tokens");
        Embedding embedding =
                Embedding.newBuilder()
                        .addAllVector(vectors)
                        .setRawData(
                                Embedding.RawData.newBuilder()
                                        .setTokenArray(Embedding.TokenArray.newBuilder().addAllTokens(rawData)))
                        .setLinkToData(StringValue.newBuilder().setValue("https://test.com/hey.jpg"))
                        .build();
        embeddingMap.put("embedding", Value.newBuilder().setEmbedding(embedding).build());

        embeddingMapNullRawData = new HashMap<>();
        Embedding embeddingNullRawData =
                Embedding.newBuilder()
                        .addAllVector(vectors)
                        .setLinkToData(StringValue.newBuilder().setValue(""))
                        .build();
        embeddingMapNullRawData.put("embedding", Value.newBuilder().setEmbedding(embeddingNullRawData).build());

        embeddingMapNullLinkToData = new HashMap<>();
        Embedding embeddingNullLinkToData =
                Embedding.newBuilder()
                        .addAllVector(vectors)
                        .setRawData(
                                Embedding.RawData.newBuilder()
                                        .setTokenArray(Embedding.TokenArray.newBuilder().addAllTokens(rawData)))
                        .build();
        embeddingMapNullLinkToData.put("embedding", Value.newBuilder().setEmbedding(embeddingNullLinkToData).build());
    }

    @Test
    public void testConvertPredictionLabel() {
        assertEquals(binaryPredLabel, RecordUtil.convertPredictionLabel(true));
        assertEquals(categoricalPredLabel, RecordUtil.convertPredictionLabel("prediction"));
        assertEquals(numericPredLabel, RecordUtil.convertPredictionLabel(20.20));
        assertEquals(
                scoreCategoricalPredLabel,
                RecordUtil.convertPredictionLabel(
                        new ArizeClient.ScoredCategorical("apple", 3.14, Arrays.asList(0.12, 0.23, 0.34))));
    }

    @Test
    public void testConvertActualLabel() {
        assertEquals(binaryActLabel, RecordUtil.convertActualLabel(false));
        assertEquals(categoricalActLabel, RecordUtil.convertActualLabel("actual"));
        assertEquals(numericActLabel, RecordUtil.convertActualLabel(20.20));
        assertEquals(
                scoreCategoricalActLabel,
                RecordUtil.convertActualLabel(
                        new ArizeClient.ScoredCategorical("pear", 4.17, Arrays.asList(0.12, 0.23, 0.34))));
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

    @Test
    public void testConvertEmbeddingFeatures() {
        Map<String, Object> embeddingFeatures = getEmbeddingDimensionMap();
        assertEquals(embeddingMap, RecordUtil.convertEmbeddingFeatures(embeddingFeatures));
    }

    @Test
    public void testConvertValidEmbeddingFeaturesEmptyVector() {
        Map<String, Object> embeddingFeatures = getInvalidEmbeddingDimensionMapEmptyVector();
        try {
            RecordUtil.convertEmbeddingFeatures(embeddingFeatures);
        } catch (IllegalArgumentException expectedException) {
            fail();
        }
    }

    @Test
    public void testConvertValidEmbeddingFeaturesNullVector() {
        Map<String, Object> embeddingFeatures = getInvalidEmbeddingDimensionMapNullVector();
        try {
            RecordUtil.convertEmbeddingFeatures(embeddingFeatures);
        } catch (IllegalArgumentException expectedException) {
            fail();
        }
    }

    @Test
    public void testConvertEmbeddingFeaturesNullRawData() {
        Map<String, Object> embeddingFeatures = getEmbeddingDimensionMapNullRawData();
        assertEquals(embeddingMapNullRawData, RecordUtil.convertEmbeddingFeatures(embeddingFeatures));
    }

    @Test
    public void testConvertEmbeddingFeaturesNullLinkToData() {
        Map<String, Object> embeddingFeatures = getEmbeddingDimensionMapNullLinkToData();
        assertEquals(embeddingMapNullLinkToData, RecordUtil.convertEmbeddingFeatures(embeddingFeatures));
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

    private static Map<String, Object> getEmbeddingDimensionMap() {
        Map<String, Object> dims = new HashMap<>();
        List<Double> vectors = new ArrayList<Double>();
        vectors.add(1.0);
        vectors.add(2.0);
        List<String> rawData = new ArrayList<String>();
        rawData.add("test");
        rawData.add("tokens");
        dims.put(
                "embedding", new com.arize.types.Embedding(vectors, rawData, "https://test.com/hey.jpg"));
        return dims;
    }

    private static Map<String, Object> getInvalidEmbeddingDimensionMapEmptyVector() {
        Map<String, Object> dims = new HashMap<>();
        List<Double> vectors = new ArrayList<Double>();
        dims.put("embedding", new com.arize.types.Embedding(vectors, null, null));
        return dims;
    }

    private static Map<String, Object> getInvalidEmbeddingDimensionMapNullVector() {
        Map<String, Object> dims = new HashMap<>();
        dims.put("embedding", new com.arize.types.Embedding(null, null, null));
        return dims;
    }

    private static Map<String, Object> getEmbeddingDimensionMapNullRawData() {
        Map<String, Object> dims = new HashMap<>();
        List<Double> vectors = new ArrayList<Double>();
        vectors.add(1.0);
        vectors.add(2.0);
        dims.put(
                "embedding", new com.arize.types.Embedding(vectors, null, ""));
        return dims;
    }

    private static Map<String, Object> getEmbeddingDimensionMapNullLinkToData() {
        Map<String, Object> dims = new HashMap<>();
        List<Double> vectors = new ArrayList<Double>();
        vectors.add(1.0);
        vectors.add(2.0);

        List<String> rawData = new ArrayList<String>();
        rawData.add("test");
        rawData.add("tokens");

        dims.put(
                "embedding", new com.arize.types.Embedding(vectors, rawData, null));
        return dims;
    }
}
