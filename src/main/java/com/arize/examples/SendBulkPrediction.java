package com.arize.examples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.arize.ArizeClient;
import com.arize.Response;

public class SendBulkPrediction {

    public static void main(final String[] args)
            throws IOException, URISyntaxException, InterruptedException, ExecutionException {

        final ArizeClient arize = new ArizeClient(System.getenv("ARIZE_API_KEY"), System.getenv("ARIZE_ORG_KEY"));

        final List<Map<String, ?>> features = new ArrayList<Map<String, ?>>();
        features.add(new HashMap<String, Object>() {{ put("days", 5); put("is_organic", 1);}});
        features.add(new HashMap<String, Object>() {{ put("days", 3); put("is_organic", 0);}});
        features.add(new HashMap<String, Object>() {{ put("days", 7); put("is_organic", 0);}});

        final List<Map<String, Double>> shapValues = new ArrayList<>();
        shapValues.add(new HashMap<String, Double>(){{ put("days", 1.0); put("is_organic", -1.5);}});
        shapValues.add(new HashMap<String, Double>(){{ put("days", 1.0); put("is_organic", -1.1);}});
        shapValues.add(new HashMap<String, Double>(){{ put("days", 1.0); put("is_organic", -1.1);}});

        final List<String> labels = new ArrayList<String>(Arrays.asList("pear", "banana", "apple"));
        final List<String> predictionIds = new ArrayList<String>(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));

        final Response asyncResponse = arize.bulkLog("exampleModelId", "v1", predictionIds, features, labels, null, shapValues,null);

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