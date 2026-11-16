package com.arize;

import com.arize.protocol.Public;
import com.arize.protocol.Public.Embedding;
import com.arize.protocol.Public.Label;
import com.arize.protocol.Public.PredictionLabel;
import com.arize.protocol.Public.ActualLabel;
import com.arize.protocol.Public.MultiClassActual;
import com.arize.protocol.Public.MultiClassPrediction;
import com.arize.protocol.Public.MultiValue;
import com.arize.protocol.Public.ScoreCategorical;
import com.arize.protocol.Public.Value;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RecordUtilTest {

    protected PredictionLabel binaryPredLabel, categoricalPredLabel, numericPredLabel, scoreCategoricalPredLabel;
    protected ActualLabel binaryActLabel, categoricalActLabel, numericActLabel, scoreCategoricalActLabel;
    protected Map<String, Value> objMap;
    protected Map<String, Value> embeddingMap, embeddingMapNullRawData, embeddingMapNullLinkToData;

    @Before
    public void setup() {
        // Boolean pred labels
        ScoreCategorical.Category.Builder binaryScb = ScoreCategorical.Category.newBuilder();
        binaryScb.setCategory("true");
        binaryPredLabel = PredictionLabel.newBuilder().setScoreCategorical(ScoreCategorical.newBuilder().setCategory(binaryScb)).build();

        // String pred labels
        ScoreCategorical.Category.Builder stringScb = ScoreCategorical.Category.newBuilder();
        stringScb.setCategory("prediction");
        categoricalPredLabel = PredictionLabel.newBuilder().setScoreCategorical(ScoreCategorical.newBuilder().setCategory(stringScb)).build();

        // Numeric pred labels
        numericPredLabel = PredictionLabel.newBuilder().setNumeric(20.20).build();

        // ScoreCategorical pred labels
        ScoreCategorical.ScoreCategory.Builder scb = ScoreCategorical.ScoreCategory.newBuilder();
        scb.setCategory("apple");
        scb.setScore(3.14);
        scb.addAllNumericSequence(Arrays.asList(0.12, 0.23, 0.34));
        scoreCategoricalPredLabel =
                PredictionLabel.newBuilder()
                        .setScoreCategorical(ScoreCategorical.newBuilder().setScoreCategory(scb))
                        .build();

        // Boolean actual labels
        ScoreCategorical.Category.Builder binaryAct = ScoreCategorical.Category.newBuilder();
        binaryAct.setCategory("false");
        binaryActLabel = ActualLabel.newBuilder().setScoreCategorical(ScoreCategorical.newBuilder().setCategory(binaryAct)).build();

        // String actual labels
        ScoreCategorical.Category.Builder stringActScb = ScoreCategorical.Category.newBuilder();
        stringActScb.setCategory("actual");
        categoricalActLabel = ActualLabel.newBuilder().setScoreCategorical(ScoreCategorical.newBuilder().setCategory(stringActScb)).build();

        // Numeric actual labels
        numericActLabel = ActualLabel.newBuilder().setNumeric(20.20).build();

        // ScoreCategorical actual labels
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
    public void testConvertMultiClassSingleLabelPrediction() {
        Map<String, Double> predictionScores = new LinkedHashMap<>();
        predictionScores.put("economy", 0.7);
        predictionScores.put("business", 0.2);
        predictionScores.put("first", 0.1);
        ArizeClient.MultiClass mc =
                ArizeClient.MultiClass.MultiClassBuilder.newBuilder().setPredictionScores(predictionScores).build();

        MultiClassPrediction expected =
                MultiClassPrediction.newBuilder()
                        .setSingleLabel(
                                MultiClassPrediction.SingleLabel.newBuilder()
                                        .putPredictionScores("economy", DoubleValue.of(0.7))
                                        .putPredictionScores("business", DoubleValue.of(0.2))
                                        .putPredictionScores("first", DoubleValue.of(0.1)))
                        .build();
        assertEquals(
                PredictionLabel.newBuilder().setMultiClass(expected).build(),
                RecordUtil.convertPredictionLabel(mc));
    }

    @Test
    public void testConvertMultiClassMultiLabelPrediction() {
        Map<String, Double> predictionScores = new HashMap<>();
        predictionScores.put("action", 0.9);
        predictionScores.put("comedy", 0.4);
        Map<String, Integer> thresholdScores = new HashMap<>();
        thresholdScores.put("action", 1);
        thresholdScores.put("comedy", 0);
        ArizeClient.MultiClass mc =
                ArizeClient.MultiClass.MultiClassBuilder.newBuilder()
                        .setPredictionScores(predictionScores)
                        .setThresholdScores(thresholdScores)
                        .build();

        MultiClassPrediction expected =
                MultiClassPrediction.newBuilder()
                        .setMultiLabel(
                                MultiClassPrediction.MultiLabel.newBuilder()
                                        .putPredictionThresholdScores(
                                                "action",
                                                MultiClassPrediction.MultiLabel.MultiLabelScores.newBuilder()
                                                        .setPredictionScore(DoubleValue.of(0.9))
                                                        .setThresholdScore(DoubleValue.of(1.0))
                                                        .build())
                                        .putPredictionThresholdScores(
                                                "comedy",
                                                MultiClassPrediction.MultiLabel.MultiLabelScores.newBuilder()
                                                        .setPredictionScore(DoubleValue.of(0.4))
                                                        .setThresholdScore(DoubleValue.of(0.0))
                                                        .build()))
                        .build();
        assertEquals(
                PredictionLabel.newBuilder().setMultiClass(expected).build(),
                RecordUtil.convertPredictionLabel(mc));
    }

    @Test
    public void testConvertMultiClassEmptyThresholdsIsSingleLabel() {
        ArizeClient.MultiClass mc =
                ArizeClient.MultiClass.MultiClassBuilder.newBuilder()
                        .setPredictionScores(Collections.singletonMap("a", 0.5))
                        .setThresholdScores(Collections.<String, Double>emptyMap())
                        .build();
        assertTrue(RecordUtil.convertPredictionLabel(mc).getMultiClass().hasSingleLabel());
    }

    @Test
    public void testConvertMultiClassActual() {
        Map<String, Integer> actualScores = new LinkedHashMap<>();
        actualScores.put("action", 1);
        actualScores.put("comedy", 0);
        actualScores.put("drama", 1);
        ArizeClient.MultiClass mc =
                ArizeClient.MultiClass.MultiClassBuilder.newBuilder().setActualScores(actualScores).build();

        ActualLabel expected =
                ActualLabel.newBuilder()
                        .setMultiClass(MultiClassActual.newBuilder().addActualLabels("action").addActualLabels("drama"))
                        .build();
        assertEquals(expected, RecordUtil.convertActualLabel(mc));
    }

    @Test
    public void testConvertMultiClassMissingScoresForSide() {
        ArizeClient.MultiClass actualOnly =
                ArizeClient.MultiClass.MultiClassBuilder.newBuilder()
                        .setActualScores(Collections.singletonMap("a", 1))
                        .build();
        expectIllegalArgument(
                "actual-only label as prediction",
                "prediction scores for multi-class prediction label cannot be null",
                () -> RecordUtil.convertPredictionLabel(actualOnly));

        ArizeClient.MultiClass predictionOnly =
                ArizeClient.MultiClass.MultiClassBuilder.newBuilder()
                        .setPredictionScores(Collections.singletonMap("a", 0.5))
                        .build();
        expectIllegalArgument(
                "prediction-only label as actual",
                "actual scores for multi-class actual label cannot be null",
                () -> RecordUtil.convertActualLabel(predictionOnly));
    }

    @Test
    public void testMultiClassRejectsPredictionAndActualScoresTogether() {
        expectIllegalArgument(
                "prediction and actual scores on one object",
                "not both",
                () ->
                        ArizeClient.MultiClass.MultiClassBuilder.newBuilder()
                                .setPredictionScores(Collections.singletonMap("a", 0.5))
                                .setActualScores(Collections.singletonMap("a", 1))
                                .build());
    }

    @Test
    public void testMultiClassPredictionScoreValidation() {
        Map<String, Double> tooManyClasses = new HashMap<>();
        for (int i = 0; i < 501; i++) {
            tooManyClasses.put("class" + i, 0.5);
        }
        Map<String, Double> nullScore = new HashMap<>();
        nullScore.put("a", null);

        expectInvalidPrediction("empty", "at least 1 class", Collections.<String, Double>emptyMap());
        expectInvalidPrediction("501 classes", "at most 500 classes", tooManyClasses);
        expectInvalidPrediction("empty class name", "non-empty class names", Collections.singletonMap("", 0.5));
        expectInvalidPrediction("null class name", "non-empty class names", Collections.singletonMap(null, 0.5));
        expectInvalidPrediction("101-char class name", "bytes in UTF-8", Collections.singletonMap(repeat("x", 101), 0.5));
        expectInvalidPrediction("null score", "cannot be null", nullScore);
        expectInvalidPrediction("score 1.1", "prediction scores must be between 0 and 1", Collections.singletonMap("a", 1.1));
        expectInvalidPrediction("score -0.1", "prediction scores must be between 0 and 1", Collections.singletonMap("a", -0.1));
        expectInvalidPrediction("score NaN", "prediction scores must be between 0 and 1", Collections.singletonMap("a", Double.NaN));
        expectInvalidPrediction(
                "score +Infinity",
                "prediction scores must be between 0 and 1",
                Collections.singletonMap("a", Double.POSITIVE_INFINITY));
    }

    @Test
    public void testMultiClassClassNameLimitIsUtf8Bytes() {
        String thirtyThreeCjkChars = repeat("中", 33);
        ArizeClient.MultiClass.MultiClassBuilder.newBuilder()
                .setPredictionScores(Collections.singletonMap(thirtyThreeCjkChars, 0.5))
                .build();

        expectInvalidPrediction(
                "34 CJK chars (102 bytes)",
                "bytes in UTF-8",
                Collections.singletonMap(repeat("中", 34), 0.5));
    }

    @Test
    public void testMultiClassThresholdScoreValidation() {
        Map<String, Double> twoClasses = new HashMap<>();
        twoClasses.put("a", 0.5);
        twoClasses.put("b", 0.5);

        expectInvalidThreshold("fewer classes than predictions", "must contain the same classes", twoClasses, Collections.singletonMap("a", 0.5));
        expectInvalidThreshold(
                "different classes",
                "must contain the same classes",
                Collections.singletonMap("a", 0.5),
                Collections.singletonMap("b", 0.5));
        expectInvalidThreshold(
                "threshold 1.1",
                "threshold scores must be between 0 and 1",
                Collections.singletonMap("a", 0.5),
                Collections.singletonMap("a", 1.1));
        expectInvalidThreshold(
                "threshold -0.1",
                "threshold scores must be between 0 and 1",
                Collections.singletonMap("a", 0.5),
                Collections.singletonMap("a", -0.1));
        expectInvalidThreshold(
                "threshold NaN",
                "threshold scores must be between 0 and 1",
                Collections.singletonMap("a", 0.5),
                Collections.singletonMap("a", Double.NaN));
        expectInvalidThreshold(
                "threshold without predictions",
                "threshold scores require prediction scores",
                null,
                Collections.singletonMap("a", 0.5));
    }

    @Test
    public void testMultiClassActualScoreValidation() {
        expectInvalidActual("empty", "at least 1 class", Collections.<String, Double>emptyMap());
        expectInvalidActual("empty class name", "non-empty class names", Collections.singletonMap("", 1.0));
        expectInvalidActual("101-char class name", "bytes in UTF-8", Collections.singletonMap(repeat("x", 101), 1.0));
        expectInvalidActual("score 0.5", "must be either 0 or 1", Collections.singletonMap("a", 0.5));
        expectInvalidActual("score 2.0", "must be either 0 or 1", Collections.singletonMap("a", 2.0));
        expectInvalidActual("score NaN", "must be either 0 or 1", Collections.singletonMap("a", Double.NaN));
    }

    @Test
    public void testMultiClassWithoutScoresIsRejected() {
        expectIllegalArgument(
                "no scores",
                "must contain prediction scores or actual scores",
                () -> ArizeClient.MultiClass.MultiClassBuilder.newBuilder().build());
    }

    private static void expectInvalidPrediction(String caseName, String messageFragment, Map<String, Double> scores) {
        expectIllegalArgument(
                caseName,
                messageFragment,
                () -> ArizeClient.MultiClass.MultiClassBuilder.newBuilder().setPredictionScores(scores).build());
    }

    private static void expectInvalidThreshold(
            String caseName, String messageFragment, Map<String, Double> predictionScores, Map<String, Double> thresholdScores) {
        expectIllegalArgument(
                caseName,
                messageFragment,
                () -> ArizeClient.MultiClass.MultiClassBuilder.newBuilder()
                        .setPredictionScores(predictionScores)
                        .setThresholdScores(thresholdScores)
                        .build());
    }

    private static void expectInvalidActual(String caseName, String messageFragment, Map<String, Double> scores) {
        expectIllegalArgument(
                caseName,
                messageFragment,
                () -> ArizeClient.MultiClass.MultiClassBuilder.newBuilder().setActualScores(scores).build());
    }

    private static void expectIllegalArgument(String caseName, String messageFragment, Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException e) {
            assertTrue(
                    caseName + ": expected message containing '" + messageFragment + "' but was '" + e.getMessage() + "'",
                    e.getMessage().contains(messageFragment));
            return;
        }
        fail(caseName + ": expected IllegalArgumentException");
    }

    private static String repeat(String s, int times) {
        return String.join("", Collections.nCopies(times, s));
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
