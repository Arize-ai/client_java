package com.arize.surrogate;

import java.util.Arrays;

/**
 * A single CART regression tree trained with squared-error loss.
 *
 * <p>The tree is used as a weak learner inside {@link GradientBoostedTrees}. In
 * addition to standard prediction, it exposes a per-feature local attribution
 * via the decision-path (a.k.a. Saabas) decomposition: walking from the root to
 * the leaf for a given input, each split node attributes {@code childValue -
 * nodeValue} to the feature it splits on. Summed along the path, these
 * attributions telescope to {@code leafValue - rootValue}, giving an additive
 * breakdown of the tree's output across features.
 *
 * <p>Missing values ({@link Double#NaN}) are routed to the branch chosen at
 * training time (the side that received the majority of samples), recorded per
 * node as {@code defaultLeft}.
 */
final class RegressionTree {

  /** Minimum variance-reduction gain required to accept a split. */
  private static final double MIN_GAIN = 1e-12;

  private final int maxDepth;
  private final int minSamplesLeaf;
  private Node root;

  RegressionTree(int maxDepth, int minSamplesLeaf) {
    this.maxDepth = maxDepth;
    this.minSamplesLeaf = minSamplesLeaf;
  }

  /**
   * Fit the tree on the supplied rows.
   *
   * @param x feature matrix ({@code [nRows][nFeatures]}); may contain {@link Double#NaN}
   * @param target regression target, parallel to {@code x}
   * @param indices the subset of row indices to train on
   */
  void fit(double[][] x, double[] target, int[] indices) {
    this.root = build(x, target, indices, 0);
  }

  /** @return the tree's prediction for a single input row. */
  double predict(double[] row) {
    Node node = root;
    while (!node.leaf) {
      node = goLeft(node, row) ? node.left : node.right;
    }
    return node.value;
  }

  /**
   * Accumulate this tree's per-feature contributions for {@code row} into
   * {@code contrib}, scaled by {@code scale}.
   */
  void addContributions(double[] row, double scale, double[] contrib) {
    Node node = root;
    while (!node.leaf) {
      Node child = goLeft(node, row) ? node.left : node.right;
      contrib[node.feature] += scale * (child.value - node.value);
      node = child;
    }
  }

  private static boolean goLeft(Node node, double[] row) {
    double v = row[node.feature];
    if (Double.isNaN(v)) {
      return node.defaultLeft;
    }
    return v <= node.threshold;
  }

  private Node build(double[][] x, double[] target, int[] idx, int depth) {
    Node node = new Node();
    node.value = mean(target, idx);

    if (depth >= maxDepth || idx.length < 2 * minSamplesLeaf) {
      node.leaf = true;
      return node;
    }

    Split best = findBestSplit(x, target, idx);
    if (best == null) {
      node.leaf = true;
      return node;
    }

    node.leaf = false;
    node.feature = best.feature;
    node.threshold = best.threshold;
    node.defaultLeft = best.defaultLeft;
    node.left = build(x, target, best.leftIdx, depth + 1);
    node.right = build(x, target, best.rightIdx, depth + 1);
    return node;
  }

  private Split findBestSplit(double[][] x, double[] target, int[] idx) {
    int nFeatures = x[0].length;
    double totalSum = 0.0;
    for (int i : idx) {
      totalSum += target[i];
    }
    int totalCount = idx.length;
    double parentScore = totalSum * totalSum / totalCount;

    Split best = null;
    double bestScore = parentScore + MIN_GAIN;

    for (int f = 0; f < nFeatures; f++) {
      // Partition into present (non-NaN) and missing rows for this feature.
      int[] present = new int[idx.length];
      int presentCount = 0;
      double missingSum = 0.0;
      int missingCount = 0;
      for (int i : idx) {
        double v = x[i][f];
        if (Double.isNaN(v)) {
          missingSum += target[i];
          missingCount++;
        } else {
          present[presentCount++] = i;
        }
      }
      if (presentCount < 2) {
        continue;
      }
      final int[] presentIdx = Arrays.copyOf(present, presentCount);
      final int feature = f;
      final double[][] xx = x;
      sortByFeature(presentIdx, xx, feature);

      double presentSum = 0.0;
      for (int i : presentIdx) {
        presentSum += target[i];
      }

      double leftSum = 0.0;
      int leftCount = 0;
      for (int p = 0; p < presentCount - 1; p++) {
        int i = presentIdx[p];
        leftSum += target[i];
        leftCount++;

        double vCur = xx[i][feature];
        double vNext = xx[presentIdx[p + 1]][feature];
        if (vCur == vNext) {
          continue; // can't split between identical values
        }

        double presentRightSum = presentSum - leftSum;
        int presentRightCount = presentCount - leftCount;

        // Try assigning missing rows to the left branch, then to the right.
        double[] candidate =
            evaluate(leftSum, leftCount, presentRightSum, presentRightCount, missingSum, missingCount);
        if (candidate != null && candidate[0] > bestScore) {
          bestScore = candidate[0];
          best = new Split();
          best.feature = feature;
          best.threshold = (vCur + vNext) / 2.0;
          best.defaultLeft = candidate[1] != 0.0;
        }
      }
    }

    if (best == null) {
      return null;
    }
    partition(x, idx, best);
    return best;
  }

