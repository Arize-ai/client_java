package com.arize.examples;

import com.arize.ArizeClient;
import com.arize.Response;
import com.arize.surrogate.ModelType;
import com.arize.surrogate.SurrogateExplainer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Example of generating surrogate-model feature importances and logging them
 * alongside predictions, mirroring the Arize Python SDK's
 * {@code surrogate_explainability=True} option.
 *
 * <p>Instead of supplying pre-computed SHAP values, this example derives them
 * from the features and prediction scores using {@link SurrogateExplainer}, then
 * passes the resulting maps as the {@code shapValues} argument to
 * {@link ArizeClient#bulkLog}.
 */
public class SendBulkPredictionWithSurrogate {

  public static void main(final String[] args)
      throws IOException, URISyntaxException, InterruptedException, ExecutionException {

    final ArizeClient arize =
        new ArizeClient.ClientBuilder()
            .apiKey(System.getenv("ARIZE_API_KEY"))
            .spaceId(System.getenv("ARIZE_SPACE_ID"))
            .build();

    // The surrogate model needs enough records to fit meaningful splits; with
    // only a handful of rows it will train a trivial surrogate and return
    // all-zero importances. Generate a realistic batch here.
    final String[] regions = {"west", "east", "south", "north"};
    final List<Map<String, ?>> features = new ArrayList<>();
    final List<Double> predictionScores = new ArrayList<>();
    final List<String> labels = new ArrayList<>();
    for (int i = 0; i < 200; i++) {
      int days = i % 10;
      int isOrganic = i % 2;
      Map<String, Object> row = new HashMap<>();
      row.put("days", days);
      row.put("is_organic", isOrganic);
      row.put("region", regions[i % regions.length]);
      features.add(row);

      // Score is driven mainly by "days"; for categorical model types it is the
      // positive-class probability and must lie in [0, 1].
      boolean positive = days >= 5;
      predictionScores.add(positive ? 0.85 : 0.15);
      labels.add(positive ? "fraud" : "not_fraud");
    }

    // Generate SHAP-style feature importances from a surrogate model.
    final List<Map<String, Double>> shapValues =
        SurrogateExplainer.computeFeatureImportances(
            features, predictionScores, ModelType.BINARY_CLASSIFICATION);

    final List<String> predictionIds = new ArrayList<>();
    for (int i = 0; i < features.size(); i++) {
      predictionIds.add(UUID.randomUUID().toString());
    }

    final Response asyncResponse =
        arize.bulkLog(
            "exampleModelId",
            "v1",
            predictionIds,
            features,
            null,
            null,
            labels,
            null,
            shapValues,
            null);

    asyncResponse.resolve();

    switch (asyncResponse.getResponseCode()) {
      case OK:
        System.out.println("Success!!!");
        break;
      case AUTHENTICATION_ERROR:
        System.out.println("Authentication error: check your API key and Space ID");
        break;
      case BAD_REQUEST:
        System.out.println("Bad Request: " + asyncResponse.getResponseBody());
        break;
      case NOT_FOUND:
        System.out.println("Not found: check your endpoint URI");
        break;
      case UNEXPECTED_FAILURE:
        System.out.println("Failure Reason: " + asyncResponse.getResponseBody());
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + asyncResponse.getResponseCode());
    }

    System.out.println("Response Code: " + asyncResponse.getResponseCode());
    System.out.println("Response Body: " + asyncResponse.getResponseBody());

    arize.close();
    System.out.println("Done");
  }
}
