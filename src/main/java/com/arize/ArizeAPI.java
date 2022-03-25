package com.arize;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ArizeAPI {

    String SDK_VERSION = "1.0.0";

    /**
     * log builds and submits a record to the Arize API.
     *
     * @param modelId             Unique identifier for a given model.
     * @param modelVersion        Optional identifier used to group together a subset of
     *                            predictions and actuals for a given modelId.
     * @param predictionId        Unique identifier for a given prediction.
     * @param features            Optional {@link Map} containing model features. Map key must be
     *                            {@link String} and values are one of: string, int,
     *                            long, short, float, double, boolean.
     * @param predictionLabel     The predicted value for a given set of model inputs.
     *                            Supported boxed types are: boolean, string, int, long,
     *                            short, float, double.
     * @param actualLabel         The latent truth value for a given set of model inputs.
     *                            Supported boxed types are: boolean, string, int, long,
     *                            short, float, double. This can either be supplied at the same time as the predictionLabel
     *                            or on its own once it is known, and it will be linked back by the predictionId
     * @param shapValues          {@link Map} from feature name to shap value.  This can either be supplied at the same time as the
     *                            predictionLabel or on its own, and it will be linked back by the predictionId
     * @param predictionTimestamp The timestamp to which the prediction should be attributed. If not set this defaults
     *                            to the time of receipt
     * @param <T>                 Boxed type for predictionLabel. Supported boxed types
     *                            are: boolean, string, int, long, short, float, double.
     * @return {@link Response}
     * @throws IOException              in case of a network error
     * @throws IllegalArgumentException in case data type for features or label are
     *                                  not supported.
     */
    <T> Response log(
            final String modelId,
            final String modelVersion,
            final String predictionId,
            final Map<String, ?> features,
            final T predictionLabel,
            final T actualLabel,
            final Map<String, Double> shapValues,
            long predictionTimestamp) throws IOException, IllegalArgumentException;


    /**
     * logBulkPrediction builds and submits a Bulk Prediction record to the Arize
     * API.
     *
     * @param modelId              Unique {@link String} identifier for a given
     *                             model.
     * @param modelVersion         Optional {@link String} identifier used to group
     *                             together a subset of predictions and actuals for
     *                             a given modelId.
     * @param predictionIds        {@link List} of unique {@link String}
     *                             indentifiers for predictions. This value is used
     *                             to match latent actual labels to their original
     *                             predictions.
     * @param features             Optional {@link List} of {@link Map} varargs
     *                             containing human-readable and debuggable model
     *                             features. Map key must be {@link String} and
     *                             values are one of: string, int, long, short,
     *                             float, double, boolean.
     * @param predictionLabels     {@link List} of predicted values. Supported boxed
     *                             types are: boolean, string, int, long, short,
     *                             float, double.
     * @param actualLabels         {@link List} of latent actual values. Supported boxed
     *                             types are: boolean, string, int, long, short,
     *                             float, double. This can either be supplied at the same
     *                             time as the predictionLabels or on their own
     *                             once they are known, and they will be linked back by the
     *                             corresponding predictionIds
     * @param shapValues           {@link List} of {@link Map} from feature name to shap value.  Can be provided at the
     *                             same time as the predictionLabels or on their own and they will
     *                             be linked back by the corresponding predictionIds
     * @param predictionTimestamps Optional {@link List} of {@link Long} of unix
     *                             epoch time in milliseconds used to overwrite
     *                             timestamp for a prediction.
     * @param <T>                  Boxed type for predictionLabel and actualLabel. Supported boxed
     *                             types are: boolean, string, int, long, short,
     *                             float, double.
     * @return {@link Response}
     * @throws IOException              in case of a network error
     * @throws IllegalArgumentException in case data type for features or label are
     *                                  not supported.
     */
    <T> Response bulkLog(
            final String modelId,
            final String modelVersion,
            final List<String> predictionIds,
            final List<Map<String, ?>> features,
            final List<T> predictionLabels,
            final List<T> actualLabels,
            final List<Map<String, Double>> shapValues,
            final List<Long> predictionTimestamps) throws IOException, IllegalArgumentException;


    /**
     * @param modelId          Unique {@link String} identifier for a given
     *                         model.
     * @param modelVersion     Optional {@link String} identifier used to group
     *                         together a subset of predictions and actuals for
     *                         a given modelId.
     * @param features         Optional {@link List} of {@link Map} varargs
     *                         containing human-readable and debuggable model
     *                         features. Map key must be {@link String} and
     *                         values are one of: string, int, long, short,
     *                         float, double, boolean.
     * @param predictionLabels {@link List} of predicted values. Supported boxed
     *                         types are: boolean, string, int, long, short,
     *                         float, double.
     * @param actualLabels     {@link List} of latent actual values. Supported boxed
     *                         types are: boolean, string, int, long, short,
     *                         float, double. This can either be supplied at the same
     *                         time as the predictionLabels or on their own
     *                         once they are known, and they will be linked back by the
     *                         corresponding predictionIds
     * @param <T>              Boxed type for predictionLabel and actualLabel. Supported boxed
     *                         types are: boolean, string, int, long, short,
     *                         float, double.
     * @return {@link Response}
     * @throws IOException              in case of a network error
     * @throws IllegalArgumentException in case data type for features or label are
     *                                  not supported.
     */
    <T> Response logTrainingRecords(
            final String modelId,
            final String modelVersion,
            final List<Map<String, ?>> features,
            final List<T> predictionLabels,
            final List<T> actualLabels
    ) throws IOException, IllegalArgumentException;

    /**
     * @param modelId          Unique {@link String} identifier for a given
     *                         model.
     * @param modelVersion     Optional {@link String} identifier used to group
     *                         together a subset of predictions and actuals for
     *                         a given modelId.
     * @param batchId          Unique identifier for the validation batch we are adding data to
     * @param features         Optional {@link List} of {@link Map} varargs
     *                         containing human readable and debuggable model
     *                         features. Map key must be {@link String} and
     *                         values are one of: string, int, long, short,
     *                         float, double, boolean.
     * @param predictionLabels {@link List} of predicted values. Supported boxed
     *                         types are: boolean, string, int, long, short,
     *                         float, double.
     * @param actualLabels     {@link List} of latent actual values. Supported boxed
     *                         types are: boolean, string, int, long, short,
     *                         float, double. This can either be supplied at the same
     *                         time as the predictionLabels or on their own
     *                         once they are known, and they will be linked back by the
     *                         corresponding predictionIds
     * @param <T>              Boxed type for predictionLabel and actualLabel. Supported boxed
     *                         types are: boolean, string, int, long, short,
     *                         float, double.
     * @return {@link Response}
     * @throws IOException              in case of a network error
     * @throws IllegalArgumentException in case data type for features or label are
     *                                  not supported.
     */
    <T> Response logValidationRecords(
            final String modelId,
            final String modelVersion,
            final String batchId,
            final List<Map<String, ?>> features,
            final List<T> predictionLabels,
            final List<T> actualLabels
    ) throws IOException;
}
