package com.arize;

import com.arize.protocol.Public;
import com.arize.protocol.Public.Label;
import com.arize.protocol.Public.MultiValue;
import com.arize.protocol.Public.ScoreCategorical;
import com.arize.protocol.Public.Value;
import com.arize.types.Embedding;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordUtil {

  protected static String toJSON(final MessageOrBuilder record) throws IOException {
    try {
      return JsonFormat.printer().omittingInsignificantWhitespace().print(record);
    } catch (InvalidProtocolBufferException e) {
      throw new IOException("Exception serializing record: " + e.getMessage());
    }
  }

  protected static <T> Map<String, Value> convertFeatures(final Map<String, T> features)
      throws IllegalArgumentException {
    return convertDimensions(features);
  }

  protected static <T> Map<String, Value> convertEmbeddingFeatures(
      final Map<String, T> embeddingFeatures) throws IllegalArgumentException {
    return convertDimensions(embeddingFeatures);
  }

  protected static <T> Map<String, Value> convertTags(final Map<String, T> tags)
      throws IllegalArgumentException {
    return convertDimensions(tags);
  }

  private static <T> Map<String, Value> convertDimensions(final Map<String, T> dimensions)
      throws IllegalArgumentException {
    Map<String, Value> converted = new HashMap<>();
    dimensions.forEach(
        (k, v) -> {
          if (v != null) {
            converted.put(k, convertValue(k, v));
          }
        });
    return converted;
  }

  protected static <T> Label convertLabel(final T rawLabel) throws IllegalArgumentException {
    Label.Builder label = Label.newBuilder();
    if (rawLabel instanceof Boolean) {
      return label.setBinary((Boolean) rawLabel).build();
    } else if (rawLabel instanceof String) {
      return label.setCategorical((String) rawLabel).build();
    } else if (rawLabel instanceof Integer
        || rawLabel instanceof Long
        || rawLabel instanceof Short
        || rawLabel instanceof Float
        || rawLabel instanceof Double) {
      return label.setNumeric(Double.parseDouble(String.valueOf(rawLabel))).build();
    } else if (rawLabel instanceof ArizeClient.ScoredCategorical) {
      ArizeClient.ScoredCategorical sc = (ArizeClient.ScoredCategorical) rawLabel;
      ScoreCategorical.Builder builder = ScoreCategorical.newBuilder();
      ScoreCategorical.ScoreCategory.Builder scb = ScoreCategorical.ScoreCategory.newBuilder();
      scb.setScore(sc.getScore());
      scb.setCategory(sc.getCategory());
      if (sc.getNumericSequence() != null && sc.getNumericSequence().size() > 0) {
        scb.addAllNumericSequence(sc.getNumericSequence());
      }
      builder.setScoreCategory(scb);
      return label.setScoreCategorical(builder).build();
    }
    throw new IllegalArgumentException(
        "Illegal label "
            + rawLabel
            + ", must be oneof: boolean, String, int, long, short, float, double, ScoreCategorical");
  }

  protected static <T> void validatePredictionActualMatches(
      final T predictionLabel, final T actualLabel) throws IllegalArgumentException {
    if (predictionLabel != null && actualLabel != null) {
      if (predictionLabel.getClass() != actualLabel.getClass()) {
        throw new IllegalArgumentException(
            "predictionLabel and actualLabel must be of the same type. predictionLabel: "
                + predictionLabel.getClass()
                + "actualLabel: "
                + actualLabel.getClass());
      }
    }
  }

  protected static <T> void validateBulkPredictionActualMatches(
      final List<T> predictionLabels, final List<T> actualLabels) throws IllegalArgumentException {
    if (predictionLabels != null && actualLabels != null) {
      if (predictionLabels.getClass().getComponentType()
          != actualLabels.getClass().getComponentType()) {
        throw new IllegalArgumentException(
            "predictionLabel and actualLabel must be of the same type. predictionLabel: "
                + predictionLabels.getClass().getComponentType()
                + " actualLabel: "
                + actualLabels.getClass().getComponentType());
      }
    }
  }

  private static Public.Embedding.Builder convertEmbedding(Embedding embedding) {
    Public.Embedding.Builder embeddingBuilder = Public.Embedding.newBuilder();
    embeddingBuilder.addAllVector(embedding.getVector());
    Public.Embedding.TokenArray.Builder tokenArrayBuilder =
        Public.Embedding.TokenArray.newBuilder();
    tokenArrayBuilder.addAllTokens(embedding.getRawData());
    Public.Embedding.RawData.Builder rawDataBuilder =
        Public.Embedding.RawData.newBuilder().setTokenArray(tokenArrayBuilder);
    embeddingBuilder.setRawData(rawDataBuilder);
    com.google.protobuf.StringValue.Builder linkToDataBuilder =
        com.google.protobuf.StringValue.newBuilder().setValue(embedding.getLinkToData());
    return embeddingBuilder.setLinkToData(linkToDataBuilder);
  }

  private static void validateEmbedding(String embName, Embedding embedding) {
    if (embedding.getVector() == null) {
      throw new IllegalArgumentException(
          "Embedding feature \"" + embName + "\" must contain a vector");
    }
    if (embedding.getVector().size() == 0) {
      throw new IllegalArgumentException(
          "Embedding vector must not be empty. Got "
              + embName
              + ".vector = "
              + embedding.getVector());
    }
  }

  @SuppressWarnings({"unchecked"})
  private static <T> Value convertValue(final String name, final T rawValue)
      throws IllegalArgumentException {
    Value.Builder val = Value.newBuilder();
    if (rawValue instanceof String) {
      return val.setString((String) rawValue).build();
    } else if (rawValue instanceof Integer
        || rawValue instanceof Long
        || rawValue instanceof Short) {
      return val.setInt(((Number) rawValue).longValue()).build();
    } else if (rawValue instanceof Double) {
      return val.setDouble((Double) rawValue).build();
    } else if (rawValue instanceof Float) {
      return val.setDouble(((Float) rawValue).doubleValue()).build();
    } else if (rawValue instanceof Boolean) {
      return val.setString(((Boolean) rawValue).toString()).build();
    } else if (rawValue instanceof Collection) {
      MultiValue.Builder values = MultiValue.newBuilder();
      for (T value : (List<T>) rawValue) {
        if (value instanceof String) {
          values.addValues((String) value);
        } else {
          throw new IllegalArgumentException(
              "Elements of multivalue feature " + name + " must be Strings");
        }
      }
      return val.setMultiValue(values).build();
    } else if (rawValue instanceof Embedding) {
      Embedding embedding = (Embedding) rawValue;
      validateEmbedding(name, embedding);
      return val.setEmbedding(convertEmbedding(embedding)).build();
    }
    throw new IllegalArgumentException(
        "Illegal feature type: " + rawValue.getClass().getSimpleName() + " for feature: " + name);
  }
}
