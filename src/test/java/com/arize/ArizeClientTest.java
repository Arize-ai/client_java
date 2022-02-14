package com.arize;

import com.arize.ArizeClient.ScoredCategorical;
import com.arize.protocol.Public;
import com.arize.protocol.Public.Record;
import com.arize.protocol.Public.Record.Builder;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Timestamps;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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

    protected String predictionPayload = "{\"organization_key\": \"orgKey\", \"model_id\": \"modelId\", \"prediction_id\": \"predictionId\", \"prediction\": {\"model_version\": \"modelVersion\", \"label\": {\"numeric\": 20.2}, \"features\": {\"int\": {\"int\": \"12345\"}, \"string\": {\"string\": \"string\"}, \"double\": {\"double\": 20.2}}}}";
    protected String actualPayload = "{\"organization_key\": \"orgKey\", \"model_id\": \"modelId\", \"prediction_id\": \"predictionId\", \"actual\": {\"label\": {\"numeric\": 20.2}}}";

    private List<String> expectedIds;
    private List<Integer> expectedLabels;
    private List<Map<String, ?>> expectedIntFeatures, expectedStringFeatures, expectedDoubleFeatures;
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
        client = new ArizeClient("apiKey", "orgKey", uri);
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
        expectedLabels = new ArrayList<>(Arrays.asList(2020, 2121, 2222));
        expectedIntFeatures = new ArrayList<>(Arrays.asList(intFeatures, intFeatures, intFeatures));
        expectedStringFeatures = new ArrayList<>(Arrays.asList(stringFeatures, stringFeatures, stringFeatures));
        expectedDoubleFeatures = new ArrayList<>(Arrays.asList(doubleFeatures, doubleFeatures, doubleFeatures));
    }

    @After
    public void teardown() {
        server.stop(0);
    }

    @Test
    public void testLogPrediction() throws IOException, ExecutionException, InterruptedException {
        Response response = client.logPrediction("modelId", "modelVersion", "predictionId", 20.20, intFeatures, doubleFeatures, stringFeatures);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server");
        }
        Assert.assertEquals("apiKey", headers.get(0).get("Authorization").get(0));
        Public.Record rec = posts.get(0);
        Assert.assertEquals("orgKey", rec.getOrganizationKey());
        Assert.assertEquals("modelId", rec.getModelId());
        Assert.assertEquals("predictionId", rec.getPredictionId());
        Assert.assertEquals("modelVersion", rec.getPrediction().getModelVersion());
        Assert.assertEquals(20.20, rec.getPrediction().getLabel().getNumeric(), 0.0);
        Assert.assertEquals(12345, rec.getPrediction().getFeaturesOrDefault("int", Public.Value.getDefaultInstance()).getInt());
        Assert.assertEquals("string", rec.getPrediction().getFeaturesOrDefault("string", Public.Value.getDefaultInstance()).getString());
        Assert.assertEquals(20.20, rec.getPrediction().getFeaturesOrDefault("double", Public.Value.getDefaultInstance()).getDouble(), 0.0);
    }

    @Test
    public void testLogActual() throws IOException, ExecutionException, InterruptedException {
        Response response = client.logActual("modelId", "predictionId", 20.20);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server");
        }
        Assert.assertEquals("apiKey", headers.get(0).get("Authorization").get(0));
        Public.Record rec = posts.get(0);
        Assert.assertEquals("orgKey", rec.getOrganizationKey());
        Assert.assertEquals("modelId", rec.getModelId());
        Assert.assertEquals("predictionId", rec.getPredictionId());
        Assert.assertEquals(20.20, rec.getActual().getLabel().getNumeric(), 0.0);
    }

    @Test
    public void testLog() throws IOException, ExecutionException, InterruptedException {
        Map<String, Object> features = new HashMap<>();
        features.putAll(intFeatures);
        features.putAll(doubleFeatures);
        features.putAll(stringFeatures);
        Response response = client.log("modelId", "modelVersion", "predictionId", features, 20.20, 20.21, null, 0);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server");
        }
        Assert.assertEquals("apiKey", headers.get(0).get("Authorization").get(0));
        Public.Record rec = posts.get(0);
        Assert.assertEquals("orgKey", rec.getOrganizationKey());
        Assert.assertEquals("modelId", rec.getModelId());
        Assert.assertEquals("predictionId", rec.getPredictionId());
        Assert.assertEquals("modelVersion", rec.getPrediction().getModelVersion());
        Assert.assertEquals(20.20, rec.getPrediction().getLabel().getNumeric(), 0.0);
        Assert.assertEquals(12345, rec.getPrediction().getFeaturesOrDefault("int", Public.Value.getDefaultInstance()).getInt());
        Assert.assertEquals("string", rec.getPrediction().getFeaturesOrDefault("string", Public.Value.getDefaultInstance()).getString());
        Assert.assertEquals(20.20, rec.getPrediction().getFeaturesOrDefault("double", Public.Value.getDefaultInstance()).getDouble(), 0.0);

        Assert.assertEquals("orgKey", rec.getOrganizationKey());
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
        Response response = client.log("modelId", "modelVersion", "predictionId", features, label, null, null, 0);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server");
        }
        Assert.assertEquals("apiKey", headers.get(0).get("Authorization").get(0));
        Public.Record rec = posts.get(0);
        Assert.assertEquals("orgKey", rec.getOrganizationKey());
        Assert.assertEquals("modelId", rec.getModelId());
        Assert.assertEquals("predictionId", rec.getPredictionId());
        Assert.assertEquals("modelVersion", rec.getPrediction().getModelVersion());
        Assert.assertEquals(20.20, rec.getPrediction().getLabel().getScoreCategorical().getScoreCategory().getScore(), 0.0);
        Assert.assertEquals("category", rec.getPrediction().getLabel().getScoreCategorical().getScoreCategory().getCategory());
        Assert.assertEquals(12345, rec.getPrediction().getFeaturesOrDefault("int", Public.Value.getDefaultInstance()).getInt());
        Assert.assertEquals("string", rec.getPrediction().getFeaturesOrDefault("string", Public.Value.getDefaultInstance()).getString());
        Assert.assertEquals(20.20, rec.getPrediction().getFeaturesOrDefault("double", Public.Value.getDefaultInstance()).getDouble(), 0.0);

        Assert.assertEquals("orgKey", rec.getOrganizationKey());
        Assert.assertEquals("modelId", rec.getModelId());
        Assert.assertEquals("predictionId", rec.getPredictionId());
    }

    @Test
    public void testOptionalPredictionFields() throws IOException, ExecutionException, InterruptedException {
        Map<String, Object> features = new HashMap<>();
        Response response = client.log("modelId", null, "predictionId", null, 20.20, null, null, 0);
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
    public void testPredictionFeaturesWithNulls() throws IOException {
        Map<String, Object> features = new HashMap<>();
        features.put("A", 12345);
        features.put("B", "");
        features.put("C", null);

        Response prediction = client.log("modelId", "modelVersion", "predictionId", features, 20.20, null, null, 0);;
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
    public void testPredictionTimeOverwrite() throws IOException, ExecutionException, InterruptedException {
        Map<String, Object> features = new HashMap<>();
        features.putAll(intFeatures);
        features.putAll(doubleFeatures);
        features.putAll(stringFeatures);
        Response response = client.log("modelId", null, "predictionId", features, 20.20, null, null, 1596560235000L);
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

        Response response = client.bulkLog("modelId", "modelVersion", expectedIds, null, null, expectedLabels, null, null);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server: " + e.getMessage());
        }
        Public.BulkRecord bulkRec = bulkPosts.get(0);
        Assert.assertEquals("modelId", bulkRec.getModelId());
        Assert.assertEquals("orgKey", bulkRec.getOrganizationKey());
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
        features.add(new HashMap<String, Object>(){{
            putAll(intFeatures);
            putAll(doubleFeatures);
            putAll(stringFeatures);
        }});
        features.add(new HashMap<String, Object>(){{
            putAll(intFeatures);
            putAll(doubleFeatures);
            putAll(stringFeatures);
        }});
        features.add(new HashMap<String, Object>(){{
            putAll(intFeatures);
            putAll(doubleFeatures);
            putAll(stringFeatures);
        }});

        Response response = client.bulkLog("modelId", "modelVersion", expectedIds, features, expectedLabels, null, null, null);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server: " + e.getMessage());
        }

        Public.BulkRecord bulk = bulkPosts.get(0);
        Assert.assertEquals("modelId", bulk.getModelId());
        Assert.assertEquals("orgKey", bulk.getOrganizationKey());
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
        }
    }

    @Test
    public void testFullLog() throws IOException, ExecutionException, InterruptedException {
        List<Map<String, ?>> features = new ArrayList<>();
        features.add(new HashMap<String, Object>(){{
            put("days", 5.0);
            put("is_organic", 0l);
        }});
        features.add(new HashMap<String, Object>(){{
            put("days", 4.5);
            put("is_organic", 1l);
        }});
        features.add(new HashMap<String, Object>(){{
            put("days", 2.0);
            put("is_organic", 0l);
        }});
        List<String> predictionLabels = Arrays.asList("ripe", "not-ripe", "not-ripe");
        List<String> actualLabels = Arrays.asList("not-ripe", "not-ripe", "not-ripe");
        List<Map<String, Double>> shapValues = new ArrayList<>();
        shapValues.add(new HashMap<String, Double>(){{
            put("days", 2.0);
            put("is_organic", -1.2);
        }});
        shapValues.add(new HashMap<String, Double>(){{
            put("days", 2.2);
            put("is_organic", -1.2);
        }});
        shapValues.add(new HashMap<String, Double>(){{
            put("days", 2.5);
            put("is_organic", -1.2);
        }});

        Response response = client.bulkLog("modelId", "modelVersion", expectedIds, features, predictionLabels, actualLabels, shapValues, null);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server: " + e.getMessage());
        }

        Public.BulkRecord bulk = bulkPosts.get(0);
        Assert.assertEquals("modelId", bulk.getModelId());
        Assert.assertEquals("orgKey", bulk.getOrganizationKey());
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
        features.add(new HashMap<String, Object>(){{
            put("days", 5.0);
            put("is_organic", 0l);
        }});
        features.add(new HashMap<String, Object>(){{
            put("days", 4.5);
            put("is_organic", 1l);
        }});
        features.add(new HashMap<String, Object>(){{
            put("days", 2.0);
            put("is_organic", 0l);
        }});
        List<String> predictionLabels = Arrays.asList("ripe", "not-ripe", "not-ripe");
        List<String> actualLabels = Arrays.asList("not-ripe", "not-ripe", "not-ripe");

        Response response = client.logTrainingRecords("modelId", "modelVersion", features, predictionLabels, actualLabels);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server: " + e.getMessage());
        }

        Headers headers = this.headers.get(0);
        Assert.assertEquals("apiKey", headers.get("Authorization").get(0));
        Assert.assertEquals("orgKey", headers.get("Grpc-Metadata-organization").get(0));

        for (int i = 0; i < preProductionRecords.size(); i++) {
            Public.PreProductionRecord preprodRec = preProductionRecords.get(i);
            Record record = preprodRec.getTrainingRecord().getRecord();
            Assert.assertEquals("modelId", record.getModelId());
            Assert.assertEquals("modelVersion", record.getPredictionAndActual().getPrediction().getModelVersion());


            // For now training records dont include a prediction id
            Assert.assertEquals("", record.getPredictionId());

            //compare input features to posted Public.Record
            Map<String, ?> f = features.get(i);
            Assert.assertEquals(f.get("days"), record.getPredictionAndActual().getPrediction().getFeaturesOrDefault("days", Public.Value.getDefaultInstance()).getDouble());
            Assert.assertEquals(f.get("is_organic"), record.getPredictionAndActual().getPrediction().getFeaturesOrDefault("is_organic", Public.Value.getDefaultInstance()).getInt());

            //compare input prediction labels to posted Public.Record
            Assert.assertEquals(predictionLabels.get(i), record.getPredictionAndActual().getPrediction().getLabel().getCategorical());

            //compare input actual labels to posted Public.Record
            Assert.assertEquals(actualLabels.get(i), record.getPredictionAndActual().getActual().getLabel().getCategorical());
        }
    }

    @Test
    public void testLogValidation() throws IOException, ExecutionException, InterruptedException {
        List<Map<String, ?>> features = new ArrayList<>();
        features.add(new HashMap<String, Object>(){{
            put("days", 5.0);
            put("is_organic", 0l);
        }});
        features.add(new HashMap<String, Object>(){{
            put("days", 4.5);
            put("is_organic", 1l);
        }});
        features.add(new HashMap<String, Object>(){{
            put("days", 2.0);
            put("is_organic", 0l);
        }});
        List<String> predictionLabels = Arrays.asList("ripe", "not-ripe", "not-ripe");
        List<String> actualLabels = Arrays.asList("not-ripe", "not-ripe", "not-ripe");

        Response response = client.logValidationRecords("modelId", "modelVersion", "offline-1", features, predictionLabels, actualLabels);
        try {
            response.resolve(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Assert.fail("timeout waiting for server: " + e.getMessage());
        }

        Headers headers = this.headers.get(0);
        Assert.assertEquals("apiKey", headers.get("Authorization").get(0));
        Assert.assertEquals("orgKey", headers.get("Grpc-Metadata-organization").get(0));

        for (int i = 0; i < preProductionRecords.size(); i++) {
            Public.PreProductionRecord preprodRec = preProductionRecords.get(i);
            Assert.assertEquals("offline-1", preprodRec.getValidationRecord().getBatchId());
            Record record = preprodRec.getValidationRecord().getRecord();
            Assert.assertEquals("modelId", record.getModelId());
            Assert.assertEquals("modelVersion", record.getPredictionAndActual().getPrediction().getModelVersion());


            // For now training records dont include a prediction id
            Assert.assertEquals("", record.getPredictionId());

            //compare input features to posted Public.Record
            Map<String, ?> f = features.get(i);
            Assert.assertEquals(f.get("days"), record.getPredictionAndActual().getPrediction().getFeaturesOrDefault("days", Public.Value.getDefaultInstance()).getDouble());
            Assert.assertEquals(f.get("is_organic"), record.getPredictionAndActual().getPrediction().getFeaturesOrDefault("is_organic", Public.Value.getDefaultInstance()).getInt());

            //compare input prediction labels to posted Public.Record
            Assert.assertEquals(predictionLabels.get(i), record.getPredictionAndActual().getPrediction().getLabel().getCategorical());

            //compare input actual labels to posted Public.Record
            Assert.assertEquals(actualLabels.get(i), record.getPredictionAndActual().getActual().getLabel().getCategorical());
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
