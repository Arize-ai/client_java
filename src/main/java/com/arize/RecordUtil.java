package com.arize;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.arize.protocol.Public.Actual;
import com.arize.protocol.Public.Label;
import com.arize.protocol.Public.Prediction;
import com.arize.protocol.Public.Record;
import com.arize.protocol.Public.Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;

public class RecordUtil {

    protected static String recordToJson(final Record record) throws IOException {
        try {
            return JsonFormat.printer().print(record);
        } catch (InvalidProtocolBufferException e) {
            throw new IOException("Exception serializing record: " + e.getMessage());
        }
    }

    protected static Record.Builder initializeRecord(final String organizationKey, final String modelId,
            final String predictionId) {
        return Record.newBuilder().setOrganizationKey(organizationKey).setModelId(modelId)
                .setPredictionId(predictionId);
    }

    protected static Record decoratePrediction(Record.Builder builder, final String modelVersion, final Label label,
            final Map<String, Value> features) {
        Prediction.Builder pBuilder = Prediction.newBuilder().setTimestamp(getCurrentTime());
        if (modelVersion != null) {
            pBuilder.setModelVersion(modelVersion);
        }
        if (features != null) {
            pBuilder.putAllFeatures(features);
        }
        return builder.setPrediction(pBuilder.setLabel(label)).build();
    }

    protected static Record decorateActual(Record.Builder builder, final Label label) {
        return builder.setActual(Actual.newBuilder().setTimestamp(getCurrentTime()).setLabel(label)).build();
    }

    protected static <T> Map<String, Value> convertFeatures(final Map<String, T> features)
            throws IllegalArgumentException {
        Map<String, Value> converted = new HashMap<>();
        features.forEach((k, v) -> converted.put(k, convertValue(k, v)));
        return converted;
    }

    protected static <T> Label convertLabel(final T rawLabel) throws IllegalArgumentException {
        Label.Builder label = Label.newBuilder();
        if (rawLabel instanceof Boolean) {
            return label.setBinary((Boolean) rawLabel).build();
        } else if (rawLabel instanceof String) {
            return label.setCategorical((String) rawLabel).build();
        } else if (rawLabel instanceof Integer || rawLabel instanceof Long || rawLabel instanceof Short
                || rawLabel instanceof Float || rawLabel instanceof Double) {
            return label.setNumeric((Double) rawLabel).build();
        }
        throw new IllegalArgumentException(
                "Illegal label type: " + rawLabel.getClass().getSimpleName() + " for label: " + rawLabel);
    }

    protected static <T> void validateInputs(final String modelId, final String predictionId, final T label)
            throws IllegalArgumentException {
        if (modelId == null || modelId.isEmpty()) {
            throw new IllegalArgumentException("modelId is null or empty, but must be present.");
        }
        if (predictionId == null || predictionId.isEmpty()) {
            throw new IllegalArgumentException("predictionId is null or empty, but must be present.");
        }
        if (label == null) {
            throw new IllegalArgumentException(
                    "label is null, but must be present. Supported types are: boolean, string, int, long, short, float, double.");
        }
    }

    private static <T> Value convertValue(final String name, final T rawValue) throws IllegalArgumentException {
        Value.Builder val = Value.newBuilder();
        if (rawValue instanceof String) {
            return val.setString((String) rawValue).build();
        } else if (rawValue instanceof Integer || rawValue instanceof Long || rawValue instanceof Short) {
            return val.setInt(((Number) rawValue).longValue()).build();
        } else if (rawValue instanceof Double || rawValue instanceof Float) {
            return val.setDouble((Double) rawValue).build();
        } else if (rawValue == null) {
            throw new IllegalArgumentException("Feature " + name + " has a null value");
        }
        throw new IllegalArgumentException(
                "Illegal feature type: " + rawValue.getClass().getSimpleName() + " for feature: " + name);
    }

    private static Timestamp getCurrentTime() {
        long millis = System.currentTimeMillis();
        return Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build();
    }
}