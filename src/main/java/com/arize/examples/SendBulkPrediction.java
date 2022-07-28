package com.arize.examples;

import com.arize.ArizeClient;
import com.arize.Response;
import com.arize.types.Embedding;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class SendBulkPrediction {

  public static void main(final String[] args)
      throws IOException, URISyntaxException, InterruptedException, ExecutionException {

    final ArizeClient arize =
        new ArizeClient(System.getenv("ARIZE_API_KEY"), System.getenv("ARIZE_SPACE_KEY"));

    final List<Map<String, ?>> features = new ArrayList<>();
    features.add(
        new HashMap<String, Object>() {
          {
            put("days", 5);
            put("is_organic", 1);
          }
        });
    features.add(
        new HashMap<String, Object>() {
          {
            put("days", 3);
            put("is_organic", 0);
          }
        });
    features.add(
        new HashMap<String, Object>() {
          {
            put("days", 7);
            put("is_organic", 0);
          }
        });

    final List<Map<String, ?>> tags = new ArrayList<>();
    tags.add(
        new HashMap<String, Object>() {
          {
            put("region", 5);
            put("age", 1);
          }
        });
    tags.add(
        new HashMap<String, Object>() {
          {
            put("region", 3);
            put("age", 0);
          }
        });
    tags.add(
        new HashMap<String, Object>() {
          {
            put("region", 7);
            put("age", 0);
          }
        });

    final List<Map<String, Double>> shapValues = new ArrayList<>();
    shapValues.add(
        new HashMap<String, Double>() {
          {
            put("days", 1.0);
            put("is_organic", -1.5);
          }
        });
    shapValues.add(
        new HashMap<String, Double>() {
          {
            put("days", 1.0);
            put("is_organic", -1.1);
          }
        });
    shapValues.add(
        new HashMap<String, Double>() {
          {
            put("days", 1.0);
            put("is_organic", -1.1);
          }
        });

    final List<Map<String, Embedding>> embeddingFeatures = new ArrayList<Map<String, Embedding>>();
    embeddingFeatures.add(
        new HashMap<String, Embedding>() {
          {
            put(
                "embedding_feature_1",
                new Embedding(
                    Arrays.asList(1.0, 0.5),
                    Arrays.asList("test", "token", "array"),
                    "https://example.com/image.jpg"));
            put(
                "embedding_feature_2",
                new Embedding(
                    Arrays.asList(1.0, 0.8),
                    Arrays.asList("this", "is"),
                    "https://example.com/image_3.jpg"));
          }
        });
    embeddingFeatures.add(
        new HashMap<String, Embedding>() {
          {
            put(
                "embedding_feature_1",
                new Embedding(
                    Arrays.asList(0.0, 0.6),
                    Arrays.asList("another", "example"),
                    "https://example.com/image_2.jpg"));
            put(
                "embedding_feature_2",
                new Embedding(
                    Arrays.asList(0.1, 1.0),
                    Arrays.asList("an", "example"),
                    "https://example.com/image_4.jpg"));
          }
        });
    embeddingFeatures.add(
        new HashMap<String, Embedding>() {
          {
            put(
                "embedding_feature_1",
                new Embedding(
                    Arrays.asList(1.0, 0.8),
                    Arrays.asList("third"),
                    "https://example.com/image_3.jpg"));
            put(
                "embedding_feature_2",
                new Embedding(
                    Arrays.asList(1.0, 0.4),
                    Arrays.asList("token", "array"),
                    "https://example.com/image_5.jpg"));
          }
        });

    final List<String> labels = new ArrayList<>(Arrays.asList("pear", "banana", "apple"));
    final List<String> predictionIds =
        new ArrayList<>(
            Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()));

    final Response asyncResponse =
        arize.bulkLog(
            "exampleModelId",
            "v1",
            predictionIds,
            features,
            embeddingFeatures,
            tags,
            labels,
            null,
            shapValues,
            null);

    // This is a blocking call similar to future.get()
    asyncResponse.resolve();

    // Check that the API call was successful
    switch (asyncResponse.getResponseCode()) {
      case OK:
        // TODO: Success!
        System.out.println("Success!!!");
        break;
      case AUTHENTICATION_ERROR:
        // TODO: Check to make sure your Arize API key and Space key are correct
        break;
      case BAD_REQUEST:
        // TODO: Malformed request
        System.out.println("Bad Request: " + asyncResponse.getResponseBody());
      case NOT_FOUND:
        // TODO: API endpoint not found, client is likely mal-configured, make sure you
        // are not overwriting Arize's endpoint URI
        break;
      case UNEXPECTED_FAILURE:
        // TODO: Unexpected failure, check for a reason on response body
        System.out.println("Failure Reason: " + asyncResponse.getResponseBody());
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + asyncResponse.getResponseCode());
    }

    System.out.println("Response Code: " + asyncResponse.getResponseCode());
    System.out.println("Response Body: " + asyncResponse.getResponseBody());

    // Don't forget to shut down the client with your application shutdown hook.
    arize.close();
    System.out.println("Done");
  }
}
