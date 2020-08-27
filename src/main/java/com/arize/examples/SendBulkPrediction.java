package com.arize.examples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.arize.ArizeClient;
import com.arize.Response;

public class SendBulkPrediction {

    public static void main(final String[] args)
            throws IOException, URISyntaxException, InterruptedException, ExecutionException {

        final ArizeClient arize = new ArizeClient(System.getenv("ARIZE_API_KEY"), System.getenv("ARIZE_ORG_KEY"));

        final Map<String, String> rawFeatures = new HashMap<>();
        rawFeatures.put("key", "value");
        final List<Map<String, ?>> features = new ArrayList<Map<String, ?>>();
        features.add(rawFeatures);
        
        final List<String> labels = new ArrayList<String>(Arrays.asList("Categorical Label"));
        final List<String> predictionIds = new ArrayList<String>(Arrays.asList("id_12345"));

        final Response asyncResponse = arize.logBulkPrediction("modelId", "modelVersion", predictionIds, labels, null, features);

        // This is a blocking call similar to future.get()
        asyncResponse.resolve();

        // Check that the API call was successful
        switch (asyncResponse.getResponseCode()) {
            case OK:
                // TODO: Success!
                System.out.println("Success!!!");
                break;
            case AUTHENTICATION_ERROR:
                // TODO: Check to make sure your Arize API KEY and Organization key are correct
                break;
            case BAD_REQUEST:
                // TODO: Malformed request
                System.out.println("Failure Reason: " + asyncResponse.getResponseBody());
            case NOT_FOUND:
                // TODO: API endpoint not found, client is likely malconfigured, make sure you
                // are not overwriting Arize's endpoint URI
                break;
            case UNEXPECTED_FAILURE:
                // TODO: Unexpected failure, check for a reason on response body
                System.out.println("Failure Reason: " + asyncResponse.getResponseBody());
                break;
        }

        System.out.println("Response Code: " + asyncResponse.getResponseCode());
        System.out.println("Response Body: " + asyncResponse.getResponseBody());

        // Don't forget to shutdown the client with your application shutdown hook.
        arize.close();
        System.out.println("Done");
    }

}