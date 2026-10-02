package com.arize.surrogate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Generates surrogate-model feature-importance values ("SHAP-style" local
 * attributions) without requiring the original model.
 *
 * <p>This is the Java analog of the Arize Python SDK's surrogate explainer
 * ({@code arize[MimicExplainer]}). Given the same features and predictions you
 * would log to Arize, it trains a surrogate gradient-boosted tree model that
 * mimics the predictions and reads per-record, per-feature local attributions
 * off that surrogate. The resulting maps can be passed directly as the
 * {@code shapValues} argument of {@link com.arize.ArizeClient#bulkLog}.
 *
 * <p>Usage mirrors the Python "augment-before-log" pattern:
 *
 * <pre>{@code
 * List<Map<String, ?>> features = ...;            // same list passed to bulkLog
 * List<Double> predictionScores = ...;            // probabilities in [0,1] for classification
 *
 * List<Map<String, Double>> shapValues =
 *     SurrogateExplainer.computeFeatureImportances(
 *         features, predictionScores, ModelType.BINARY_CLASSIFICATION);
 *
 * arize.bulkLog(modelId, version, ids, features, embeddings, tags,
 *               labels, actuals, shapValues, timestamps);
 * }</pre>
 *
 * <p>Validation rules match the Python SDK:
 *
 * <ul>
 *   <li>Categorical model types ({@link ModelType#SCORE_CATEGORICAL},
 *       {@link ModelType#BINARY_CLASSIFICATION}) require prediction scores in
 *       {@code [0, 1]}.
 *   <li>Numeric model types ({@link ModelType#NUMERIC}, {@link ModelType#REGRESSION})
 *       require finite (non-NaN, non-infinite) predictions.
 *   <li>If no features are present, explainability is a no-op and empty maps are
 *       returned for every record.
 * </ul>
 *
 * <p>Non-numeric (String / Boolean) feature values are integer-encoded per
 * column, mirroring the label-encoding the Python implementation applies before
 * fitting the surrogate. Missing feature values are supported and routed through
 * the trees via a learned default direction.
 *
 * <p>This explainer is self-contained and deterministic — it adds no native or
 * third-party machine-learning dependencies to the SDK.
 *
 * <p>Note: the surrogate is trained on a (possibly sampled) subset of the
 * records for speed, but importances are computed for <em>every</em> record. A
 * meaningful surrogate requires enough records to fit tree splits; with only a
 * handful of records the surrogate stays trivial and every importance is
 * {@code 0.0}.
 */
public final class SurrogateExplainer {

  // Defaults mirror the modest, fast-to-fit surrogate used for explanations.
  private static final int DEFAULT_NUM_TREES = 50;
  private static final double DEFAULT_LEARNING_RATE = 0.1;
  private static final int DEFAULT_MAX_DEPTH = 4;
  private static final int DEFAULT_MIN_SAMPLES_LEAF = 5;

  // Sampling bounds for training, matching the Python implementation: cap the
  // total number of feature "cells" to 20M, clamped to [1_000, 100_000] rows.
  private static final long MAX_CELLS = 20_000_000L;
  private static final int MIN_SAMPLE_ROWS = 1_000;
  private static final int MAX_SAMPLE_ROWS = 100_000;

  private final int numTrees;
  private final double learningRate;
  private final int maxDepth;
  private final int minSamplesLeaf;
  private final long seed;

  private SurrogateExplainer(Builder b) {
    this.numTrees = b.numTrees;
    this.learningRate = b.learningRate;
    this.maxDepth = b.maxDepth;
    this.minSamplesLeaf = b.minSamplesLeaf;
    this.seed = b.seed;
  }

  /**
   * Compute feature-importance values using a default-configured surrogate
   * explainer.
   *
   * @param features per-record feature maps (the same list passed to {@code bulkLog})
   * @param predictions per-record predictions; probabilities in {@code [0, 1]} for
   *     categorical model types, the predicted value for numeric model types
   * @param modelType the model type being explained
   * @return per-record maps of feature name to local importance value, suitable for
   *     the {@code shapValues} argument of {@code bulkLog}
   * @throws IllegalArgumentException if inputs are inconsistent or violate the
   *     model-type constraints described in the class documentation
   */
  public static List<Map<String, Double>> computeFeatureImportances(
      List<Map<String, ?>> features, List<Double> predictions, ModelType modelType) {
    return new Builder().build().explain(features, predictions, modelType);
  }

  /**
   * Compute feature-importance values for the supplied records.
   *
   * @see #computeFeatureImportances(List, List, ModelType)
   */
  public List<Map<String, Double>> explain(
      List<Map<String, ?>> features, List<Double> predictions, ModelType modelType) {
    if (features == null) {
      throw new IllegalArgumentException("features cannot be null");
    }
    if (predictions == null) {
      throw new IllegalArgumentException("predictions cannot be null");
    }
    if (modelType == null) {
      throw new IllegalArgumentException("modelType cannot be null");
    }
    if (features.size() != predictions.size()) {
      throw new IllegalArgumentException(
          "features.size() ("
              + features.size()
              + ") must equal predictions.size() ("
              + predictions.size()
              + ")");
    }

    int nRows = features.size();

    // Canonical, deterministic feature ordering across all records.
    TreeSet<String> featureNameSet = new TreeSet<>();
    for (Map<String, ?> row : features) {
      if (row != null) {
        featureNameSet.addAll(row.keySet());
      }
    }
    List<String> featureNames = new ArrayList<>(featureNameSet);
    int nFeatures = featureNames.size();

    // No features => surrogate explainability is a no-op (matches Python).
    if (nFeatures == 0 || nRows == 0) {
      List<Map<String, Double>> empty = new ArrayList<>(nRows);
      for (int i = 0; i < nRows; i++) {
        empty.add(new HashMap<String, Double>());
      }
      return empty;
    }

    double[] y = buildTarget(predictions, modelType);
    double[][] x = buildFeatureMatrix(features, featureNames);

    int[] trainIdx = sampleTrainingIndices(nRows, nFeatures);

    GradientBoostedTrees model =
        new GradientBoostedTrees(numTrees, learningRate, maxDepth, minSamplesLeaf);
    model.fit(x, y, trainIdx);

    List<Map<String, Double>> result = new ArrayList<>(nRows);
    for (int i = 0; i < nRows; i++) {
      double[] contrib = model.contributions(x[i]);
      Map<String, Double> row = new LinkedHashMap<>();
      for (int f = 0; f < nFeatures; f++) {
        row.put(featureNames.get(f), contrib[f]);
      }
      result.add(row);
    }
    return result;
  }

  private static double[] buildTarget(List<Double> predictions, ModelType modelType) {
    int n = predictions.size();
    double[] y = new double[n];
    for (int i = 0; i < n; i++) {
      Double p = predictions.get(i);
      if (p == null) {
        throw new IllegalArgumentException(
            "prediction at index " + i + " is null; predictions must not be null");
      }
      y[i] = p;
    }

    if (modelType.isCategorical()) {
      double min = Double.POSITIVE_INFINITY;
      double max = Double.NEGATIVE_INFINITY;
      for (double v : y) {
        min = Math.min(min, v);
        max = Math.max(max, v);
      }
      if (!(min >= 0.0 && min <= 1.0) || !(max >= 0.0 && max <= 1.0)) {
        throw new IllegalArgumentException(
            "To calculate surrogate explainability for "
                + modelType
                + ", prediction scores must be between 0 and 1, but current prediction scores "
                + "range from "
                + min
                + " to "
                + max
                + ".");
      }
    } else if (modelType.isNumeric()) {
      int nonFinite = 0;
      for (double v : y) {
        if (!isFinite(v)) {
          nonFinite++;
        }
      }
      if (nonFinite > 0) {
        throw new IllegalArgumentException(
            "To calculate surrogate explainability for "
                + modelType
                + ", predictions must not contain NaN or infinite values, but "
                + nonFinite
                + " NaN or infinite value(s) were found.");
      }
    } else {
      throw new IllegalArgumentException(
          "Surrogate explainability is not supported for the specified model type " + modelType + ".");
    }
    return y;
  }

  /**
   * Build the numeric feature matrix. A column is treated as numeric when every
   * non-null value is a {@link Number}; otherwise it is integer-encoded by
   * sorted distinct string representation (label encoding). Missing values are
   * encoded as {@link Double#NaN}.
   */
  private static double[][] buildFeatureMatrix(
      List<Map<String, ?>> features, List<String> featureNames) {
    int nRows = features.size();
    int nFeatures = featureNames.size();

    // First pass: decide per-column whether it is numeric or categorical, and
    // build label encoders for categorical columns.
    boolean[] numeric = new boolean[nFeatures];
    List<Map<String, Integer>> encoders = new ArrayList<>(nFeatures);
    for (int f = 0; f < nFeatures; f++) {
      numeric[f] = true;
      encoders.add(null);
    }
    for (int f = 0; f < nFeatures; f++) {
      String name = featureNames.get(f);
      for (Map<String, ?> row : features) {
        Object v = row == null ? null : row.get(name);
        if (v != null && !(v instanceof Number)) {
          numeric[f] = false;
          break;
        }
      }
      if (!numeric[f]) {
        TreeSet<String> distinct = new TreeSet<>();
        for (Map<String, ?> row : features) {
          Object v = row == null ? null : row.get(name);
          if (v != null) {
            distinct.add(String.valueOf(v));
          }
        }
        Map<String, Integer> encoder = new TreeMap<>();
        int code = 0;
        for (String value : distinct) {
          encoder.put(value, code++);
        }
        encoders.set(f, encoder);
      }
    }

    double[][] x = new double[nRows][nFeatures];
    for (int r = 0; r < nRows; r++) {
      Map<String, ?> row = features.get(r);
      for (int f = 0; f < nFeatures; f++) {
        Object v = row == null ? null : row.get(featureNames.get(f));
        if (v == null) {
          x[r][f] = Double.NaN;
        } else if (numeric[f]) {
          x[r][f] = ((Number) v).doubleValue();
        } else {
          Integer code = encoders.get(f).get(String.valueOf(v));
          x[r][f] = code == null ? Double.NaN : code.doubleValue();
        }
      }
    }
    return x;
  }

  /**
   * Select the training row indices. Mirrors the Python sampling rule: cap the
   * number of feature cells to {@value #MAX_CELLS}, clamped to
   * {@code [MIN_SAMPLE_ROWS, MAX_SAMPLE_ROWS]} rows. When the dataset is small
   * enough, all rows are used. Sampling is deterministic given {@code seed}.
   */
  private int[] sampleTrainingIndices(int nRows, int nFeatures) {
    long perFeatureCap = Math.max(MIN_SAMPLE_ROWS, MAX_CELLS / nFeatures);
    long cappedRows = Math.min(MAX_SAMPLE_ROWS, perFeatureCap);
    int sampleSize = (int) Math.min(nRows, cappedRows);

    if (sampleSize >= nRows) {
      int[] all = new int[nRows];
      for (int i = 0; i < nRows; i++) {
        all[i] = i;
      }
      return all;
    }

    // Partial Fisher-Yates shuffle to pick `sampleSize` distinct indices.
    int[] pool = new int[nRows];
    for (int i = 0; i < nRows; i++) {
      pool[i] = i;
    }
    java.util.Random random = new java.util.Random(seed);
    for (int i = 0; i < sampleSize; i++) {
      int j = i + random.nextInt(nRows - i);
      int tmp = pool[i];
      pool[i] = pool[j];
      pool[j] = tmp;
    }
    int[] sample = new int[sampleSize];
    System.arraycopy(pool, 0, sample, 0, sampleSize);
    return sample;
  }

  private static boolean isFinite(double v) {
    return !Double.isNaN(v) && !Double.isInfinite(v);
  }

  /** Builder for tuning the surrogate model used to generate explanations. */
  public static final class Builder {
    private int numTrees = DEFAULT_NUM_TREES;
    private double learningRate = DEFAULT_LEARNING_RATE;
    private int maxDepth = DEFAULT_MAX_DEPTH;
    private int minSamplesLeaf = DEFAULT_MIN_SAMPLES_LEAF;
    private long seed = 42L;

    /** Number of boosting iterations (trees) in the surrogate model. */
    public Builder numTrees(int numTrees) {
      if (numTrees < 1) {
        throw new IllegalArgumentException("numTrees must be >= 1");
      }
      this.numTrees = numTrees;
      return this;
    }

    /** Shrinkage applied to each tree's contribution. */
    public Builder learningRate(double learningRate) {
      if (!(learningRate > 0.0)) {
        throw new IllegalArgumentException("learningRate must be > 0");
      }
      this.learningRate = learningRate;
      return this;
    }

    /** Maximum depth of each surrogate tree. */
    public Builder maxDepth(int maxDepth) {
      if (maxDepth < 1) {
        throw new IllegalArgumentException("maxDepth must be >= 1");
      }
      this.maxDepth = maxDepth;
      return this;
    }

    /** Minimum number of training samples required in a leaf. */
    public Builder minSamplesLeaf(int minSamplesLeaf) {
      if (minSamplesLeaf < 1) {
        throw new IllegalArgumentException("minSamplesLeaf must be >= 1");
      }
      this.minSamplesLeaf = minSamplesLeaf;
      return this;
    }

    /** Seed controlling deterministic training-set sampling for large datasets. */
    public Builder seed(long seed) {
      this.seed = seed;
      return this;
    }

    public SurrogateExplainer build() {
      return new SurrogateExplainer(this);
    }
  }
}
