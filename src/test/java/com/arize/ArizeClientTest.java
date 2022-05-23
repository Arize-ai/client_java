package com.arize;

import com.arize.ArizeClient.ScoredCategorical;
import com.arize.protocol.Public;
import com.arize.protocol.Public.Record;
import com.arize.protocol.Public.Record.Builder;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Timestamps;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ArizeClientTest {

    protected ArizeClient client;
    protected HttpServer server;

    protected List<Public.Record> posts;
    protected List<Public.BulkRecord> bulkPosts;
    protected List<Public.PreProductionRecord> preProductionRecords;
    protected List<Headers> headers;

    protected Map<String, Integer> intFeatures;
    protected Map<String, Long> longFeatures;
    protected Map<String, String> stringFeatures;
    protected Map<String, Double> doubleFeatures;
    protected Map<String, String> stringTags;

    protected String predictionPayload = "{\"space_key\": \"spaceKey\", \"model_id\": \"modelId\", \"prediction_id\": \"predictionId\", \"prediction\": {\"model_version\": \"modelVersion\", \"label\": {\"numeric\": 20.2}, \"tags\": {\"string\": {\"string\": \"string\"}}, \"features\": {\"int\": {\"int\": \"12345\"}, \"string\": {\"string\": \"string\"}, \"double\": {\"double\": 20.2}}}}";
    protected String actualPayload = "{\"space_key\": \"spaceKey\", \"model_id\": \"modelId\", \"prediction_id\": \"predictionId\", \"actual\": {\"label\": {\"numeric\": 20.2}}}, \"tags\": {\"string\": {\"string\": \"string\"}}";

    private List<String> expectedIds;
    private List<Integer> expectedLabels;
    private Record expectedActual, expectedPrediction;

    @Before
    public void setup() throws URISyntaxException, IOException {
        posts = new ArrayList<>();
        bulkPosts = new ArrayList<>();
        preProductionRecords = new ArrayList<>();
        headers = new ArrayList<>();
        server = testServer(posts, bulkPosts, preProductionRecords, headers);
        server.start();
        String uri = "http://localhost:" + server.getAddress().getPort() + "/v1";
        client = new ArizeClient("apiKey", "spaceKey", uri);
        intFeatures = new HashMap<>();
        longFeatures = new HashMap<>();
        stringFeatures = new HashMap<>();
        doubleFeatures = new HashMap<>();
        stringTags = new HashMap<>();
        intFeatures.put("int", 12345);
        stringFeatures.put("string", "string");
        doubleFeatures.put("double", 20.20);
        stringTags.put("string", "string");

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
        expectedLabels = new ArrayList<>(Arrays.asList(2020, 2121, 2222));
    }

    @After
    public void teardown() {
        server.stop(0);
    }


    @Test
    public void testLog() throws IOException, ExecutionException, InterruptedException {
        Map<String, Object> features = new HashMap<>();
        features.putAll(intFeatures);
        features.putAll(doubleFeatures);
        features.putAll(stringFeatures);
        Response response = client.log("modelId", "modelVersion", "predictionId", features, stringTags, 20.20, 20.21, null, 0);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server");
        }
        Assert.assertEquals("apiKey", headers.get(0).get("Authorization").get(0));
        Public.Record rec = posts.get(0);
        Assert.assertEquals("spaceKey", rec.getSpaceKey());
        Assert.assertEquals("modelId", rec.getModelId());
        Assert.assertEquals("predictionId", rec.getPredictionId());
        Assert.assertEquals("modelVersion", rec.getPrediction().getModelVersion());
        Assert.assertEquals(20.20, rec.getPrediction().getLabel().getNumeric(), 0.0);
        Assert.assertEquals(12345, rec.getPrediction().getFeaturesOrDefault("int", Public.Value.getDefaultInstance()).getInt());
        Assert.assertEquals("string", rec.getPrediction().getFeaturesOrDefault("string", Public.Value.getDefaultInstance()).getString());
        Assert.assertEquals(20.20, rec.getPrediction().getFeaturesOrDefault("double", Public.Value.getDefaultInstance()).getDouble(), 0.0);
        Assert.assertEquals("string", rec.getPrediction().getTagsOrDefault("string", Public.Value.getDefaultInstance()).getString());

        Assert.assertEquals("spaceKey", rec.getSpaceKey());
        Assert.assertEquals("modelId", rec.getModelId());
        Assert.assertEquals("predictionId", rec.getPredictionId());
        Assert.assertEquals(20.21, rec.getActual().getLabel().getNumeric(), 0.0);
    }

    @Test
    public void testLogScoredModel() throws IOException, ExecutionException, InterruptedException {
        Map<String, Object> features = new HashMap<>();
        features.putAll(intFeatures);
        features.putAll(doubleFeatures);
        features.putAll(stringFeatures);
        ScoredCategorical label = new ScoredCategorical("category", 20.20);
        Response response = client.log("modelId", "modelVersion", "predictionId", features, stringTags, label, null, null, 0);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server");
        }
        Assert.assertEquals("apiKey", headers.get(0).get("Authorization").get(0));
        Public.Record rec = posts.get(0);
        Assert.assertEquals("spaceKey", rec.getSpaceKey());
        Assert.assertEquals("modelId", rec.getModelId());
        Assert.assertEquals("predictionId", rec.getPredictionId());
        Assert.assertEquals("modelVersion", rec.getPrediction().getModelVersion());
        Assert.assertEquals(20.20, rec.getPrediction().getLabel().getScoreCategorical().getScoreCategory().getScore(), 0.0);
        Assert.assertEquals("category", rec.getPrediction().getLabel().getScoreCategorical().getScoreCategory().getCategory());
        Assert.assertEquals(12345, rec.getPrediction().getFeaturesOrDefault("int", Public.Value.getDefaultInstance()).getInt());
        Assert.assertEquals("string", rec.getPrediction().getFeaturesOrDefault("string", Public.Value.getDefaultInstance()).getString());
        Assert.assertEquals(20.20, rec.getPrediction().getFeaturesOrDefault("double", Public.Value.getDefaultInstance()).getDouble(), 0.0);
        Assert.assertEquals("string", rec.getPrediction().getTagsOrDefault("string", Public.Value.getDefaultInstance()).getString());

        Assert.assertEquals("spaceKey", rec.getSpaceKey());
        Assert.assertEquals("modelId", rec.getModelId());
        Assert.assertEquals("predictionId", rec.getPredictionId());
    }

    @Test
    public void testOptionalPredictionFields() throws IOException, ExecutionException, InterruptedException {
        Map<String, Object> features = new HashMap<>();
        Response response = client.log("modelId", null, "predictionId", null, null, 20.20, null, null, 0);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server: " + e.getMessage());
        }
        Record rec = posts.get(0);
        Assert.assertEquals("", rec.getPrediction().getModelVersion());
        Assert.assertTrue(rec.getPrediction().getFeaturesMap().isEmpty());
    }

    @Test
    public void testPredActualMissmatchedTypes() throws IOException, ExecutionException, InterruptedException {
        List intList = new ArrayList<>();
        intList.add(0, 1);
        List scList = new ArrayList<>();
        scList.add(0, new ScoredCategorical("cat", 1));
        try {
            client.log("modelId", null, "predictionId", null, null, 20.20, 1, null, 0);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("predictionLabel and actualLabel must be of the same type."));
        }
        try {
            client.log("modelId", null, "predictionId", null, null, new ScoredCategorical("1", 1), 1, null, 0);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("predictionLabel and actualLabel must be of the same type."));
        }
        try {
            client.log("modelId", null, "predictionId", null, null, 20.20, 1, null, 0);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("predictionLabel and actualLabel must be of the same type."));
        }
        try {
            client.log("modelId", null, "predictionId", null, null, 1L, 1, null, 0);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("predictionLabel and actualLabel must be of the same type."));
        }
        try {
            client.logTrainingRecords("modelId", "version", null, null, intList, scList);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("predictionLabel and actualLabel must be of the same type."));
        }
        try {
            client.logValidationRecords("modelId", "version", "batch", null, null, intList, scList);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("predictionLabel and actualLabel must be of the same type."));
        }
    }


    @Test
    public void testPredictionFeaturesWithNulls() throws IOException {
        Map<String, Object> features = new HashMap<>();
        features.put("A", 12345);
        features.put("B", "");
        features.put("C", null);

        Response prediction = client.log("modelId", "modelVersion", "predictionId", features, null, 20.20, null, null, 0);
        try {
            prediction.resolve(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
        Record rec = posts.get(0);
        Assert.assertEquals("modelVersion", rec.getPrediction().getModelVersion());
        Assert.assertFalse(rec.getPrediction().getFeaturesMap().isEmpty());
        Assert.assertTrue(rec.getPrediction().getFeaturesMap().containsKey("A"));
        Assert.assertTrue(rec.getPrediction().getFeaturesMap().containsKey("B"));
        Assert.assertFalse(rec.getPrediction().getFeaturesMap().containsKey("C"));
    }

    @Test
    public void testPredictionWithTimestamp() throws IOException, ExecutionException, InterruptedException {
        Map<String, Object> features = new HashMap<>();
        features.putAll(intFeatures);
        features.putAll(doubleFeatures);
        features.putAll(stringFeatures);
        Response response = client.log("modelId", null, "predictionId", features, null, 20.20, null, null, 1596560235000L);
        Timestamp expectedTime = Timestamps.fromMillis(1596560235000L);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server: " + e.getMessage());
        }
        Assert.assertEquals(expectedTime, posts.get(0).getPrediction().getTimestamp());
    }

    @Test
    public void testBuildBulkActual() throws IOException, ExecutionException, InterruptedException {
        List<String> expectedIds = new ArrayList<>(Arrays.asList("one", "two", "three"));
        List<Integer> expectedLabels = new ArrayList<Integer>(Arrays.asList(2020, 2121, 2222));

        Response response = client.bulkLog("modelId", "modelVersion", expectedIds, null, null, null, expectedLabels, null, null);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server: " + e.getMessage());
        }
        Public.BulkRecord bulkRec = bulkPosts.get(0);
        Assert.assertEquals("modelId", bulkRec.getModelId());
        Assert.assertEquals("spaceKey", bulkRec.getSpaceKey());
        Assert.assertEquals(3, bulkRec.getRecordsCount());
        List<Record> records = bulkRec.getRecordsList();
        for (Record record : records) {
            Assert.assertTrue(record.hasActual());
            Assert.assertTrue(expectedIds.contains(record.getPredictionId()));
            Assert.assertTrue(expectedLabels.contains(((Double) record.getActual().getLabel().getNumeric()).intValue()));
            Assert.assertEquals("modelId", record.getModelId());
            Assert.assertFalse("Failed timestamp", record.getActual().hasTimestamp());
        }
    }

    @Test
    public void testBuildBulkPrediction() throws ExecutionException, InterruptedException, IOException {
        List<Map<String, ?>> features = new ArrayList<>();
        features.add(new HashMap<String, Object>() {{
            putAll(intFeatures);
            putAll(doubleFeatures);
            putAll(stringFeatures);
        }});
        features.add(new HashMap<String, Object>() {{
            putAll(intFeatures);
            putAll(doubleFeatures);
            putAll(stringFeatures);
        }});
        features.add(new HashMap<String, Object>() {{
            putAll(intFeatures);
            putAll(doubleFeatures);
            putAll(stringFeatures);
        }});
        List<Map<String, ?>> tags = new ArrayList<>();
        tags.add(new HashMap<String, Object>() {{
            putAll(stringTags);
        }});
        tags.add(new HashMap<String, Object>() {{
            putAll(stringTags);
        }});
        tags.add(new HashMap<String, Object>() {{
            putAll(stringTags);
        }});

        Response response = client.bulkLog("modelId", "modelVersion", expectedIds, features, tags, expectedLabels, null, null, null);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server: " + e.getMessage());
        }

        Public.BulkRecord bulk = bulkPosts.get(0);
        Assert.assertEquals("modelId", bulk.getModelId());
        Assert.assertEquals("spaceKey", bulk.getSpaceKey());
        Assert.assertEquals("modelVersion", bulk.getModelVersion());
        Assert.assertEquals(3, bulk.getRecordsCount());

        List<Record> records = bulk.getRecordsList();
        for (Record record : records) {
            Assert.assertTrue(record.hasPrediction());
            Assert.assertTrue(expectedIds.contains(record.getPredictionId()));
            Assert.assertTrue(expectedLabels.contains(((Double) record.getPrediction().getLabel().getNumeric()).intValue()));
            Assert.assertEquals("modelVersion", record.getPrediction().getModelVersion());
            Assert.assertEquals("modelId", record.getModelId());
            Assert.assertFalse(record.getPrediction().hasTimestamp());
            Assert.assertEquals(3, record.getPrediction().getFeaturesCount());
            Assert.assertEquals(1, record.getPrediction().getTagsCount());
        }
    }

    @Test
    public void testFullLog() throws IOException, ExecutionException, InterruptedException {
        List<Map<String, ?>> features = new ArrayList<>();
        features.add(new HashMap<String, Object>() {{
            put("days", 5.0);
            put("is_organic", 0L);
        }});
        features.add(new HashMap<String, Object>() {{
            put("days", 4.5);
            put("is_organic", 1L);
        }});
        features.add(new HashMap<String, Object>() {{
            put("days", 2.0);
            put("is_organic", 0L);
        }});
        List<Map<String, ?>> tags = new ArrayList<>();
        tags.add(new HashMap<String, Object>() {{
            put("tag_1", 5.0);
            put("tag_2", "tag_2");
        }});
        tags.add(new HashMap<String, Object>() {{
            put("tag_1", 5.0);
            put("tag_2", "tag_2");
        }});
        tags.add(new HashMap<String, Object>() {{
            put("tag_1", 5.0);
            put("tag_2", "tag_2");
        }});
        List<String> predictionLabels = Arrays.asList("ripe", "not-ripe", "not-ripe");
        List<String> actualLabels = Arrays.asList("not-ripe", "not-ripe", "not-ripe");
        List<Map<String, Double>> shapValues = new ArrayList<>();
        shapValues.add(new HashMap<String, Double>() {{
            put("days", 2.0);
            put("is_organic", -1.2);
        }});
        shapValues.add(new HashMap<String, Double>() {{
            put("days", 2.2);
            put("is_organic", -1.2);
        }});
        shapValues.add(new HashMap<String, Double>() {{
            put("days", 2.5);
            put("is_organic", -1.2);
        }});

        Response response = client.bulkLog("modelId", "modelVersion", expectedIds, features, tags, predictionLabels, actualLabels, shapValues, null);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server: " + e.getMessage());
        }

        Public.BulkRecord bulk = bulkPosts.get(0);
        Assert.assertEquals("modelId", bulk.getModelId());
        Assert.assertEquals("spaceKey", bulk.getSpaceKey());
        Assert.assertEquals("modelVersion", bulk.getModelVersion());
        Assert.assertEquals(3, bulk.getRecordsCount());

        List<Record> records = bulk.getRecordsList();
        for (int i = 0; i < records.size(); i++) {
            Public.Record rec = records.get(i);

            //compare input prediction ids to posted Public.Record
            Assert.assertEquals(expectedIds.get(i), rec.getPredictionId());

            //compare input features to posted Public.Record
            Map<String, ?> f = features.get(i);
            Assert.assertEquals(f.get("days"), rec.getPrediction().getFeaturesOrDefault("days", Public.Value.getDefaultInstance()).getDouble());
            Assert.assertEquals(f.get("is_organic"), rec.getPrediction().getFeaturesOrDefault("is_organic", Public.Value.getDefaultInstance()).getInt());

            //compare input tags to posted Public.Record
            Map<String, ?> t = tags.get(i);
            Assert.assertEquals(t.get("tag_1"), rec.getPrediction().getTagsOrDefault("tag_1", Public.Value.getDefaultInstance()).getDouble());
            Assert.assertEquals(t.get("tag_2"), rec.getPrediction().getTagsOrDefault("tag_2", Public.Value.getDefaultInstance()).getString());

            //compare input prediction labels to posted Public.Record
            Assert.assertEquals(predictionLabels.get(i), rec.getPrediction().getLabel().getCategorical());

            //compare input actual labels to posted Public.Record
            Assert.assertEquals(actualLabels.get(i), rec.getActual().getLabel().getCategorical());

            //compare input shap values to posted Public.Record
            Map<String, Double> s = shapValues.get(i);
            Assert.assertEquals(s.get("days"), rec.getFeatureImportances().getFeatureImportancesOrDefault("days", Public.Value.getDefaultInstance().getDouble()), 0.0);
            Assert.assertEquals(s.get("is_organic"), rec.getFeatureImportances().getFeatureImportancesOrDefault("is_organic", Public.Value.getDefaultInstance().getDouble()), 0.0);
        }
    }

    @Test
    public void testLogTraining() throws IOException, ExecutionException, InterruptedException {
        List<Map<String, ?>> features = new ArrayList<>();
        features.add(new HashMap<String, Object>() {{
            put("days", 5.0);
            put("is_organic", 0L);
        }});
        features.add(new HashMap<String, Object>() {{
            put("days", 4.5);
            put("is_organic", 1L);
        }});
        features.add(new HashMap<String, Object>() {{
            put("days", 2.0);
            put("is_organic", 0L);
        }});
        List<Map<String, ?>> tags = new ArrayList<>();
        tags.add(new HashMap<String, Object>() {{
            put("tag_1", 5.0);
            put("tag_2", "tag_2");
        }});
        tags.add(new HashMap<String, Object>() {{
            put("tag_1", 5.0);
            put("tag_2", "tag_2");
        }});
        tags.add(new HashMap<String, Object>() {{
            put("tag_1", 5.0);
            put("tag_2", "tag_2");
        }});
        List<String> predictionLabels = Arrays.asList("ripe", "not-ripe", "not-ripe");
        List<String> actualLabels = Arrays.asList("not-ripe", "not-ripe", "not-ripe");

        Response response = client.logTrainingRecords("modelId", "modelVersion", features, tags, predictionLabels, actualLabels);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server: " + e.getMessage());
        }

        Headers headers = this.headers.get(0);
        Assert.assertEquals("apiKey", headers.get("Authorization").get(0));
        Assert.assertEquals("spaceKey", headers.get("Grpc-Metadata-space").get(0));

        for (int i = 0; i < preProductionRecords.size(); i++) {
            Public.PreProductionRecord preprodRec = preProductionRecords.get(i);
            Record record = preprodRec.getTrainingRecord().getRecord();
            Assert.assertEquals("modelId", record.getModelId());
            Assert.assertEquals("modelVersion", record.getPrediction().getModelVersion());


            // For now training records dont include a prediction id
            Assert.assertEquals("", record.getPredictionId());

            //compare input features to posted Public.Record
            Map<String, ?> f = features.get(i);
            Assert.assertEquals(f.get("days"), record.getPrediction().getFeaturesOrDefault("days", Public.Value.getDefaultInstance()).getDouble());
            Assert.assertEquals(f.get("is_organic"), record.getPrediction().getFeaturesOrDefault("is_organic", Public.Value.getDefaultInstance()).getInt());

            //compare input tags to posted Public.Record
            Map<String, ?> t = tags.get(i);
            Assert.assertEquals(t.get("tag_1"), record.getPrediction().getTagsOrDefault("tag_1", Public.Value.getDefaultInstance()).getDouble());
            Assert.assertEquals(t.get("tag_2"), record.getPrediction().getTagsOrDefault("tag_2", Public.Value.getDefaultInstance()).getString());

            //compare input prediction labels to posted Public.Record
            Assert.assertEquals(predictionLabels.get(i), record.getPrediction().getLabel().getCategorical());

            //compare input actual labels to posted Public.Record
            Assert.assertEquals(actualLabels.get(i), record.getActual().getLabel().getCategorical());
        }
    }

    @Test
    public void testLogValidation() throws IOException, ExecutionException, InterruptedException {
        List<Map<String, ?>> features = new ArrayList<>();
        features.add(new HashMap<String, Object>() {{
            put("days", 5.0);
            put("is_organic", 0L);
        }});
        features.add(new HashMap<String, Object>() {{
            put("days", 4.5);
            put("is_organic", 1L);
        }});
        features.add(new HashMap<String, Object>() {{
            put("days", 2.0);
            put("is_organic", 0L);
        }});
        List<Map<String, ?>> tags = new ArrayList<>();
        tags.add(new HashMap<String, Object>() {{
            put("tag_1", 5.0);
            put("tag_2", "tag_2");
        }});
        tags.add(new HashMap<String, Object>() {{
            put("tag_1", 5.0);
            put("tag_2", "tag_2");
        }});
        tags.add(new HashMap<String, Object>() {{
            put("tag_1", 5.0);
            put("tag_2", "tag_2");
        }});
        List<String> predictionLabels = Arrays.asList("ripe", "not-ripe", "not-ripe");
        List<String> actualLabels = Arrays.asList("not-ripe", "not-ripe", "not-ripe");

        Response response = client.logValidationRecords("modelId", "modelVersion", "offline-1", features, tags, predictionLabels, actualLabels);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server: " + e.getMessage());
        }

        Headers headers = this.headers.get(0);
        Assert.assertEquals("apiKey", headers.get("Authorization").get(0));
        Assert.assertEquals("spaceKey", headers.get("Grpc-Metadata-space").get(0));

        for (int i = 0; i < preProductionRecords.size(); i++) {
            Public.PreProductionRecord preprodRec = preProductionRecords.get(i);
            Assert.assertEquals("offline-1", preprodRec.getValidationRecord().getBatchId());
            Record record = preprodRec.getValidationRecord().getRecord();
            Assert.assertEquals("modelId", record.getModelId());
            Assert.assertEquals("modelVersion", record.getPrediction().getModelVersion());

            // For now training records dont include a prediction id
            Assert.assertEquals("", record.getPredictionId());

            //compare input features to posted Public.Record
            Map<String, ?> f = features.get(i);
            Assert.assertEquals(f.get("days"), record.getPrediction().getFeaturesOrDefault("days", Public.Value.getDefaultInstance()).getDouble());
            Assert.assertEquals(f.get("is_organic"), record.getPrediction().getFeaturesOrDefault("is_organic", Public.Value.getDefaultInstance()).getInt());

            //compare input tags to posted Public.Record
            Map<String, ?> t = tags.get(i);
            Assert.assertEquals(t.get("tag_1"), record.getPrediction().getTagsOrDefault("tag_1", Public.Value.getDefaultInstance()).getDouble());
            Assert.assertEquals(t.get("tag_2"), record.getPrediction().getTagsOrDefault("tag_2", Public.Value.getDefaultInstance()).getString());

            //compare input prediction labels to posted Public.Record
            Assert.assertEquals(predictionLabels.get(i), record.getPrediction().getLabel().getCategorical());

            //compare input actual labels to posted Public.Record
            Assert.assertEquals(actualLabels.get(i), record.getActual().getLabel().getCategorical());
        }
    }


    private HttpServer testServer(List<Public.Record> postBodies, List<Public.BulkRecord> bulkPostBodies, List<Public.PreProductionRecord> preProductionPostBodies, List<Headers> headers) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/log", exchange -> {
            headers.add(exchange.getRequestHeaders());
            String postBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            Builder rec = Record.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(postBody, rec);
            postBodies.add(rec.build());
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/v1/bulk", exchange -> {
            headers.add(exchange.getRequestHeaders());
            String postBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            Public.BulkRecord.Builder rec = Public.BulkRecord.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(postBody, rec);
            bulkPostBodies.add(rec.build());
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/v1/preprod", exchange -> {
            headers.add(exchange.getRequestHeaders());
            String postBody = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            String[] preprodRecords = postBody.split("\n");
            for (String preprodRecord : preprodRecords) {
                Public.PreProductionRecord.Builder rec = Public.PreProductionRecord.newBuilder();
                JsonFormat.parser().ignoringUnknownFields().merge(preprodRecord, rec);
                preProductionPostBodies.add(rec.build());
            }
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        return server;
    }
}
