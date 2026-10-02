package com.arize.surrogate;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SurrogateExplainerTest {

  /**
   * Build a separable binary dataset where the prediction score is driven almost
   * entirely by a single feature ("signal"), with a second irrelevant feature
   * ("noise"). The surrogate explainer should attribute most importance to the
   * signal feature.
   */
  @Test
  public void attributesImportanceToDrivingFeature() {
    List<Map<String, ?>> features = new ArrayList<>();
    List<Double> scores = new ArrayList<>();
    for (int i = 0; i < 200; i++) {
      double signal = (i % 2 == 0) ? 1.0 : 0.0;
      double noise = (i % 3 == 0) ? 1.0 : 0.0;
      Map<String, Object> row = new HashMap<>();
      row.put("signal", signal);
      row.put("noise", noise);
      features.add(row);
      // Score depends only on signal.
      scores.add(signal == 1.0 ? 0.95 : 0.05);
    }

    List<Map<String, Double>> importances =
        SurrogateExplainer.computeFeatureImportances(
            features, scores, ModelType.BINARY_CLASSIFICATION);

    assertEquals(features.size(), importances.size());

    double totalSignal = 0.0;
    double totalNoise = 0.0;
    for (Map<String, Double> row : importances) {
      assertTrue(row.containsKey("signal"));
      assertTrue(row.containsKey("noise"));
      totalSignal += Math.abs(row.get("signal"));
      totalNoise += Math.abs(row.get("noise"));
    }
    assertTrue(
        "signal should carry more importance than noise (signal="
            + totalSignal
            + ", noise="
            + totalNoise
            + ")",
        totalSignal > totalNoise * 5.0);
  }

  /** Contributions should sum to (surrogate prediction - baseline), and high vs. low
   * signal rows must receive oppositely-signed signal attributions. */
  @Test
  public void signalContributionSignTracksScore() {
    List<Map<String, ?>> features = new ArrayList<>();
    List<Double> scores = new ArrayList<>();
    for (int i = 0; i < 200; i++) {
      double signal = (i % 2 == 0) ? 1.0 : 0.0;
      Map<String, Object> row = new HashMap<>();
      row.put("signal", signal);
      features.add(row);
      scores.add(signal == 1.0 ? 0.9 : 0.1);
    }

    List<Map<String, Double>> importances =
        SurrogateExplainer.computeFeatureImportances(
            features, scores, ModelType.BINARY_CLASSIFICATION);

    double highRow = importances.get(0).get("signal"); // signal == 1.0
    double lowRow = importances.get(1).get("signal"); // signal == 0.0
    assertTrue("high-score row should get positive signal attribution", highRow > 0.0);
    assertTrue("low-score row should get negative signal attribution", lowRow < 0.0);
  }

  @Test
  public void numericModelTypeIsSupported() {
    List<Map<String, ?>> features = new ArrayList<>();
    List<Double> predictions = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      Map<String, Object> row = new HashMap<>();
      double x = i;
      row.put("x", x);
      features.add(row);
      predictions.add(2.0 * x + 5.0); // unbounded numeric target
    }

    List<Map<String, Double>> importances =
        SurrogateExplainer.computeFeatureImportances(features, predictions, ModelType.REGRESSION);

    assertEquals(100, importances.size());
    double total = 0.0;
    for (Map<String, Double> row : importances) {
      total += Math.abs(row.get("x"));
    }
    assertTrue("monotonic target should yield non-zero attribution to x", total > 0.0);
  }

  @Test
  public void categoricalScoreOutOfRangeThrows() {
    List<Map<String, ?>> features = new ArrayList<>();
    features.add(singletonFeature("a", 1.0));
    features.add(singletonFeature("a", 2.0));
    List<Double> scores = Arrays.asList(0.5, 1.5); // 1.5 is out of [0,1]

    try {
      SurrogateExplainer.computeFeatureImportances(
          features, scores, ModelType.BINARY_CLASSIFICATION);
      fail("expected IllegalArgumentException for out-of-range prediction score");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("between 0 and 1"));
    }
  }

  @Test
  public void numericNonFiniteThrows() {
    List<Map<String, ?>> features = new ArrayList<>();
    features.add(singletonFeature("a", 1.0));
    features.add(singletonFeature("a", 2.0));
    List<Double> predictions = Arrays.asList(1.0, Double.NaN);

    try {
      SurrogateExplainer.computeFeatureImportances(features, predictions, ModelType.NUMERIC);
      fail("expected IllegalArgumentException for non-finite prediction");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("NaN or infinite"));
    }
  }

  @Test
  public void noFeaturesIsNoOp() {
    List<Map<String, ?>> features = new ArrayList<>();
    features.add(new HashMap<String, Object>());
    features.add(new HashMap<String, Object>());
    List<Double> scores = Arrays.asList(0.2, 0.8);

    List<Map<String, Double>> importances =
        SurrogateExplainer.computeFeatureImportances(
            features, scores, ModelType.BINARY_CLASSIFICATION);

    assertEquals(2, importances.size());
    assertTrue(importances.get(0).isEmpty());
    assertTrue(importances.get(1).isEmpty());
  }

  @Test
  public void mismatchedSizesThrows() {
    List<Map<String, ?>> features = new ArrayList<>();
    features.add(singletonFeature("a", 1.0));
    List<Double> scores = Arrays.asList(0.2, 0.8);

    try {
      SurrogateExplainer.computeFeatureImportances(
          features, scores, ModelType.BINARY_CLASSIFICATION);
      fail("expected IllegalArgumentException for mismatched sizes");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("must equal"));
    }
  }

  @Test
  public void handlesStringAndMissingFeatureValues() {
    List<Map<String, ?>> features = new ArrayList<>();
    List<Double> scores = new ArrayList<>();
    String[] categories = {"red", "green", "blue"};
    for (int i = 0; i < 120; i++) {
      Map<String, Object> row = new HashMap<>();
      row.put("color", categories[i % 3]);
      if (i % 5 != 0) {
        row.put("count", (double) (i % 7)); // sometimes missing
      }
      features.add(row);
      scores.add((i % 3 == 0) ? 0.9 : 0.2);
    }

    List<Map<String, Double>> importances =
        SurrogateExplainer.computeFeatureImportances(
            features, scores, ModelType.SCORE_CATEGORICAL);

    assertEquals(120, importances.size());
    for (Map<String, Double> row : importances) {
      assertTrue(row.containsKey("color"));
      assertTrue(row.containsKey("count"));
      assertFalse(Double.isNaN(row.get("color")));
      assertFalse(Double.isNaN(row.get("count")));
    }
  }

  @Test
  public void resultsAreDeterministic() {
    List<Map<String, ?>> features = new ArrayList<>();
    List<Double> scores = new ArrayList<>();
    for (int i = 0; i < 150; i++) {
      Map<String, Object> row = new HashMap<>();
      row.put("a", (double) (i % 4));
      row.put("b", (double) (i % 9));
      features.add(row);
      scores.add(((i % 4) >= 2) ? 0.8 : 0.3);
    }

    List<Map<String, Double>> first =
        SurrogateExplainer.computeFeatureImportances(
            features, scores, ModelType.BINARY_CLASSIFICATION);
    List<Map<String, Double>> second =
        SurrogateExplainer.computeFeatureImportances(
            features, scores, ModelType.BINARY_CLASSIFICATION);

    assertEquals(first, second);
  }

  @Test
  public void tooFewRecordsYieldZeroImportances() {
    // Fewer records than the surrogate needs to fit a split: importances must
    // still be well-formed (present and finite), just all zero.
    List<Map<String, ?>> features = new ArrayList<>();
    List<Double> scores = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      Map<String, Object> row = new HashMap<>();
      row.put("a", (double) i);
      features.add(row);
      scores.add(i >= 2 ? 0.9 : 0.1);
    }

    List<Map<String, Double>> importances =
        SurrogateExplainer.computeFeatureImportances(
            features, scores, ModelType.BINARY_CLASSIFICATION);

    assertEquals(4, importances.size());
    for (Map<String, Double> row : importances) {
      assertEquals(0.0, row.get("a"), 0.0);
    }
  }

  private static Map<String, ?> singletonFeature(String name, double value) {
    Map<String, Object> row = new HashMap<>();
    row.put(name, value);
    return row;
  }
}
