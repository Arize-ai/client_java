package com.arize.examples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.arize.ArizeClient;
import com.arize.Response;
import com.arize.ArizeClient.ScoredCategorical;

public class SendBulkNumericSequence {

    public static void main(final String[] args)
            throws IOException, URISyntaxException, InterruptedException, ExecutionException {

        final ArizeClient arize = new ArizeClient(System.getenv("ARIZE_API_KEY"), System.getenv("ARIZE_ORG_KEY"));

        final List<String> predictionIds = new ArrayList<String>(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        
        final List<ScoredCategorical> predictionLabels = Arrays.asList(
                new ScoredCategorical("relevant", 3.14),
                new ScoredCategorical("relevant", 6.28), 
                new ScoredCategorical("relevant", 9.42));
                
        final List<ScoredCategorical> actualLabels = Arrays.asList(
                new ScoredCategorical("relevant", 4.13, Arrays.asList(0.12, 0.23, 0.34)),
                new ScoredCategorical("relevant", 8.26),
                new ScoredCategorical("relevant", 2.49, Arrays.asList(0.45, 0.56)));

        final Response asyncResponse = arize.bulkLog("exampleModelId", "v1", predictionIds, null, predictionLabels, actualLabels, null, null);

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
