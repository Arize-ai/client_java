package com.arize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.arize.protocol.Public.Actual;
import com.arize.protocol.Public.Prediction;
import com.arize.protocol.Public.Record;
import com.arize.protocol.Public.Record.Builder;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BuildRecordTest {

    protected ArizeClient client;

    protected Builder expectedPredictionBuilder, expectedActualBuilder;
    protected Builder predictionBuilder, actualBuilder;

    protected Map<String, Integer> intFeatures;
    protected Map<String, Long> longFeatures;
    protected Map<String, String> stringFeatures;
    protected Map<String, Double> doubleFeatures;

    protected String predictionPayload = "{\"organization_key\": \"orgKey\", \"model_id\": \"modelId\", \"prediction_id\": \"predictionId\", \"prediction\": {\"model_version\": \"modelVersion\", \"label\": {\"numeric\": 20.2}, \"features\": {\"A\": {\"int\": \"12345\"}, \"B\": {\"string\": \"string\"}, \"C\": {\"double\": 20.2}}}}";
    protected String actualPayload = "{\"organization_key\": \"orgKey\", \"model_id\": \"modelId\", \"prediction_id\": \"predictionId\", \"actual\": {\"label\": {\"numeric\": 20.2}}}";

    private Builder builder;

    @Before
    public void setup() throws URISyntaxException {
        client = new ArizeClient("apiKey", "orgKey");

        intFeatures = new HashMap<>();
        longFeatures = new HashMap<>();
        stringFeatures = new HashMap<>();
        doubleFeatures = new HashMap<>();
        intFeatures.put("A", 12345);
        stringFeatures.put("B", "string");
        doubleFeatures.put("C", 20.20);

        predictionBuilder = Record.newBuilder();
        actualBuilder = Record.newBuilder();
        try {
            JsonFormat.parser().merge(predictionPayload, predictionBuilder);
            JsonFormat.parser().merge(actualPayload, actualBuilder);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        // Hack so timestamps match
        Actual actual = actualBuilder.getActualBuilder().setTimestamp(Timestamp.getDefaultInstance()).build();
        expectedActualBuilder = actualBuilder.setActual(actual);

        Prediction pred = predictionBuilder.getPredictionBuilder().setTimestamp(Timestamp.getDefaultInstance()).build();
        expectedPredictionBuilder = predictionBuilder.setPrediction(pred);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBuildPrediction() {
        Record prediction = client.buildPrediction("modelId", "modelVersion", "predictionId", 20.20, intFeatures,
                doubleFeatures, stringFeatures);

        builder = prediction.toBuilder();
        Prediction pred = builder.getPredictionBuilder().setTimestamp(Timestamp.getDefaultInstance()).build();
        Record rec = builder.setPrediction(pred).build();

        assertEquals(expectedPredictionBuilder.build(), rec);
    }

    @Test
    public void testBuildActual() {
        Record actual = client.buildActual("modelId", "predictionId", 20.20);

        builder = actual.toBuilder();
        Actual pred = builder.getActualBuilder().setTimestamp(Timestamp.getDefaultInstance()).build();
        Record rec = builder.setActual(pred).build();

        assertEquals(expectedActualBuilder.build(), rec);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPredictionNullModelVersion() {
        Record prediction = client.buildPrediction("modelId", null, "predictionId", 20.20, intFeatures, doubleFeatures,
                stringFeatures);

        // Clear Timestamp
        builder = prediction.toBuilder();
        Prediction pred = builder.getPredictionBuilder().setTimestamp(Timestamp.getDefaultInstance()).build();
        Record rec = builder.setPrediction(pred).build();

        // Fix expected
        Builder expectedClone = expectedPredictionBuilder.clone();
        Prediction clearVersion = expectedClone.getPredictionBuilder().clearModelVersion().build();
        expectedClone.setPrediction(clearVersion);

        assertEquals(expectedClone.build(), rec);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPredictionNullFeatures() {
        Record prediction = client.buildPrediction("modelId", "modelVersion", "predictionId", 20.20, null, intFeatures,
                null);

        builder = prediction.toBuilder();
        Prediction pred = builder.getPredictionBuilder().setTimestamp(Timestamp.getDefaultInstance()).clearFeatures()
                .build();
        Record rec = builder.setPrediction(pred).build();

        // Fix expected
        Builder expectedClone = expectedPredictionBuilder.clone();
        Prediction clearVersion = expectedClone.getPredictionBuilder().clearFeatures().build();
        expectedClone.setPrediction(clearVersion);

        assertEquals(expectedClone.build(), rec);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testPredictionFeaturesWithNulls() {
        Map brokenMap = new HashMap<>();
        brokenMap.put("A", 12345);
        brokenMap.put("B", "");
        brokenMap.put("C", null);

        Record prediction = null;
        try {
            prediction = client.buildPrediction("modelId", "modelVersion", "predictionId", 20.20, brokenMap,
                    intFeatures, null);
        } catch (Exception ex) {
            assertNull(prediction);
            assertTrue(ex instanceof IllegalArgumentException);
        }
    }
}