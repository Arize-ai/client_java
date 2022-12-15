package com.arize.examples;

import com.arize.ArizeClient;
import com.arize.Response;
import com.arize.protocol.Public.MultiValue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class SendBulkRankingData {

    /**
     * This is an example of ingesting a ranking model data:
     * For prediction list:
     * There are two different queries represented with prediction group id of "XX" and "YY",
     * Each query has three items on the list with their prediction scores and ranks.
     * Prediction group ids and Ranks are required and Scores are optional.
     * For actual list:
     * Each element represents each item on the prediction list above and contains relevance score and engagement actions list.
     * One of relevance score or engagement actions list is required for actual list, but prefer relevance score.
     * For features and tags, they are same as other model data ingestion.
     * For tag list:
     * Need to add the Rank as tag if you wish to use it.
     */

    public static void main(String[] args)
            throws IOException, URISyntaxException, InterruptedException, ExecutionException {
        ArizeClient arize =
                new ArizeClient(System.getenv("ARIZE_API_KEY"), System.getenv("ARIZE_SPACE_KEY"));

        final List<String> predictionIds =
                new ArrayList<>(
                        Arrays.asList(
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString(),
                                UUID.randomUUID().toString()));


        final List<ArizeClient.Ranking> predictionLabels =
                Arrays.asList(
                        new ArizeClient.Ranking.RankingBuilder().setPredictionGroupId("XX").setPredictionScore(9.8).setRank(1).build(),
                        new ArizeClient.Ranking.RankingBuilder().setPredictionGroupId("XX").setPredictionScore(9.5).setRank(2).build(),
                        new ArizeClient.Ranking.RankingBuilder().setPredictionGroupId("XX").setPredictionScore(9.0).setRank(3).build(),
                        new ArizeClient.Ranking.RankingBuilder().setPredictionGroupId("YY").setPredictionScore(9.7).setRank(1).build(),
                        new ArizeClient.Ranking.RankingBuilder().setPredictionGroupId("YY").setPredictionScore(9.2).setRank(2).build(),
                        new ArizeClient.Ranking.RankingBuilder().setPredictionGroupId("YY").setPredictionScore(8.0).setRank(3).build()
                );

        final List<ArizeClient.Ranking> actualLabels =
                Arrays.asList(
                        new ArizeClient.Ranking.RankingBuilder().setRelevanceScore(1).setAttributions(MultiValue.newBuilder().addAllValues(Arrays.asList("click", "purchase")).build()).build(),
                        new ArizeClient.Ranking.RankingBuilder().setRelevanceScore(1).setAttributions(MultiValue.newBuilder().addAllValues(Collections.singletonList("click")).build()).build(),
                        new ArizeClient.Ranking.RankingBuilder().setRelevanceScore(1).setAttributions(MultiValue.newBuilder().addAllValues(Collections.singletonList("no-event")).build()).build(),
                        new ArizeClient.Ranking.RankingBuilder().setRelevanceScore(1).setAttributions(MultiValue.newBuilder().addAllValues(Collections.singletonList("click")).build()).build(),
                        new ArizeClient.Ranking.RankingBuilder().setRelevanceScore(1).setAttributions(MultiValue.newBuilder().addAllValues(Collections.singletonList("no-event")).build()).build(),
                        new ArizeClient.Ranking.RankingBuilder().setRelevanceScore(1).setAttributions(MultiValue.newBuilder().addAllValues(Arrays.asList("click", "purchase")).build()).build()

                );
        final List<Map<String, ?>> tags = new ArrayList<>();
        for (ArizeClient.Ranking predictionLabel : predictionLabels) {
            tags.add(new HashMap<String, Object>() {
                {
                    put("Rank", predictionLabel.getRank());
                }
            });
        }

        final Response asyncResponse =
                arize.bulkLog(
                        "exampleModelId",
                        "v1",
                        predictionIds,
                        null,
                        null,
                        tags,
                        predictionLabels,
                        actualLabels,
                        null,
                        null);

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
