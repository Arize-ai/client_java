package com.arize;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.arize.protocol.Public.Label;
import com.arize.protocol.Public.Record;
import com.arize.protocol.Public.Value;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

public class ArizeClient implements ArizeAPI {

    /**
     * The URI to which to connect.
     */
    private final URI host;

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
     * @throws URISyntaxException
     */
    public ArizeClient(final CloseableHttpAsyncClient client, final String uri) throws URISyntaxException {
        this.client = client;
        this.apiKey = "";
        this.organizationKey = "";
        this.host = new URI(uri);
        this.client.start();
    }

    /**
     * Construct a new API wrapper.
     * 
     * @param apiKey          your Arize API Key
     * @param organizationKey your Arize organization key
     * @throws URISyntaxException
     */
    public ArizeClient(final String apiKey, final String organizationKey) throws URISyntaxException {
        this.client = HttpAsyncClients.createDefault();
        this.host = new URI("https://api.arize.com/v1/log");
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
     * @throws URISyntaxException
     */
    public ArizeClient(final String apiKey, final String organizationKey, final String uri) throws URISyntaxException {
        this.client = HttpAsyncClients.createDefault();
        this.host = new URI(uri);
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

        final Record record = this.buildPrediction(modelId, modelVersion, predictionId, predictionLabel, null,
                features);
        final HttpPost request = buildRequest(RecordUtil.recordToJson(record));

        request.setURI(this.host);
        request.addHeader("Authorization", this.apiKey);
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

        final Record record = this.buildPrediction(modelId, modelVersion, predictionId, predictionLabel, timeOverwrite,
                features);
        final HttpPost request = buildRequest(RecordUtil.recordToJson(record));

        request.setURI(this.host);
        request.addHeader("Authorization", this.apiKey);
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

        final Record record = this.buildActual(modelId, predictionId, actualLabel);
        final HttpPost request = buildRequest(RecordUtil.recordToJson(record));

        request.setURI(this.host);
        request.addHeader("Authorization", this.apiKey);
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
     * @param features        Optional {@link Map} varargs containing human readable
     *                        and debuggable model features. Map key must be
     *                        {@link String} and values are one of: string, int,
     *                        long, short, float, double.
     * @return Record
     * @throws IllegalArgumentException in case data type for features or label are
     *                                  not supported.
     */
    @SuppressWarnings({ "unchecked" })
    protected <T> Record buildPrediction(final String modelId, final String modelVersion, final String predictionId,
            final T predictionLabel, final Long timeOverwrite, final Map<String, ?>... features)
            throws IllegalArgumentException {

        RecordUtil.validateInputs(modelId, predictionId, predictionLabel);

        final Record.Builder baseRecord = RecordUtil.initializeRecord(this.organizationKey, modelId, predictionId);
        final Map<String, Value> feature = new HashMap<>();
        if (features != null) {
            for (final Map<String, ?> featureMap : features) {
                if (featureMap != null) {
                    feature.putAll(RecordUtil.convertFeatures(featureMap));
                }
            }
        }
        final Label label = RecordUtil.convertLabel(predictionLabel);
        return RecordUtil.decoratePrediction(baseRecord, modelVersion, label, feature, timeOverwrite);
    }

    /**
     * Builds an Actual record
     * 
     * @param <T>          Boxed type for actualLabel. Supported boxed types are:
     *                     boolean, string, int, long, short, float, double.
     * @param modelId      Unique identifier for a given model.
     * @param predictionId Unique indentifier for a given actual record. This value
     *                     is used to match the record to their original prediction.
     * @param actualLabel  The actual "truth" label for the original prediction.
     *                     Supported boxed types are: boolean, string, int, long,
     *                     short, float, double.
     * @return Record
     * @throws IllegalArgumentException in case data type for features or label are
     *                                  not supported.
     */
    protected <T> Record buildActual(final String modelId, final String predictionId, final T actualLabel)
            throws IllegalArgumentException {

        RecordUtil.validateInputs(modelId, predictionId, actualLabel);

        final Record.Builder baseRecord = RecordUtil.initializeRecord(this.organizationKey, modelId, predictionId);
        final Label label = RecordUtil.convertLabel(actualLabel);
        return RecordUtil.decorateActual(baseRecord, label);
    }

    protected static HttpPost buildRequest(final String body) {
        final HttpPost req = new HttpPost();
        req.setEntity(new StringEntity(body, Charset.forName("UTF-8")));
        return req;
    }
}