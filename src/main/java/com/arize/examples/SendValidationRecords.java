package com.arize.examples;

import com.arize.ArizeClient;
import com.arize.Response;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class SendValidationRecords {

    public static void main(final String[] args)
            throws IOException, URISyntaxException, InterruptedException, ExecutionException {

        final ArizeClient arize = new ArizeClient(System.getenv("ARIZE_API_KEY"), System.getenv("ARIZE_SPACE_KEY"));

        final List<Map<String, ?>> features = new ArrayList<Map<String, ?>>();
        features.add(new HashMap<String, Object>() {{
            put("days", 5);
            put("is_organic", 1);
        }});
        features.add(new HashMap<String, Object>() {{
            put("days", 3);
            put("is_organic", 0);
        }});
        features.add(new HashMap<String, Object>() {{
            put("days", 7);
            put("is_organic", 0);
        }});

        final List<String> predictionLabels = new ArrayList<String>(Arrays.asList("pear", "banana", "apple"));
        final List<String> actualLabels = new ArrayList<String>(Arrays.asList("pear", "strawberry", "apple"));

        final Response asyncResponse = arize.logValidationRecords("exampleModelId", "v1", "offline", features, null, predictionLabels, actualLabels);

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
                System.out.println("Failure Reason: " + asyncResponse.getResponseBody());
            case NOT_FOUND:
                // TODO: API endpoint not found, client is likely mal-configured, make sure you
                // are not overwriting Arize's endpoint URI
                break;
            case UNEXPECTED_FAILURE:
                // TODO: Unexpected failure, check for a reason on response body
                System.out.println("Failure Reason: " + asyncResponse.getResponseBody());
                break;
        }

        System.out.println("Response Code: " + asyncResponse.getResponseCode());
        System.out.println("Response Body: " + asyncResponse.getResponseBody());

        // Don't forget to shut down the client with your application shutdown hook.
        arize.close();
        System.out.println("Done");
    }

}
