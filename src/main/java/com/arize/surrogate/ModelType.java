package com.arize.surrogate;

/**
 * Model types supported by {@link SurrogateExplainer}.
 *
 * <p>Mirrors the model-type categories used by the Arize Python SDK's surrogate
 * explainer. Categorical model types are explained against a prediction
 * probability (score) that must lie in {@code [0, 1]}; numeric model types are
 * explained against the predicted value directly.
 */
public enum ModelType {
  /** Numeric model with an unbounded continuous prediction. */
  NUMERIC,
  /** Regression model with an unbounded continuous prediction. */
  REGRESSION,
  /** Score-categorical model whose prediction score is a probability in {@code [0, 1]}. */
  SCORE_CATEGORICAL,
  /** Binary classification model whose prediction score is a probability in {@code [0, 1]}. */
  BINARY_CLASSIFICATION;

  /**
   * @return {@code true} for model types explained against a probability score
   *     constrained to {@code [0, 1]} (score-categorical, binary classification).
   */
  public boolean isCategorical() {
    return this == SCORE_CATEGORICAL || this == BINARY_CLASSIFICATION;
  }

  /**
   * @return {@code true} for model types explained against an unconstrained
   *     numeric prediction (numeric, regression).
   */
  public boolean isNumeric() {
    return this == NUMERIC || this == REGRESSION;
  }
}
