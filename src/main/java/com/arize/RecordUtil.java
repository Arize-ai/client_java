package com.arize;

import static com.google.protobuf.util.Timestamps.fromMillis;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arize.protocol.Public.Actual;
import com.arize.protocol.Public.BulkRecord;
import com.arize.protocol.Public.Label;
import com.arize.protocol.Public.MultiValue;
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

    protected static String recordToJson(final BulkRecord record) throws IOException {
        try {
            return JsonFormat.printer().print(record);
        } catch (InvalidProtocolBufferException e) {
            throw new IOException("Exception serializing record: " + e.getMessage());
        }
    }

    protected static Record.Builder initializeRecord(final String organizationKey, final String modelId,
            final String predictionId) throws IllegalArgumentException {
        if (predictionId == null || predictionId.length() == 0) {
            throw new IllegalArgumentException("Invalid predictionId, must be a String");
        }
        Record.Builder builder = Record.newBuilder().setPredictionId(predictionId);
        if (organizationKey != null) {
            builder.setOrganizationKey(organizationKey);
        }
        if (modelId != null) {
            builder.setModelId(modelId);
        }
        return builder;
    }

    protected static BulkRecord.Builder initializeBulkRecord(final String organizationKey, final String modelId,
            final String modelVersion) {
        BulkRecord.Builder builder = BulkRecord.newBuilder();
        if (organizationKey != null) {
            builder.setOrganizationKey(organizationKey);
        }
        if (modelId != null) {
            builder.setModelId(modelId);
        }
        if (modelVersion != null) {
            builder.setModelVersion(modelVersion);
        }
        return builder;
    }

    protected static Map<String, Value> getFeatureMap(final List<Map<String, ?>> features) {
        final Map<String, Value> feature = new HashMap<>();
        if (features != null) {
            for (final Map<String, ?> featureMap : features) {
                if (featureMap != null) {
                    feature.putAll(RecordUtil.convertFeatures(featureMap));
                }
            }
            return feature;
        }
        return null;
    }

    protected static Record decoratePrediction(Record.Builder builder, final String modelVersion, final Label label,
            final Map<String, Value> features, final Timestamp timestampMillis) {
        Prediction.Builder prediction = Prediction.newBuilder();
        prediction.setLabel(label);
        if (modelVersion != null) {
            prediction.setModelVersion(modelVersion);
        }
        if (features != null) {
            prediction.putAllFeatures(features);
        }
        if (timestampMillis != null) {
            prediction.setTimestamp(timestampMillis);
        }
        return builder.setPrediction(prediction).build();
    }

    protected static Record decorateActual(Record.Builder builder, final Label label, final Timestamp timestampMillis) {
        Actual.Builder actual = Actual.newBuilder();
        actual.setLabel(label);
        if (timestampMillis != null) {
            actual.setTimestamp(timestampMillis);
        }
        return builder.setActual(actual).build();
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
            return label.setNumeric(Double.parseDouble(String.valueOf(rawLabel))).build();
        }
        throw new IllegalArgumentException(
                "Illegal label " + rawLabel + ", must be oneof: boolean, String, int, long, short, float, double");
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

    protected static <T> void validateBulkInputs(final String modelId, final List<String> predictionIds,
            final List<T> labels) throws IllegalArgumentException {
        if (modelId == null || modelId.isEmpty()) {
            throw new IllegalArgumentException("modelId is null or empty, but must be present.");
        }
        if (predictionIds == null || predictionIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "predictionIds is null or empty, but must consist of a list of Strings.");
        }
        if (labels == null || labels.isEmpty()) {
            throw new IllegalArgumentException(
                    "labels is null or empty, but a List of supported label types must be present. "
                            + "Supported types are: boolean, string, int, long, short, float, double.");
        }
        if (labels.size() != predictionIds.size()) {
            throw new IllegalArgumentException("PredictionIds, size: " + String.valueOf(predictionIds.size())
                    + ", but must be same size as labels, size: " + String.valueOf(labels.size()));
        }
    }

    @SuppressWarnings({ "unchecked" })
    private static <T> Value convertValue(final String name, final T rawValue) throws IllegalArgumentException {
        Value.Builder val = Value.newBuilder();
        if (rawValue instanceof String) {
            return val.setString((String) rawValue).build();
        } else if (rawValue instanceof Integer || rawValue instanceof Long || rawValue instanceof Short) {
            return val.setInt(((Number) rawValue).longValue()).build();
        } else if (rawValue instanceof Double) {
            return val.setDouble((Double) rawValue).build();
        } else if (rawValue instanceof Float) {
            return val.setDouble(((Float)rawValue).doubleValue()).build();
        } else if (rawValue instanceof Collection) {
            MultiValue.Builder values = MultiValue.newBuilder();
            for (T value : (List<T>) rawValue) {
                if (value instanceof String) {
                    values.addValues((String) value);
                } else {
                    throw new IllegalArgumentException("Elements of multivalue feature " + name + " must be Strings");
                }
            }
            return val.setMultiValue(values).build();
        } else if (rawValue == null) {
            throw new IllegalArgumentException("Feature " + name + " has a null value");
        }
        throw new IllegalArgumentException(
                "Illegal feature type: " + rawValue.getClass().getSimpleName() + " for feature: " + name);
    }

    protected static Timestamp getTimestamp(final long timestampMillis) {
        return fromMillis(timestampMillis);
    }
}