<div align="center">
  <img src="https://storage.googleapis.com/arize-assets/arize-logo-white.jpg" width="600" /><br><br>
</div>

[![Maven Central](https://img.shields.io/maven-central/v/com.arize/arize-api-client.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.arize%22%20AND%20a:%22arize-api-client%22)
[![Slack](https://img.shields.io/badge/slack-@arize-yellow.svg?logo=slack)](https://join.slack.com/t/arize-ai/shared_invite/zt-g9c1j1xs-aQEwOAkU4T2x5K8cqI1Xqg)

================
### Overview

A helper library to interact with Arize AI APIs. Visit us at https://www.arize.com

---
## Quickstart
This guide will help you instrument your code to log model observability data. The types of data supported include prediction labels, human readable/debuggable model features and tags, actual labels (once the ground truth is learned), and other model related data. Logging model data allows you to generate powerful visualizations in the Arize platform to better understand and debug your model's behavior. Additionally, Arize can provide monitoring for the data quality, data drift, and performance of your production models.

Start logging your model data with the following steps:

### 1. Create your account
Sign up for a free account by reaching out to <contacts@arize.com>.

<div align="center">
  <img src="https://storage.googleapis.com/arize-assets/Arize%20UI%20platform.jpg" /><br><br>
</div>

### 2. Get your service API key
When you create an account, we generate a service API key. You will need this API Key and your Organization ID for logging authentication.


### 3. Instrument your code
### Java Client
If you are using the Arize Java client, add a few lines to your code to log predictions and actuals. Logs are sent to Arize asynchronously.

### Importing Library

#### Maven Project
```
<dependency>
  <groupId>com.arize</groupId>
  <artifactId>arize-api-client</artifactId>
  <version>0.0.9</version>
</dependency>
```

#### Bazel Project
```
maven_jar(
    name = "arize-api-client",
    artifact = "com.arize:arize-api-client:0.0.9",
    sha1 = "f96374138c5333d5886a31a55d0e81e5ab75087f",
)
```

### Initialize Java Client

Initialize `arize` at the start of your service using your previously created API Key and Organization ID.

> **_NOTE:_** We strongly suggest storing the API key as a secret or an environment variable.

```java
import com.arize.ArizeClient;

ArizeClient arize = new ArizeClient("ARIZE_API_KEY", "ORG_KEY");
```

### Collect your model input features and labels you'd like to track

#### Real-time single prediction:
For a single real-time prediction, you can track all input features used at prediction time by logging them via a key:value map.

```java
import com.arize.ArizeClient;
import com.arize.Response;

Map<String, String> rawFeatures = new HashMap<>();
        rawFeatures.put("key", "value");

        ArizeClient arize = new ArizeClient(System.getenv("ARIZE_API_KEY"), System.getenv("ARIZE_ORG_KEY"));

        Response asyncResponse = arize.log("exampleModelId", "v1", UUID.randomUUID().toString(), rawFeatures, "pear", null, null, 0);

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


```

#### Bulk predictions:
When dealing with bulk predictions, you can pass in input features, prediction/actual labels, and prediction_ids for more than one prediction.
```java
import com.arize.ArizeClient;
import com.arize.Response;

// You only need to instantiate the client once
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
```

### 3. Log In for Analytics
That's it! Once your service is deployed and predictions are logged you'll be able to log into your Arize account and dive into your data, slicing it by features, tags, models, time, etc.

#### Analytics Dashboard
<div align="center">
  <img src="https://storage.googleapis.com/arize-assets/Arize%20UI%20platform.jpg" /><br><br>
</div>

---
### Logging SHAP values
Log SHAP feature importances to the Arize platform to explain your model's predictions. By logging SHAP values you gain the ability to view the global feature importances of your predictions as well as the ability to perform cohort and prediction based analysis to compare feature importance values under varying conditions. For more information on SHAP and how to use SHAP with Arize, check out our [SHAP documentation](https://app.gitbook.com/@arize/s/arize-onboarding/platform-features/explainability/shap).

---
### Other languages
If you are using a different language, you'll be able to post an HTTP request to our Arize edge-servers to log your events.

### HTTP post request to Arize

```bash
curl -X POST -H "Authorization: YOU_API_KEY" "https://log.arize.com/v1/log" -d'{"organization_key": "YOUR_ORG_KEY", "model_id": "test_model_1", "prediction_id":"test100", "prediction":{"model_version": "v1.23.64", "features":{"state":{"string": "CO"}, "item_count":{"int": 10}, "charge_amt":{"float": 12.34}, "physical_card":{"string": true}}, "prediction_label": {"binary": false}}}'
```
---
