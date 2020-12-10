package com.arize;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arize.protocol.Public.BulkRecord;
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

    protected Map<String, Integer> intFeatures;
    protected Map<String, Long> longFeatures;
    protected Map<String, String> stringFeatures;
    protected Map<String, Double> doubleFeatures;

    protected String predictionPayload = "{\"organization_key\": \"orgKey\", \"model_id\": \"modelId\", \"prediction_id\": \"predictionId\", \"prediction\": {\"model_version\": \"modelVersion\", \"label\": {\"numeric\": 20.2}, \"features\": {\"int\": {\"int\": \"12345\"}, \"string\": {\"string\": \"string\"}, \"double\": {\"double\": 20.2}}}}";
    protected String actualPayload = "{\"organization_key\": \"orgKey\", \"model_id\": \"modelId\", \"prediction_id\": \"predictionId\", \"actual\": {\"label\": {\"numeric\": 20.2}}}";

    private List<String> expectedIds;
    private List<Integer> expectedLabels;
    private List<Map<String, ?>> expectedIntFeatures, expectedStringFeatures, expectedDoubleFeatures;
    private Record expectedActual, expectedPrediction;

    @Before
    public void setup() throws URISyntaxException {
        client = new ArizeClient("apiKey", "orgKey");

        intFeatures = new HashMap<>();
        longFeatures = new HashMap<>();
        stringFeatures = new HashMap<>();
        doubleFeatures = new HashMap<>();
        intFeatures.put("int", 12345);
        stringFeatures.put("string", "string");
        doubleFeatures.put("double", 20.20);

        Builder predictionBuilder = Record.newBuilder();
        Builder actualBuilder = Record.newBuilder();
        try {
            JsonFormat.parser().merge(predictionPayload, predictionBuilder);
            JsonFormat.parser().merge(actualPayload, actualBuilder);
        } catch (final Exception e) {
            Assert.fail(e.getMessage());
        }
        expectedActual = actualBuilder.build();
        expectedPrediction = predictionBuilder.build();

        expectedIds = new ArrayList<>(Arrays.asList("one", "two", "three"));
        expectedLabels = new ArrayList<Integer>(Arrays.asList(2020, 2121, 2222));
        expectedIntFeatures = new ArrayList<>(Arrays.asList(intFeatures, intFeatures, intFeatures));
        expectedStringFeatures = new ArrayList<>(Arrays.asList(stringFeatures, stringFeatures, stringFeatures));
        expectedDoubleFeatures = new ArrayList<>(Arrays.asList(doubleFeatures, doubleFeatures, doubleFeatures));
    }

    @Test
    public void testBuildPrediction() {
        final List<Map<String, ?>> feats = new ArrayList<>();
        feats.add(intFeatures);
        feats.add(doubleFeatures);
        feats.add(stringFeatures);
        final Record prediction = client.buildPrediction("modelId", "modelVersion", "predictionId", 20.20, null, feats);
        assertEquals(expectedPrediction, prediction);
    }

    @Test
    public void testBuildActual() {
        final Record actual = client.buildActual("modelId", "predictionId", 20.20, null);
        assertEquals(expectedActual, actual);
    }

    @Test
    public void testPredictionNullModelVersion() {
        final List<Map<String, ?>> feats = new ArrayList<>();
        feats.add(intFeatures);
        feats.add(doubleFeatures);
        feats.add(stringFeatures);
        final Record prediction = client.buildPrediction("modelId", null, "predictionId", 20.20, null, feats);

        // Clear version for expected
        final Builder expectedClone = expectedPrediction.toBuilder().clone();
        final Prediction clearVersion = expectedClone.getPredictionBuilder().clearModelVersion().build();
        expectedClone.setPrediction(clearVersion);

        assertEquals(expectedClone.build(), prediction);
    }

    @Test
    public void testPredictionNullFeatures() {
        final List<Map<String, ?>> feats = new ArrayList<>();
        feats.add(intFeatures);
        feats.add(null);
        final Record prediction = client.buildPrediction("modelId", "modelVersion", "predictionId", 20.20, null, feats);

        // Fix expected to only contain int features
        final Builder expectedClone = expectedPrediction.toBuilder().clone();
        Prediction.Builder predictionBuilder = expectedClone.getPredictionBuilder();
        predictionBuilder.removeFeatures("string").removeFeatures("double");
        final Prediction clearVersion = predictionBuilder.build();
        expectedClone.setPrediction(clearVersion);

        assertEquals(expectedClone.build(), prediction);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testPredictionFeaturesWithNulls() {
        final Map brokenMap = new HashMap<>();
        brokenMap.put("A", 12345);
        brokenMap.put("B", "");
        brokenMap.put("C", null);

        final List<Map<String, ?>> feats = new ArrayList<>();
        feats.add(intFeatures);
        feats.add(doubleFeatures);
        feats.add(stringFeatures);
        feats.add(brokenMap);
        Exception e = null;
        Record prediction = null;
        try {
            prediction = client.buildPrediction("modelId", "modelVersion", "predictionId", 20.20, null, feats);
        } catch (final Exception ex) {
            e = ex;
        }
        assertNull(prediction);
        assertTrue(e instanceof IllegalArgumentException);
    }

    @Test
    public void testPredictionTimeOverwrite() {
        final List<Map<String, ?>> feats = new ArrayList<>();
        feats.add(intFeatures);
        feats.add(doubleFeatures);
        feats.add(stringFeatures);
        final Record prediction = client.buildPrediction("modelId", null, "predictionId", 20.20, 1596560235000L, feats);
        final Timestamp expectedTime = fromMillis(1596560235000L);
        assertEquals(expectedTime, prediction.getPrediction().getTimestamp());
    }

    @Test
    public void testBuildBulkActual() {

        List<String> expectedIds = new ArrayList<>(Arrays.asList("one", "two", "three"));
        List<Integer> expectedLabels = new ArrayList<Integer>(Arrays.asList(2020, 2121, 2222));

        final BulkRecord bulk = client.buildBulkActuals("modelId", expectedIds, expectedLabels);

        assertEquals("modelId", bulk.getModelId());
        assertEquals("orgKey", bulk.getOrganizationKey());
        assertFalse(bulk.hasTimestamp());
        assertEquals(3, bulk.getRecordsCount());
        List<Record> records = bulk.getRecordsList();
        for (Record record : records) {
            assertTrue(record.hasActual());
            assertTrue(expectedIds.contains(record.getPredictionId()));
            assertTrue(expectedLabels.contains(((Double) record.getActual().getLabel().getNumeric()).intValue()));
            assertFalse("Failed timestamp", record.getActual().hasTimestamp());
            assertEquals("Failed model id", record.getModelId(), "");
        }
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void testBuildBulkPrediction() {
        final BulkRecord bulk = client.buildBulkPrediction("modelId", "modelVersion", expectedIds, expectedLabels, null,
                expectedIntFeatures, expectedStringFeatures, expectedDoubleFeatures);

        assertEquals("modelId", bulk.getModelId());
        assertEquals("orgKey", bulk.getOrganizationKey());
        assertEquals("modelVersion", bulk.getModelVersion());
        assertFalse(bulk.hasTimestamp());
        assertEquals(3, bulk.getRecordsCount());

        List<Record> records = bulk.getRecordsList();
        for (Record record : records) {
            assertTrue(record.hasPrediction());
            assertTrue(expectedIds.contains(record.getPredictionId()));
            assertTrue(expectedLabels.contains(((Double) record.getPrediction().getLabel().getNumeric()).intValue()));
            assertEquals(record.getPrediction().getModelVersion(), "");
            assertEquals(record.getModelId(), "");
            assertFalse(record.getPrediction().hasTimestamp());
            assertEquals(3, record.getPrediction().getFeaturesCount());
        }
    }
}