  /**
   * @return {@code [score, defaultLeftFlag]} for the better of the two missing-row
   *     assignments, or {@code null} if neither side satisfies {@code minSamplesLeaf}.
   */
  private double[] evaluate(
      double leftSum,
      int leftCount,
      double rightSum,
      int rightCount,
      double missingSum,
      int missingCount) {
    double bestScore = Double.NEGATIVE_INFINITY;
    double bestDefaultLeft = -1.0;

    // Missing -> left.
    int lc = leftCount + missingCount;
    int rc = rightCount;
    if (lc >= minSamplesLeaf && rc >= minSamplesLeaf) {
      double ls = leftSum + missingSum;
      double score = ls * ls / lc + rightSum * rightSum / rc;
      if (score > bestScore) {
        bestScore = score;
        bestDefaultLeft = 1.0;
      }
    }

    // Missing -> right.
    lc = leftCount;
    rc = rightCount + missingCount;
    if (lc >= minSamplesLeaf && rc >= minSamplesLeaf) {
      double rs = rightSum + missingSum;
      double score = leftSum * leftSum / lc + rs * rs / rc;
      if (score > bestScore) {
        bestScore = score;
        bestDefaultLeft = 0.0;
      }
    }

    if (bestDefaultLeft < 0.0) {
      return null;
    }
    return new double[] {bestScore, bestDefaultLeft};
  }

  private static void partition(double[][] x, int[] idx, Split split) {
    int[] left = new int[idx.length];
    int[] right = new int[idx.length];
    int li = 0;
    int ri = 0;
    for (int i : idx) {
      double v = x[i][split.feature];
      boolean toLeft;
      if (Double.isNaN(v)) {
        toLeft = split.defaultLeft;
      } else {
        toLeft = v <= split.threshold;
      }
      if (toLeft) {
        left[li++] = i;
      } else {
        right[ri++] = i;
      }
    }
    split.leftIdx = Arrays.copyOf(left, li);
    split.rightIdx = Arrays.copyOf(right, ri);
  }

  private static void sortByFeature(int[] indices, double[][] x, int feature) {
    // Box to use a comparator; index arrays are small relative to overall work.
    Integer[] boxed = new Integer[indices.length];
    for (int i = 0; i < indices.length; i++) {
      boxed[i] = indices[i];
    }
    Arrays.sort(boxed, (a, b) -> Double.compare(x[a][feature], x[b][feature]));
    for (int i = 0; i < indices.length; i++) {
      indices[i] = boxed[i];
    }
  }

  private static double mean(double[] target, int[] idx) {
    if (idx.length == 0) {
      return 0.0;
    }
    double sum = 0.0;
    for (int i : idx) {
      sum += target[i];
    }
    return sum / idx.length;
  }

  /** A tree node. Internal nodes carry a split; leaves carry only a value. */
  private static final class Node {
    boolean leaf;
    double value;
    int feature;
    double threshold;
    boolean defaultLeft;
    Node left;
    Node right;
  }

  /** A candidate (or chosen) split, plus the row partition it induces. */
  private static final class Split {
    int feature;
    double threshold;
    boolean defaultLeft;
    int[] leftIdx;
    int[] rightIdx;
  }
}
