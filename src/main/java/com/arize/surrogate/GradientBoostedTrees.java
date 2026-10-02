package com.arize.surrogate;

/**
 * A gradient-boosted regression tree ensemble (squared-error loss) used as the
 * surrogate ("student") model for explainability.
 *
 * <p>Training fits an additive model {@code F(x) = base + lr * sum_t tree_t(x)}
 * where each tree is fit to the residuals of the ensemble so far. The ensemble
 * mimics the original ("teacher") model's predictions as a function of the
 * features; per-feature attributions are then read off the surrogate via the
 * decision-path decomposition of each tree (see {@link RegressionTree}).
 *
 * <p>The reported feature contributions sum to {@code F(x) - baseline}, where
 * {@code baseline = base + lr * sum_t tree_t.rootValue}; the baseline term is
 * not attributed to any feature.
 */
final class GradientBoostedTrees {

  private final int numTrees;
  private final double learningRate;
  private final int maxDepth;
  private final int minSamplesLeaf;

  private double base;
  private RegressionTree[] trees;

  GradientBoostedTrees(int numTrees, double learningRate, int maxDepth, int minSamplesLeaf) {
    this.numTrees = numTrees;
    this.learningRate = learningRate;
    this.maxDepth = maxDepth;
    this.minSamplesLeaf = minSamplesLeaf;
  }

  /**
   * Fit the ensemble.
   *
   * @param x feature matrix ({@code [nRows][nFeatures]})
   * @param y regression target, parallel to {@code x}
   * @param trainIdx the subset of row indices to train on
   */
  void fit(double[][] x, double[] y, int[] trainIdx) {
    this.base = mean(y, trainIdx);

    double[] f = new double[x.length];
    for (int i : trainIdx) {
      f[i] = base;
    }

    this.trees = new RegressionTree[numTrees];
    double[] residual = new double[x.length];
    for (int t = 0; t < numTrees; t++) {
      for (int i : trainIdx) {
        residual[i] = y[i] - f[i];
      }
      RegressionTree tree = new RegressionTree(maxDepth, minSamplesLeaf);
      tree.fit(x, residual, trainIdx);
      for (int i : trainIdx) {
        f[i] += learningRate * tree.predict(x[i]);
      }
      trees[t] = tree;
    }
  }

  /**
   * @return per-feature local contributions for {@code row}; the values sum to
   *     {@code predict(row) - baseline}.
   */
  double[] contributions(double[] row) {
    double[] contrib = new double[row.length];
    for (RegressionTree tree : trees) {
      tree.addContributions(row, learningRate, contrib);
    }
    return contrib;
  }

  private static double mean(double[] y, int[] idx) {
    if (idx.length == 0) {
      return 0.0;
    }
    double sum = 0.0;
    for (int i : idx) {
      sum += y[i];
    }
    return sum / idx.length;
  }
}
