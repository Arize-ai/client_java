package com.arize;

import com.arize.protocol.Public;
import com.arize.protocol.Public.BulkRecord;
import com.arize.protocol.Public.Record;
import com.google.protobuf.util.Timestamps;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArizeClient implements ArizeAPI {

    private static final String DEFAULT_URI = "https://api.arize.com/v1";

    /**
     * The URI to which to connect for single records.
     */
    private final URI host;

    /**
     * The URI to which to connect for bulk records.
     */
    private final URI bulkHost;

    /**
     * Training/Validation record endpoint.
     */
    private final URI trainingValidationHost;

    /**
     * The Arize api key for the corresponding organization.
     */
    private final String apiKey;

    /**
     * The Arize organization key
     */
    private final String organizationKey;

    /**
     * The HTTP client.
     */
    private final CloseableHttpAsyncClient client;

    /**
     * Constructor for passing in an httpClient, typically for mocking.
     *
     * @param client an Apache CloseableHttpAsyncClient
     * @param uri    uri for Arize endpoint
     * @throws URISyntaxException if uri string violates RFC 2396
     */
    public ArizeClient(final CloseableHttpAsyncClient client, final String apiKey, final String organizationKey, final String uri) throws URISyntaxException {
        this.client = client;
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("apiKey cannot be null or empty");
        }
        if (organizationKey == null || organizationKey.isEmpty()) {
            throw new IllegalArgumentException("organizationKey cannot be null or empty");
        }
        this.apiKey = apiKey;
        this.organizationKey = organizationKey;
        this.host = new URI(uri + "/log");
        this.bulkHost = new URI(uri + "/bulk");
        this.trainingValidationHost = new URI(uri + "/preprod");
        this.client.start();
    }

    /**
     * Construct a new API wrapper.
     *
     * @param apiKey          your Arize API Key
     * @param organizationKey your Arize organization key
     * @throws URISyntaxException if uri string violates RFC 2396
     */
    public ArizeClient(final String apiKey, final String organizationKey) throws URISyntaxException {
        this(HttpAsyncClients.createDefault(), apiKey, organizationKey, DEFAULT_URI);
    }

    /**
     * Construct a new API wrapper while overwriting the Arize endpoint, typically
     * for custom integrations and e2e testing.
     *
     * @param apiKey          your Arize API Key
     * @param organizationKey your Arize organization key
     * @param uri             uri for Arize endpoint
     * @throws URISyntaxException if uri string violates RFC 2396
     */
    public ArizeClient(final String apiKey, final String organizationKey, final String uri) throws URISyntaxException {
        this(HttpAsyncClients.createDefault(), apiKey, organizationKey, uri);
    }

    /**
     * Get the host.
     *
     * @return the host
     */
    public URI getHost() {
        return this.host;
    }

    /**
     * Get the organization key in the Arize platform
     *
     * @return the Arize supplied organization key
     */
    public String getOrganizationKey() {
        return organizationKey;
    }


    /**
     * {@inheritDoc}
     *
     * log constructs a record and executes the API call asynchronously returning a future.
     */
    @Override
    public <T> Response log(String modelId, String modelVersion, String predictionId, Map<String, ?> features, T predictionLabel, T actualLabel, Map<String, Double> shapValues, long predictionTimestamp) throws IOException, IllegalArgumentException {
        if (modelId == null || modelId.isEmpty()) {
            throw new IllegalArgumentException("modelId cannot be null or empty");
        }
        if (predictionId == null || predictionId.length() == 0) {
            throw new IllegalArgumentException("predictionId cannot be null or empty");
        }
        Record.Builder builder = Record.newBuilder();
        builder.setModelId(modelId);
        builder.setPredictionId(predictionId);
        builder.setOrganizationKey(this.organizationKey);

        if (predictionLabel != null) {
            Public.Prediction.Builder predictionBuilder = Public.Prediction.newBuilder();
            predictionBuilder.setLabel(RecordUtil.convertLabel(predictionLabel));
            if (modelVersion != null) {
                predictionBuilder.setModelVersion(modelVersion);
            }
            if (features != null) {
                predictionBuilder.putAllFeatures(RecordUtil.convertFeatures(features));
            }
            if (predictionTimestamp != 0) {
                predictionBuilder.setTimestamp(Timestamps.fromMillis(predictionTimestamp));
            }
            builder.setPrediction(predictionBuilder);
        }
        if (actualLabel != null) {
            Public.Actual.Builder actualBuilder = Public.Actual.newBuilder();
            actualBuilder.setLabel(RecordUtil.convertLabel(actualLabel));
            if (predictionTimestamp != 0) {
                actualBuilder.setTimestamp(Timestamps.fromMillis(predictionTimestamp));
            }
            builder.setActual(actualBuilder);
        }
        if (shapValues != null && !shapValues.isEmpty()) {
            Public.FeatureImportances.Builder featureImportancesBuilder = Public.FeatureImportances.newBuilder();
            if (modelVersion != null) {
                featureImportancesBuilder.setModelVersion(modelVersion);
            }
            if (predictionTimestamp != 0) {
                featureImportancesBuilder.setTimestamp(Timestamps.fromMillis(predictionTimestamp));
            }
            featureImportancesBuilder.putAllFeatureImportances(shapValues);
            builder.setFeatureImportances(featureImportancesBuilder);
        }
        HttpPost req = buildRequest(RecordUtil.toJSON(builder.build()), this.host, this.apiKey, this.organizationKey);
        return new Response(client.execute(req, null));
    }

    /**
     * {@inheritDoc}
     *
     * bulkLog constructs a bulk record and executes the API call asynchronously returning a future response.
     */
    @Override
    public <T> Response bulkLog(String modelId, String modelVersion, List<String> predictionIds, List<Map<String, ?>> features, List<T> predictionLabels, List<T> actualLabels, List<Map<String, Double>> shapValues, List<Long> predictionTimestamps) throws IOException, IllegalArgumentException {
        if (modelId == null || modelId.isEmpty()) {
            throw new IllegalArgumentException("modelId cannot be null or empty");
        }
        if (predictionIds == null || predictionIds.isEmpty()) {
            throw new IllegalArgumentException("predictionIds cannot be null or empty");
        }
        if (predictionLabels != null && predictionIds.size() != predictionLabels.size()) {
            throw new IllegalArgumentException("predictionIds.size() must equal predictionLabels.size()");
        }
        if (actualLabels != null && predictionIds.size() != actualLabels.size()) {
            throw new IllegalArgumentException("predictionIds.size() must equal actualLabels.size()");
        }
        if (features != null && predictionIds.size() != features.size()) {
            throw new IllegalArgumentException("predictionIds.size() must equal features.size()");
        }
        if (shapValues!= null && predictionIds.size() != shapValues.size()) {
            throw new IllegalArgumentException("predictionIds.size() must equal shapValues.size()");
        }
        if (predictionTimestamps != null && predictionIds.size() != predictionTimestamps.size()) {
            throw new IllegalArgumentException("predictionIds.size() must equal predictionTimestamps.size()");
        }
        BulkRecord.Builder builder = BulkRecord.newBuilder();
        builder.setModelId(modelId);
        builder.setOrganizationKey(organizationKey);
        if (modelVersion != null) {
            builder.setModelVersion(modelVersion);
        }
        for (int index = 0; index < predictionIds.size(); index++) {
            Record.Builder recordBuilder = Record.newBuilder();
            recordBuilder.setModelId(modelId);
            final String predictionId = predictionIds.get(index);
            recordBuilder.setPredictionId(predictionId);
            if (predictionLabels != null) {
                Public.Prediction.Builder predictionBuilder = Public.Prediction.newBuilder();
                predictionBuilder.setLabel(RecordUtil.convertLabel(predictionLabels.get(index)));
                if (modelVersion != null) {
                    predictionBuilder.setModelVersion(modelVersion);
                }
                if (features != null) {
                    predictionBuilder.putAllFeatures(RecordUtil.convertFeatures(features.get(index)));
                }
                if (predictionTimestamps != null) {
                    predictionBuilder.setTimestamp(Timestamps.fromMillis(predictionTimestamps.get(index)));
                }
                recordBuilder.setPrediction(predictionBuilder);
            }
            if (actualLabels != null) {
                Public.Actual.Builder actualBuilder = Public.Actual.newBuilder();
                actualBuilder.setLabel(RecordUtil.convertLabel(actualLabels.get(index)));
                if (predictionTimestamps != null) {
                    actualBuilder.setTimestamp(Timestamps.fromMillis(predictionTimestamps.get(index)));
                }
                recordBuilder.setActual(actualBuilder);
            }
            if (shapValues != null) {
                Public.FeatureImportances.Builder featureImportancesBuilder = Public.FeatureImportances.newBuilder();
                featureImportancesBuilder.putAllFeatureImportances(shapValues.get(index));
                if (modelVersion != null) {
                    featureImportancesBuilder.setModelVersion(modelVersion);
                }
                if (predictionTimestamps != null) {
                    featureImportancesBuilder.setTimestamp(Timestamps.fromMillis(predictionTimestamps.get(index)));
                }
                recordBuilder.setFeatureImportances(featureImportancesBuilder);
            }
            builder.addRecords(recordBuilder);
        }
        final HttpPost request = buildRequest(RecordUtil.toJSON(builder.build()), this.bulkHost, this.apiKey, this.organizationKey);
        return new Response(client.execute(request, null));
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public <T> Response logTrainingRecords(String modelId, String modelVersion, List<Map<String, ?>> features, List<T> predictionLabels, List<T> actualLabels) throws IOException {
        if (modelId == null || modelId.isEmpty()) {
            throw new IllegalArgumentException("modelId cannot be null or empty");
        }
        if (predictionLabels == null || predictionLabels.isEmpty()) {
            throw new IllegalArgumentException("predictionLabels cannot be null or empty");
        }
        if (features != null && features.size() != predictionLabels.size()) {
            throw new IllegalArgumentException("if specified, features list must be the same length as predictionLabels");
        }
        if (actualLabels == null || actualLabels.size() != predictionLabels.size()) {
            throw new IllegalArgumentException("actualLabels cannot be null and must be the same length as predictionLabels");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < predictionLabels.size(); i++) {
            Public.PreProductionRecord.Builder pprBuilder = Public.PreProductionRecord.newBuilder();
            Public.PreProductionRecord.TrainingRecord.Builder trBuilder = Public.PreProductionRecord.TrainingRecord.newBuilder();
            Record.Builder recordBuilder = Record.newBuilder();
            recordBuilder.setModelId(modelId);

            Public.PredictionAndActual.Builder panda = Public.PredictionAndActual.newBuilder();
            Public.Prediction.Builder predictionBuilder = Public.Prediction.newBuilder();
            predictionBuilder.setLabel(RecordUtil.convertLabel(predictionLabels.get(i)));
            if (modelVersion != null) {
                predictionBuilder.setModelVersion(modelVersion);
            }
            if (features != null) {
                predictionBuilder.putAllFeatures(RecordUtil.convertFeatures(features.get(i)));
            }
            panda.setPrediction(predictionBuilder);

            Public.Actual.Builder actualBuilder = Public.Actual.newBuilder();
            actualBuilder.setLabel(RecordUtil.convertLabel(actualLabels.get(i)));
            panda.setActual(actualBuilder);

            recordBuilder.setPredictionAndActual(panda);
            trBuilder.setRecord(recordBuilder);

            pprBuilder.setTrainingRecord(trBuilder);
            sb.append(RecordUtil.toJSON(pprBuilder.build()));
            sb.append('\n');
        }
        final HttpPost request = buildRequest(sb.toString(), this.trainingValidationHost, this.apiKey, this.organizationKey);
        return new Response(client.execute(request, null));
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public <T> Response logValidationRecords(String modelId, String modelVersion, String batchId, List<Map<String, ?>> features, List<T> predictionLabels, List<T> actualLabels) throws IOException {
        if (modelId == null || modelId.isEmpty()) {
            throw new IllegalArgumentException("modelId cannot be null or empty");
        }
        if (batchId == null || batchId.isEmpty()) {
            throw new IllegalArgumentException("batchId cannot be null or empty");
        }
        if (predictionLabels == null || predictionLabels.isEmpty()) {
            throw new IllegalArgumentException("predictionLabels cannot be null or empty");
        }
        if (features != null && features.size() != predictionLabels.size()) {
            throw new IllegalArgumentException("if specified, features list must be the same length as predictionLabels");
        }
        if (actualLabels == null || actualLabels.size() != predictionLabels.size()) {
            throw new IllegalArgumentException("actualLabels cannot be null and must be the same length as predictionLabels");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < predictionLabels.size(); i++) {
            Public.PreProductionRecord.Builder pprBuilder = Public.PreProductionRecord.newBuilder();
            Public.PreProductionRecord.ValidationRecord.Builder vrBuilder = Public.PreProductionRecord.ValidationRecord.newBuilder();
            vrBuilder.setBatchId(batchId);

            Record.Builder recordBuilder = Record.newBuilder();
            recordBuilder.setModelId(modelId);

            Public.PredictionAndActual.Builder panda = Public.PredictionAndActual.newBuilder();
            Public.Prediction.Builder predictionBuilder = Public.Prediction.newBuilder();
            predictionBuilder.setLabel(RecordUtil.convertLabel(predictionLabels.get(i)));
            if (modelVersion != null) {
                predictionBuilder.setModelVersion(modelVersion);
            }
            if (features != null) {
                predictionBuilder.putAllFeatures(RecordUtil.convertFeatures(features.get(i)));
            }
            panda.setPrediction(predictionBuilder);

            Public.Actual.Builder actualBuilder = Public.Actual.newBuilder();
            actualBuilder.setLabel(RecordUtil.convertLabel(actualLabels.get(i)));
            panda.setActual(actualBuilder);

            recordBuilder.setPredictionAndActual(panda);
            vrBuilder.setRecord(recordBuilder);

            pprBuilder.setValidationRecord(vrBuilder);
            sb.append(RecordUtil.toJSON(pprBuilder.build()));
            sb.append('\n');
        }
        final HttpPost request = buildRequest(sb.toString(), this.trainingValidationHost, this.apiKey, this.organizationKey);
        return new Response(client.execute(request, null));
    }

    /**
     * {@inheritDoc}
     *
     * logPrediction builds a Prediction record and executes the API call
     * asynchronously returning a future response.
     */
    @Deprecated
    @Override
    @SuppressWarnings({ "unchecked" })
    public <T> Response logPrediction(final String modelId, final String modelVersion, final String predictionId,
            final T predictionLabel, final Map<String, ?>... features) throws IOException, IllegalArgumentException {
        Map<String, Object> feats = new HashMap<>();
        for (Map<String, ?> f : features) {
            feats.putAll(f);
        }
        return log(modelId, modelVersion, predictionId, feats, predictionLabel, null, null, 0);
    }

    /**
     * {@inheritDoc}
     *
     * logPrediction builds a Prediction record with a overwritten timestamp and
     * executes the API call asynchronously returning a future response.
     */
    @Deprecated
    @Override
    @SuppressWarnings({ "unchecked" })
    public <T> Response logPrediction(final String modelId, final String modelVersion, final String predictionId,
            final T predictionLabel, final long timeOverwrite, final Map<String, ?>... features)
            throws IOException, IllegalArgumentException {
        Map<String, Object> feats = new HashMap<>();
        for (Map<String, ?> f : features) {
            feats.putAll(f);
        }
        return log(modelId, modelVersion, predictionId, feats, predictionLabel, null, null, timeOverwrite);
    }

    /**
     * {@inheritDoc}
     *
     * logActual builds an Actual record and executes the API call asynchronously
     * returning a future response.
     */
    @Deprecated
    @Override
    public <T> Response logActual(final String modelId, final String predictionId, final T actualLabel)
            throws IOException, IllegalArgumentException {
        Map<String, Object> feats = new HashMap<>();
        return log(modelId, "", predictionId, null, null, actualLabel, null, 0);

    }

    /**
     * {@inheritDoc}
     *
     * logBulkPrediction builds a Bulk Prediction record and executes the API call
     * asynchronously returning a future response.
     */
    @Deprecated
    @Override
    @SuppressWarnings({ "unchecked" })
    public <T> Response logBulkPrediction(final String modelId, final String modelVersion,
            final List<String> predictionIds, final List<T> predictionLabels, final List<Long> timesOverwriteMillis,
            final List<Map<String, ?>>... features) throws IOException, IllegalArgumentException {
        List<Map<String, ?>> featuresColumn = new ArrayList<>();
        for (List<Map<String, ?>> f : features) {
            featuresColumn.addAll(f);
        }
        return bulkLog(modelId, modelVersion, predictionIds, featuresColumn, predictionLabels, null, null, timesOverwriteMillis);
    }

    /**
     * {@inheritDoc}
     *
     * logBulkActual builds a Bulk Actual record and executes the API call
     * asynchronously returning a future response.
     */
    @Deprecated
    @Override
    public <T> Response logBulkActual(final String modelId, final List<String> predictionIds,
            final List<T> actualLabels) throws IOException, IllegalArgumentException {
        return bulkLog(modelId, "", predictionIds, null, null, actualLabels, null, null);
    }

    /**
     * Closes the http client.
     *
     * @throws IOException in case of a network error
     */
    public void close() throws IOException {
        this.client.close();
    }

    protected static HttpPost buildRequest(final String body, final URI host, String apiKey, String organizationKey) {
        final HttpPost request = new HttpPost();
        request.setEntity(new StringEntity(body, Charset.forName("UTF-8")));
        request.setURI(host);
        request.addHeader("Authorization", apiKey);
        request.addHeader("Grpc-Metadata-organization", organizationKey);
        request.addHeader("Grpc-Metadata-sdk", "jvm");
        request.addHeader("Grpc-Metadata-sdk-version", SDK_VERSION);
        return request;
    }

    public static class ScoredCategorical {
        private String category;
        private double score;
        List<Double> numSeq;
        
        public ScoredCategorical(String category, double score) {
            this.category = category;
            this.score = score;
        }

        public ScoredCategorical(String category, double score, List<Double> numSeq) {
            this.category = category;
            this.score = score;
            this.numSeq = numSeq;
        }

        public String getCategory() {
            return category;
        }

        public double getScore() {
            return score;
        }
        
        public List<Double> getNumericSequence() {
            return numSeq;
        }
    }
}
