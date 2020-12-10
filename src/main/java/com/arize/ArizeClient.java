package com.arize;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.arize.protocol.Public.BulkRecord;
import com.arize.protocol.Public.Label;
import com.arize.protocol.Public.Record;
import com.arize.protocol.Public.Value;
import com.google.protobuf.Timestamp;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

public class ArizeClient implements ArizeAPI {

    /**
     * The URI to which to connect for single records.
     */
    private final URI host;

    /**
     * The URI to which to connect for bulk records.
     */
    private final URI bulkHost;

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
    public ArizeClient(final CloseableHttpAsyncClient client, final String uri) throws URISyntaxException {
        this.client = client;
        this.apiKey = "";
        this.organizationKey = "";
        this.host = new URI(uri + "/log");
        this.bulkHost = new URI(uri + "/bulk");
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
        this.client = HttpAsyncClients.createDefault();
        this.host = new URI("https://api.arize.com/v1/log");
        this.bulkHost = new URI("https://api.arize.com/v1/bulk");
        this.apiKey = apiKey;
        this.organizationKey = organizationKey;
        this.client.start();
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
        this.client = HttpAsyncClients.createDefault();
        this.host = new URI(uri + "/log");
        this.bulkHost = new URI(uri + "/bulk");
        this.apiKey = apiKey;
        this.organizationKey = organizationKey;
        this.client.start();
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
     * logPrediction builds a Prediction record and executes the API call
     * asynchronously returning a future response.
     */
    @Override
    @SuppressWarnings({ "unchecked" })
    public <T> Response logPrediction(final String modelId, final String modelVersion, final String predictionId,
            final T predictionLabel, final Map<String, ?>... features) throws IOException, IllegalArgumentException {
        RecordUtil.validateInputs(modelId, predictionId, predictionLabel);
        final List<Map<String, ?>> feats = new ArrayList<Map<String, ?>>();
        for (final Map<String, ?> feature : features) {
            feats.add(feature);
        }
        final Record record = this.buildPrediction(modelId, modelVersion, predictionId, predictionLabel,
                null, feats);
        final HttpPost request = buildRequest(RecordUtil.recordToJson(record), this.host, this.apiKey);
        final Future<HttpResponse> future = client.execute(request, null);
        return new Response(future);
    }

    /**
     * {@inheritDoc}
     * 
     * logPrediction builds a Prediction record with a overwritten timestamp and
     * executes the API call asynchronously returning a future response.
     */
    @Override
    @SuppressWarnings({ "unchecked" })
    public <T> Response logPrediction(final String modelId, final String modelVersion, final String predictionId,
            final T predictionLabel, final long timeOverwrite, final Map<String, ?>... features)
            throws IOException, IllegalArgumentException {

        RecordUtil.validateInputs(modelId, predictionId, predictionLabel);
        final List<Map<String, ?>> feats = new ArrayList<Map<String, ?>>();
        for (final Map<String, ?> feature : features) {
            feats.add(feature);
        }
        final Record record = this.buildPrediction(modelId, modelVersion, predictionId, predictionLabel, timeOverwrite,
                feats);
        final HttpPost request = buildRequest(RecordUtil.recordToJson(record), this.host, this.apiKey);
        final Future<HttpResponse> future = client.execute(request, null);
        return new Response(future);
    }

    /**
     * {@inheritDoc}
     * 
     * logActual builds an Actual record and executes the API call asynchronously
     * returning a future response.
     */
    @Override
    public <T> Response logActual(final String modelId, final String predictionId, final T actualLabel)
            throws IOException, IllegalArgumentException {
        RecordUtil.validateInputs(modelId, predictionId, actualLabel);
        final Record record = this.buildActual(modelId, predictionId, actualLabel, null);
        final HttpPost request = buildRequest(RecordUtil.recordToJson(record), this.host, this.apiKey);
        final Future<HttpResponse> future = client.execute(request, null);
        return new Response(future);
    }

    /**
     * {@inheritDoc}
     * 
     * logBulkPrediction builds a Bulk Prediction record and executes the API call
     * asynchronously returning a future response.
     */
    @Override
    @SuppressWarnings({ "unchecked" })
    public <T> Response logBulkPrediction(final String modelId, final String modelVersion,
            final List<String> predictionIds, final List<T> predictionLabels, final List<Long> timesOverwriteMillis,
            final List<Map<String, ?>>... features) throws IOException, IllegalArgumentException {
        RecordUtil.validateBulkInputs(modelId, predictionIds, predictionLabels);
        final BulkRecord record = this.buildBulkPrediction(modelId, modelVersion, predictionIds, predictionLabels,
                timesOverwriteMillis, features);
        final HttpPost request = buildRequest(RecordUtil.recordToJson(record), this.bulkHost, this.apiKey);  
        final Future<HttpResponse> future = client.execute(request, null);
        return new Response(future);
    }

    /**
     * {@inheritDoc}
     * 
     * logBulkActual builds a Bulk Actual record and executes the API call
     * asynchronously returning a future response.
     */
    @Override
    public <T> Response logBulkActual(final String modelId, final List<String> predictionIds,
            final List<T> actualLabels) throws IOException, IllegalArgumentException {
        RecordUtil.validateBulkInputs(modelId, predictionIds, actualLabels);
        final BulkRecord.Builder builder = RecordUtil.initializeBulkRecord(this.organizationKey, modelId, null);
        for (int index = 0; index < predictionIds.size(); index++) {
            final String predictionId = predictionIds.get(index);
            final T actualLabel = actualLabels.get(index);
            final Record record = this.buildActual(modelId, predictionId, actualLabel, null);
            builder.addRecords(record);
        }
        final BulkRecord record = builder.build();
        final HttpPost request = buildRequest(RecordUtil.recordToJson(record), this.bulkHost, this.apiKey);
        final Future<HttpResponse> future = client.execute(request, null);
        return new Response(future);
    }

    /**
     * Closes the http client.
     *
     * @throws IOException in case of a network error
     */
    public void close() throws IOException {
        this.client.close();
    }

    /**
     * Builds a Prediction record
     * 
     * @param <T>             Boxed type for predictionLabel. Supported boxed types
     *                        are: boolean, string, int, long, short, float, double.
     * @param modelId         Unique identifier for a given model.
     * @param modelVersion    Optional identifier used to group together a subset of
     *                        predictions and actuals for a given modelId.
     * @param predictionId    Unique indentifier for a given prediction. This value
     *                        is used to match latent actual labels to their
     *                        original prediction.
     * @param predictionLabel The predicted value for a given set of model inputs.
     *                        Supported boxed types are: boolean, string, int, long,
     *                        short, float, double.
     * @param timestampMillis Optional {@link Long} unix epoch time in milliseconds
     *                        used to overwrite timestamp for a prediction.
     * @param features        Optional {@link Map} varargs containing human readable
     *                        and debuggable model features. Map key must be
     *                        {@link String} and values are one of: string, int,
     *                        long, short, float, double.
     * @return {@link Response}
     * @throws IllegalArgumentException in case data type for features or label are
     *                                  not supported.
     */
    protected <T> Record buildPrediction(final String modelId, final String modelVersion, final String predictionId,
            final T predictionLabel, final Long timestampMillis, final List<Map<String, ?>> features)
            throws IllegalArgumentException {

        final Record.Builder baseRecord = RecordUtil.initializeRecord(this.organizationKey, modelId, predictionId);
        final Map<String, Value> feature = RecordUtil.getFeatureMap(features);
        final Label label = RecordUtil.convertLabel(predictionLabel);
        Timestamp timestamp = null;
        if (timestampMillis != null) {
            timestamp = RecordUtil.getTimestamp(timestampMillis);
        }
        return RecordUtil.decoratePrediction(baseRecord, modelVersion, label, feature, timestamp);
    }

    /**
     * Builds an Actual record
     * 
     * @param <T>           Boxed type for actualLabel. Supported boxed types are:
     *                      boolean, string, int, long, short, float, double.
     * @param modelId       Unique identifier for a given model.
     * @param predictionId  Unique indentifier for a given actual record. This value
     *                      is used to match the record to their original
     *                      prediction.
     * @param actualLabel   The actual "truth" label for the original prediction.
     *                      Supported boxed types are: boolean, string, int, long,
     *                      short, float, double.
     * @param timeOverwrite Optional {@link Long} unix epoch time in milliseconds
     *                      used to overwrite timestamp for a prediction.
     * @return {@link Response}
     * @throws IllegalArgumentException in case data type for features or label are
     *                                  not supported.
     */
    protected <T> Record buildActual(final String modelId, final String predictionId, final T actualLabel,
            final Long timeOverwrite) throws IllegalArgumentException {

        final Record.Builder baseRecord = RecordUtil.initializeRecord(this.organizationKey, modelId, predictionId);
        final Label label = RecordUtil.convertLabel(actualLabel);
        Timestamp timestamp = null;
        if (timeOverwrite != null) {
            timestamp = RecordUtil.getTimestamp(timeOverwrite);
        }
        return RecordUtil.decorateActual(baseRecord, label, timestamp);
    }

    @SuppressWarnings({ "unchecked" })
    protected <T> BulkRecord buildBulkPrediction(final String modelId, final String modelVersion,
            final List<String> predictionIds, final List<T> predictionLabels, final List<Long> timesOverwriteMillis,
            final List<Map<String, ?>>... features) {
        final BulkRecord.Builder builder = RecordUtil.initializeBulkRecord(this.organizationKey, modelId, modelVersion);
        for (int index = 0; index < predictionIds.size(); index++) {
            final String predictionId = predictionIds.get(index);
            final T predictionLabel = predictionLabels.get(index);
            Long timeOverwrite = null;
            if (timesOverwriteMillis != null && !timesOverwriteMillis.isEmpty()) {
                timeOverwrite = timesOverwriteMillis.get(index);
            }
            final List<Map<String, ?>> feats = new ArrayList<Map<String, ?>>();
            for (final List<Map<String, ?>> feature : features) {
                feats.add(feature.get(index));
            }
            final Record record = this.buildPrediction(null, null, predictionId, predictionLabel, timeOverwrite, feats);
            builder.addRecords(record);
        }
        return builder.build();
    }

    protected <T> BulkRecord buildBulkActuals(final String modelId, final List<String> predictionIds,
            final List<T> actualLabels) {
        final BulkRecord.Builder builder = RecordUtil.initializeBulkRecord(this.organizationKey, modelId, null);
        for (int index = 0; index < predictionIds.size(); index++) {
            final String predictionId = predictionIds.get(index);
            final T actualLabel = actualLabels.get(index);
            final Record record = this.buildActual(null, predictionId, actualLabel, null);
            builder.addRecords(record);
        }
        return builder.build();
    }

    protected static HttpPost buildRequest(final String body, final URI host, String apiKey) {
        final HttpPost request = new HttpPost();
        request.setEntity(new StringEntity(body, Charset.forName("UTF-8")));
        request.setURI(host);
        request.addHeader("Authorization", apiKey);
        return request;
    }
}