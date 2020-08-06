package com.arize.examples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.arize.ArizeClient;
import com.arize.Response;

public class SendPrediction {

    @SuppressWarnings("unchecked")
    public static void main(String[] args)
            throws IOException, InterruptedException, ExecutionException, URISyntaxException {
        Map<String, String> rawFeatures = new HashMap<>();
        rawFeatures.put("key", "value");

        ArizeClient arize = new ArizeClient(System.getenv("ARIZE_API_KEY"), System.getenv("ARIZE_ORG_KEY"));

        Response asyncResponse = arize.logPrediction("modelId", "predictionId", "modelVersion", "rawLabel",
                rawFeatures);

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

        // Don't forget to shutdown the client with your application shutdown hook.
        arize.close();
        System.out.println("Done");
    }

}