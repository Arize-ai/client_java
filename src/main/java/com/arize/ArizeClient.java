package com.arize;

import com.arize.protocol.Public;
import com.arize.protocol.Public.BulkRecord;
import com.arize.protocol.Public.Record;
import com.arize.types.Embedding;
import com.google.protobuf.util.Timestamps;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ArizeClient implements ArizeAPI {

  private static final String DEFAULT_URI = "https://api.arize.com/v1";

  /** The URI to which to connect for single records. */
  private final URI host;

  /** The URI to which to connect for bulk records. */
  private final URI bulkHost;

  /** Training/Validation record endpoint. */
  private final URI trainingValidationHost;

  /** The Arize api key for the corresponding space. */
  private final String apiKey;

  /** The Arize space key */
  private final String spaceKey;

  /** The HTTP client. */
  private final CloseableHttpAsyncClient client;

  /**
   * Constructor for passing in an httpClient, typically for mocking.
   *
   * @param client an Apache CloseableHttpAsyncClient
   * @param uri uri for Arize endpoint
   * @throws URISyntaxException if uri string violates RFC 2396
   */
  public ArizeClient(
      final CloseableHttpAsyncClient client,
      final String apiKey,
      final String spaceKey,
      final String uri)
      throws URISyntaxException {
    this.client = client;
    if (apiKey == null || apiKey.isEmpty()) {
      throw new IllegalArgumentException("apiKey cannot be null or empty");
    }
    if (spaceKey == null || spaceKey.isEmpty()) {
      throw new IllegalArgumentException("spaceKey cannot be null or empty");
    }
    this.apiKey = apiKey;
    this.spaceKey = spaceKey;
    this.host = new URI(uri + "/log");
    this.bulkHost = new URI(uri + "/bulk");
    this.trainingValidationHost = new URI(uri + "/preprod");
    this.client.start();
  }

  /**
   * Construct a new API wrapper.
   *
   * @param apiKey your Arize API Key
   * @param spaceKey your Arize space key
   * @throws URISyntaxException if uri string violates RFC 2396
   */
  public ArizeClient(final String apiKey, final String spaceKey) throws URISyntaxException {
    this(HttpAsyncClients.createDefault(), apiKey, spaceKey, DEFAULT_URI);
  }

  /**
   * Construct a new API wrapper while overwriting the Arize endpoint, typically for custom
   * integrations and e2e testing.
   *
   * @param apiKey your Arize API Key
   * @param spaceKey your Arize space key
   * @param uri uri for Arize endpoint
   * @throws URISyntaxException if uri string violates RFC 2396
   */
  public ArizeClient(final String apiKey, final String spaceKey, final String uri)
      throws URISyntaxException {
    this(HttpAsyncClients.createDefault(), apiKey, spaceKey, uri);
  }

  protected static HttpPost buildRequest(
      final String body, final URI host, String apiKey, String spaceKey) {
    final HttpPost request = new HttpPost();
    request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
    request.setURI(host);
    request.addHeader("Authorization", apiKey);
    request.addHeader("Grpc-Metadata-space", spaceKey);
    request.addHeader("Grpc-Metadata-sdk", "jvm");
    request.addHeader("Grpc-Metadata-sdk-version", SDK_VERSION);
    return request;
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
   * Get the space key in the Arize platform
   *
   * @return the Arize supplied space key
   */
  public String getSpaceKey() {
    return spaceKey;
  }

  /**
   * {@inheritDoc}
   *
   * <p>log constructs a record and executes the API call asynchronously returning a future.
   */
  @Override
  public <T> Response log(
      String modelId,
      String modelVersion,
      String predictionId,
      Map<String, ?> features,
      Map<String, Embedding> embeddingFeatures,
      Map<String, ?> tags,
      T predictionLabel,
      T actualLabel,
      Map<String, Double> shapValues,
      long predictionTimestamp)
      throws IOException, IllegalArgumentException {
    if (modelId == null || modelId.isEmpty()) {
      throw new IllegalArgumentException("modelId cannot be null or empty");
    }
    if (predictionId == null || predictionId.length() == 0) {
      throw new IllegalArgumentException("predictionId cannot be null or empty");
    }
    RecordUtil.validatePredictionActualMatches(predictionLabel, actualLabel);
    Record.Builder builder = Record.newBuilder();
    builder.setModelId(modelId);
    builder.setPredictionId(predictionId);
    builder.setSpaceKey(this.spaceKey);

    if (predictionLabel != null) {
      Public.Prediction.Builder predictionBuilder = Public.Prediction.newBuilder();
      predictionBuilder.setLabel(RecordUtil.convertLabel(predictionLabel));
      if (modelVersion != null) {
        predictionBuilder.setModelVersion(modelVersion);
      }
      if (features != null) {
        predictionBuilder.putAllFeatures(RecordUtil.convertFeatures(features));
      }
      if (embeddingFeatures != null) {
        predictionBuilder.putAllFeatures(RecordUtil.convertEmbeddingFeatures(embeddingFeatures));
      }
      if (tags != null) {
        predictionBuilder.putAllTags(RecordUtil.convertTags(tags));
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
      // Added to support latent tags on actuals.
      if (tags != null) {
        actualBuilder.putAllTags(RecordUtil.convertTags(tags));
      }
      builder.setActual(actualBuilder);
    }
    if (shapValues != null && !shapValues.isEmpty()) {
      Public.FeatureImportances.Builder featureImportancesBuilder =
          Public.FeatureImportances.newBuilder();
      if (modelVersion != null) {
        featureImportancesBuilder.setModelVersion(modelVersion);
      }
      if (predictionTimestamp != 0) {
        featureImportancesBuilder.setTimestamp(Timestamps.fromMillis(predictionTimestamp));
      }
      featureImportancesBuilder.putAllFeatureImportances(shapValues);
      builder.setFeatureImportances(featureImportancesBuilder);
    }
    HttpPost req =
        buildRequest(RecordUtil.toJSON(builder.build()), this.host, this.apiKey, this.spaceKey);
    return new Response(client.execute(req, null));
  }

  /**
   * {@inheritDoc}
   *
   * <p>bulkLog constructs a bulk record and executes the API call asynchronously returning a future
   * response.
   */
  @Override
  public <T> Response bulkLog(
      String modelId,
      String modelVersion,
      List<String> predictionIds,
      List<Map<String, ?>> features,
      List<Map<String, Embedding>> embeddingFeatures,
      List<Map<String, ?>> tags,
      List<T> predictionLabels,
      List<T> actualLabels,
      List<Map<String, Double>> shapValues,
      List<Long> predictionTimestamps)
      throws IOException, IllegalArgumentException {
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
    if (embeddingFeatures != null && predictionIds.size() != embeddingFeatures.size()) {
      throw new IllegalArgumentException(
          "predictionIds.size() must equal embeddingFeatures.size()");
    }
    if (tags != null && predictionIds.size() != tags.size()) {
      throw new IllegalArgumentException("predictionIds.size() must equal tags.size()");
    }
    if (shapValues != null && predictionIds.size() != shapValues.size()) {
      throw new IllegalArgumentException("predictionIds.size() must equal shapValues.size()");
    }
    if (predictionTimestamps != null && predictionIds.size() != predictionTimestamps.size()) {
      throw new IllegalArgumentException(
          "predictionIds.size() must equal predictionTimestamps.size()");
    }
    RecordUtil.validateBulkPredictionActualMatches(predictionLabels, actualLabels);
    BulkRecord.Builder builder = BulkRecord.newBuilder();
    builder.setModelId(modelId);
    builder.setSpaceKey(spaceKey);
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
        if (embeddingFeatures != null) {
          predictionBuilder.putAllFeatures(
              RecordUtil.convertEmbeddingFeatures(embeddingFeatures.get(index)));
        }
        if (tags != null) {
          predictionBuilder.putAllTags(RecordUtil.convertTags(tags.get(index)));
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
        // Added to support latent tags on actuals.
        if (tags != null) {
          actualBuilder.putAllTags(RecordUtil.convertTags(tags.get(index)));
        }
        recordBuilder.setActual(actualBuilder);
      }
      if (shapValues != null) {
        Public.FeatureImportances.Builder featureImportancesBuilder =
            Public.FeatureImportances.newBuilder();
        featureImportancesBuilder.putAllFeatureImportances(shapValues.get(index));
        if (modelVersion != null) {
          featureImportancesBuilder.setModelVersion(modelVersion);
        }
        if (predictionTimestamps != null) {
          featureImportancesBuilder.setTimestamp(
              Timestamps.fromMillis(predictionTimestamps.get(index)));
        }
        recordBuilder.setFeatureImportances(featureImportancesBuilder);
      }
      builder.addRecords(recordBuilder);
    }
    final HttpPost request =
        buildRequest(RecordUtil.toJSON(builder.build()), this.bulkHost, this.apiKey, this.spaceKey);
    return new Response(client.execute(request, null));
  }

  /** {@inheritDoc} */
  @Override
  public <T> Response logTrainingRecords(
      String modelId,
      String modelVersion,
      List<Map<String, ?>> features,
      List<Map<String, Embedding>> embeddingFeatures,
      List<Map<String, ?>> tags,
      List<T> predictionLabels,
      List<T> actualLabels)
      throws IOException {
    if (modelId == null || modelId.isEmpty()) {
      throw new IllegalArgumentException("modelId cannot be null or empty");
    }
    if (predictionLabels == null || predictionLabels.isEmpty()) {
      throw new IllegalArgumentException("predictionLabels cannot be null or empty");
    }
    if (features != null && features.size() != predictionLabels.size()) {
      throw new IllegalArgumentException(
          "if specified, features list must be the same length as predictionLabels");
    }
    if (embeddingFeatures != null && embeddingFeatures.size() != predictionLabels.size()) {
      throw new IllegalArgumentException(
          "if specified, embeddingFeatures list must be the same length as predictionLabels");
    }
    if (tags != null && tags.size() != predictionLabels.size()) {
      throw new IllegalArgumentException("predictionLabels.size() must equal tags.size()");
    }
    if (actualLabels == null || actualLabels.size() != predictionLabels.size()) {
      throw new IllegalArgumentException(
          "actualLabels cannot be null and must be the same length as predictionLabels");
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < predictionLabels.size(); i++) {
      Public.PreProductionRecord.Builder pprBuilder = Public.PreProductionRecord.newBuilder();
      Public.PreProductionRecord.TrainingRecord.Builder trBuilder =
          Public.PreProductionRecord.TrainingRecord.newBuilder();
      Record.Builder recordBuilder = Record.newBuilder();
      recordBuilder.setModelId(modelId);

      Public.Prediction.Builder predictionBuilder = Public.Prediction.newBuilder();
      predictionBuilder.setLabel(RecordUtil.convertLabel(predictionLabels.get(i)));
      if (modelVersion != null) {
        predictionBuilder.setModelVersion(modelVersion);
      }
      if (features != null) {
        predictionBuilder.putAllFeatures(RecordUtil.convertFeatures(features.get(i)));
      }
      if (embeddingFeatures != null) {
        predictionBuilder.putAllFeatures(
            RecordUtil.convertEmbeddingFeatures(embeddingFeatures.get(i)));
      }
      if (tags != null) {
        predictionBuilder.putAllTags(RecordUtil.convertTags(tags.get(i)));
      }
      recordBuilder.setPrediction(predictionBuilder);

      Public.Actual.Builder actualBuilder = Public.Actual.newBuilder();
      actualBuilder.setLabel(RecordUtil.convertLabel(actualLabels.get(i)));
      recordBuilder.setActual(actualBuilder);

      trBuilder.setRecord(recordBuilder);

      pprBuilder.setTrainingRecord(trBuilder);
      sb.append(RecordUtil.toJSON(pprBuilder.build()));
      sb.append('\n');
    }
    final HttpPost request =
        buildRequest(sb.toString(), this.trainingValidationHost, this.apiKey, this.spaceKey);
    return new Response(client.execute(request, null));
  }

  /** {@inheritDoc} */
  @Override
  public <T> Response logValidationRecords(
      String modelId,
      String modelVersion,
      String batchId,
      List<Map<String, ?>> features,
      List<Map<String, Embedding>> embeddingFeatures,
      List<Map<String, ?>> tags,
      List<T> predictionLabels,
      List<T> actualLabels)
      throws IOException {
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
      throw new IllegalArgumentException(
          "if specified, features list must be the same length as predictionLabels");
    }
    if (embeddingFeatures != null && embeddingFeatures.size() != predictionLabels.size()) {
      throw new IllegalArgumentException(
          "if specified, embeddingFeatures list must be the same length as predictionLabels");
    }
    if (tags != null && tags.size() != predictionLabels.size()) {
      throw new IllegalArgumentException("predictionLabels.size() must equal tags.size()");
    }
    if (actualLabels == null || actualLabels.size() != predictionLabels.size()) {
      throw new IllegalArgumentException(
          "actualLabels cannot be null and must be the same length as predictionLabels");
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < predictionLabels.size(); i++) {
      Public.PreProductionRecord.Builder pprBuilder = Public.PreProductionRecord.newBuilder();
      Public.PreProductionRecord.ValidationRecord.Builder vrBuilder =
          Public.PreProductionRecord.ValidationRecord.newBuilder();
      vrBuilder.setBatchId(batchId);

      Record.Builder recordBuilder = Record.newBuilder();
      recordBuilder.setModelId(modelId);

      Public.Prediction.Builder predictionBuilder = Public.Prediction.newBuilder();
      predictionBuilder.setLabel(RecordUtil.convertLabel(predictionLabels.get(i)));
      if (modelVersion != null) {
        predictionBuilder.setModelVersion(modelVersion);
      }
      if (features != null) {
        predictionBuilder.putAllFeatures(RecordUtil.convertFeatures(features.get(i)));
      }
      if (embeddingFeatures != null) {
        predictionBuilder.putAllFeatures(
            RecordUtil.convertEmbeddingFeatures(embeddingFeatures.get(i)));
      }
      if (tags != null) {
        predictionBuilder.putAllTags(RecordUtil.convertTags(tags.get(i)));
      }
      recordBuilder.setPrediction(predictionBuilder);

      Public.Actual.Builder actualBuilder = Public.Actual.newBuilder();
      actualBuilder.setLabel(RecordUtil.convertLabel(actualLabels.get(i)));
      recordBuilder.setActual(actualBuilder);

      vrBuilder.setRecord(recordBuilder);

      pprBuilder.setValidationRecord(vrBuilder);
      sb.append(RecordUtil.toJSON(pprBuilder.build()));
      sb.append('\n');
    }
    final HttpPost request =
        buildRequest(sb.toString(), this.trainingValidationHost, this.apiKey, this.spaceKey);
    return new Response(client.execute(request, null));
  }

  /**
   * Closes the http client.
   *
   * @throws IOException in case of a network error
   */
  public void close() throws IOException {
    this.client.close();
  }

  public static class ScoredCategorical {
    private final String category;
    private final double score;
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
