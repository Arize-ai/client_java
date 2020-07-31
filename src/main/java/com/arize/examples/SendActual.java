package com.arize.examples;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.arize.ArizeClient;
import com.arize.RequestUtil;

import org.apache.http.HttpResponse;

public class SendActual {

  public static void main(String[] args)
      throws IOException, URISyntaxException, InterruptedException, ExecutionException {

    ArizeClient arize = new ArizeClient(System.getenv("ARIZE_API_KEY"), System.getenv("ARIZE_ORG_KEY"));

    Map<String, String> rawFeatures = new HashMap<>();
    rawFeatures.put("key", "value");

    Future<HttpResponse> future = arize.logActual("modelId", "predictionId", "rawLabelActual");

    HttpResponse response = future.get();

    System.out.println("Response Status Code: " + RequestUtil.getResponseCode(response));
    System.out.println("Response Body: " + RequestUtil.getResponseBody(response));

    // Don't forget to shutdown the client with your application shutdown hook.
    arize.close();
    System.out.println("Done");
  }

}