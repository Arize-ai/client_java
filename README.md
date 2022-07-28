<div align="center">
  <img src="https://storage.googleapis.com/arize-assets/arize-logo-white.jpg" width="600" /><br><br>
</div>

[![Maven Central](https://img.shields.io/maven-central/v/com.arize/arize-api-client.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.arize%22%20AND%20a:%22arize-api-client%22)
[![javadoc](https://javadoc.io/badge2/com.arize/arize-api-client/javadoc.svg)](https://javadoc.io/doc/com.arize/arize-api-client)
[![openjdk](https://img.shields.io/badge/opendjk-%3E=1.8-green)](https://openjdk.java.net)
[![Slack](https://img.shields.io/badge/slack-@arize-yellow.svg?logo=slack)](https://join.slack.com/t/arize-ai/shared_invite/zt-g9c1j1xs-aQEwOAkU4T2x5K8cqI1Xqg)
[![license](https://img.shields.io/github/license/arize-ai/client_java)](https://github.com/Arize-ai/client_java/blob/main/LICENSE)
----
## Overview
A helper library to interact with Arize AI APIs.

Arize is an end-to-end ML observability and model monitoring platform. The platform is designed to help ML engineers and data science practitioners surface and fix issues with ML models in production faster with:
- Automated ML monitoring and model monitoring
- Workflows to troubleshoot model performance
- Real-time visualizations for model performance monitoring, data quality monitoring, and drift monitoring
- Model prediction cohort analysis
- Pre-deployment model validation
- Integrated model explainability

---
## Quickstart
This guide will help you instrument your code to log observability data for model monitoring and ML observability. The types of data supported include prediction labels, human readable/debuggable model features and tags, actual labels (once the ground truth is learned), and other model-related data. Logging model data allows you to generate powerful visualizations in the Arize platform to better monitor model performance, understand issues that arise, and debug your model's behavior. Additionally, Arize provides data quality monitoring, data drift detection, and performance management of your production models.

Start logging your model data with the following steps:

### 1. Sign up for your account
Sign up for a free account at https://arize.com/join.

<div align="center">
  <img src="https://storage.googleapis.com/arize-assets/Arize%20UI%20platform.jpg" /><br><br>
</div>

### 2. Get your service API key
When you create an account, we generate a service API key. You will need this API Key and your Space Key for logging authentication.

<div align="center">
  <img src="https://storage.googleapis.com/arize-assets/fixtures/copy-keys.png" /><br><br>
</div>

### 3. Instrument your code
If you are using the Arize Java client, add a few lines to your code to log predictions and actuals. Logs are sent to Arize asynchronously.

### Importing Library

#### Maven Project
```
<dependency>
  <groupId>com.arize</groupId>
  <artifactId>arize-api-client</artifactId>
  <version>2.0.1</version>
</dependency>
```

#### Bazel Project
```
maven_jar(
    name = "arize-api-client",
    artifact = "com.arize:arize-api-client:2.0.1",
    sha1 = "2df6860c04899d9c1f508043388b5351ae2ee61c",
)
```

### Initialize Java Client

Initialize `arize` at the start of your service using your previously created API Key and Space Key.

> **_NOTE:_** We strongly suggest storing the API key as a secret.

```java
import com.arize.ArizeClient;
import com.arize.Response;
import com.arize.types.Embedding;

ArizeClient arize = new ArizeClient("ARIZE_API_KEY", "ARIZE_SPACE_KEY");
```

### Collect your model input features and labels you'd like to track

#### Real-time single prediction:
For a single real-time prediction, you can track all input features used at prediction time by logging them via a key:value map.

```java
Map<String, String> features = new HashMap<>();
features.put("feature name", "feature value");

Map<String, String> eventMetadata = new HashMap<>();
eventMetadata.put("business metric", "business metric value");

Map<String, Float> shapValues = new HashMap<>();
shapValues.put("feature name", 0.856f);

Map<String, Embedding> embeddingFeatures = new HashMap<>();
embeddingFeatures.put("embedding feature name", new Embedding(Arrays.asList(1.0, 0.5), Arrays.asList("test", "token", "array"), "https://example.com/image.jpg"));

Response asyncResponse = arize.log("exampleModelId", "v1", UUID.randomUUID().toString(), features, embeddingFeatures, eventMetadata, "pear", "apple", shapValues, System.currentTimeMillis());

// This is a blocking call similar to future.get()
asyncResponse.resolve();

// Check that the API call was successful
switch (asyncResponse.getResponseCode()) {
    case OK:
        // TODO: Success!
        System.out.println("Success!!!");
        break;
    case AUTHENTICATION_ERROR:
        // TODO: Check to make sure your Arize API KEY and Space key are correct
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

// Don't forget to shutdown the client with your application shutdown hook.
arize.close();


```

#### Bulk predictions:
When dealing with bulk predictions, you can pass in input features, prediction/actual labels, and prediction_ids for more than one prediction.
```java
import com.arize.ArizeClient;
import com.arize.Response;
import com.arize.types.Embedding;

// You only need to instantiate the client once
ArizeClient arize = new ArizeClient("ARIZE_API_KEY", "ARIZE_SPACE_KEY");

final List<Map<String, ?>> features = new ArrayList<Map<String, ?>>();
features.add(new HashMap<String, Object>() {{ put("days", 5); put("is_organic", 1);}});
features.add(new HashMap<String, Object>() {{ put("days", 3); put("is_organic", 0);}});
features.add(new HashMap<String, Object>() {{ put("days", 7); put("is_organic", 0);}});

final List<Map<String, ?>> tags = new ArrayList<Map<String, ?>>();
tags.add(new HashMap<String, Object>() {{ put("metadata", 5); put("my business metric", 1);}});
tags.add(new HashMap<String, Object>() {{ put("metadata", 3); put("my business metric", 0);}});
tags.add(new HashMap<String, Object>() {{ put("metadata", 7); put("my business metric", 8);}});

final List<Map<String, Double>> shapValues = new ArrayList<>();
shapValues.add(new HashMap<String, Double>(){{ put("days", 1.0); put("is_organic", -1.5);}});
shapValues.add(new HashMap<String, Double>(){{ put("days", 1.0); put("is_organic", -1.1);}});
shapValues.add(new HashMap<String, Double>(){{ put("days", 1.0); put("is_organic", -1.1);}});

final List<Map<String, Embedding>> embeddingFeatures = new ArrayList<Map<String, Embedding>>();
embeddingFeatures.add(new HashMap<String, Embedding>() {{ put("embedding_feature_1", new Embedding(Arrays.asList(1.0, 0.5), Arrays.asList("test", "token", "array"), "https://example.com/image.jpg")); put("embedding_feature_2", new Embedding(Arrays.asList(1.0, 0.8), Arrays.asList("this", "is"), "https://example.com/image_3.jpg"));}});
embeddingFeatures.add(new HashMap<String, Embedding>() {{ put("embedding_feature_1", new Embedding(Arrays.asList(0.0, 0.6), Arrays.asList("another", "example"), "https://example.com/image_2.jpg")); put("embedding_feature_2", new Embedding(Arrays.asList(0.1, 1.0), Arrays.asList("an", "example"), "https://example.com/image_4.jpg"));}});
embeddingFeatures.add(new HashMap<String, Embedding>() {{ put("embedding_feature_1", new Embedding(Arrays.asList(1.0, 0.8), Arrays.asList("third"), "https://example.com/image_3.jpg")); put("embedding_feature_2", new Embedding(Arrays.asList(1.0, 0.4), Arrays.asList("token", "array"), "https://example.com/image_5.jpg"));}});

final List<String> predictions = new ArrayList<String>(Arrays.asList("pear", "banana", "apple"));
final List<String> actuals = new ArrayList<String>(Arrays.asList("pear", "pear", "apple"));
final List<String> predictionIds = new ArrayList<String>(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));

final Response asyncResponse = arize.bulkLog("exampleModelId", "v1", predictionIds, features, embeddingFeatures, tags, predictions, actuals, shapValues, null);

// This is a blocking call similar to future.get()
asyncResponse.resolve();

// Check that the API call was successful
switch (asyncResponse.getResponseCode()) {
    case OK:
        // TODO: Success!
        System.out.println("Success!!!");
        break;
    case AUTHENTICATION_ERROR:
        // TODO: Check to make sure your Arize API KEY and Space key are correct
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

// Don't forget to shutdown the client with your application shutdown hook.
arize.close();
System.out.println("Done");
```

### 3. Log In for Analytics
That's it! Once your service is deployed and predictions are logged you'll be able to log into your Arize account and dive into your data, slicing it by features, tags, models, time, etc.

#### Analytics Dashboard
<div align="center">
  <img src="https://storage.googleapis.com/arize-assets/Arize%20UI%20platform.jpg" /><br><br>
</div>

---
### Arize Documentation
For further SDK documentation and product user guides, check out our [SDK documentation](https://docs.arize.com/arize/data-ingestion/api-reference/java-sdk).

---
### Website
Visit Us At: https://arize.com/model-monitoring/

### Additional Resources
- [What is ML observability?](https://arize.com/what-is-ml-observability/)
- [Playbook to model monitoring in production](https://arize.com/the-playbook-to-monitor-your-models-performance-in-production/)
- [Using statistical distance metrics for ML monitoring and observability](https://arize.com/using-statistical-distance-metrics-for-machine-learning-observability/)
- [ML infrastructure tools for data preparation](https://arize.com/ml-infrastructure-tools-for-data-preparation/)
- [ML infrastructure tools for model building](https://arize.com/ml-infrastructure-tools-for-model-building/)
- [ML infrastructure tools for production](https://arize.com/ml-infrastructure-tools-for-production-part-1/)
- [ML infrastructure tools for model deployment and model serving](https://arize.com/ml-infrastructure-tools-for-production-part-2-model-deployment-and-serving/)
- [ML infrastructure tools for ML monitoring and observability](https://arize.com/ml-infrastructure-tools-ml-observability/)

Visit the [Arize Blog](https://arize.com/blog) and [Resource Center](https://arize.com/resource-hub/) for more resources on ML observability and model monitoring.

