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

public class SendPrediction {

  @SuppressWarnings("unchecked")
  public static void main(String[] args)
      throws IOException, InterruptedException, ExecutionException, URISyntaxException {
    Map<String, String> rawFeatures = new HashMap<>();
    rawFeatures.put("key", "value");

    ArizeClient arize = new ArizeClient(System.getenv("ARIZE_API_KEY"), System.getenv("ARIZE_ORG_KEY"));

    Future<HttpResponse> future = arize.logPrediction("modelId", "predictionId", "modelVersion", "rawLabel", rawFeatures);

    HttpResponse response = future.get();

    System.out.println("Response Status Code: " + RequestUtil.getResponseCode(response));
    System.out.println("Response Body: " + RequestUtil.getResponseBody(response));

    // Don't forget to shutdown the client with your application shutdown hook.
    arize.close();
    System.out.println("Done");
  }

}