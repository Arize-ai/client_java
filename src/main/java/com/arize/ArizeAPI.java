package com.arize;

import java.io.IOException;
import java.util.Map;

public interface ArizeAPI {

    /**
     * logPrediction builds and submits a Prediction record to the Arize API.
     * 
     * @param <T>             Boxed type for predictionLabel. Supported boxed types
     *                        are: boolean, string, int, long, short, float, double.
     * @param modelId         Unique identifier for a given model.
     * @param modelVersion    Optional identifier used to group together a subset of
     *                        predictions and actuals for a given modelId.
     * @param predictionId    Unique indentifier for a given prediction. This value
     *                        is used to match latent actual labels to their
     *                        original prediction.
     * @param predictionLabel The predicted value for a given set of model inputs.
     *                        Supported boxed types are: boolean, string, int, long,
     *                        short, float, double.
     * @param features        Optional {@link Map} varargs containing human readable
     *                        and debuggable model features. Map key must be
     *                        {@link String} and values are one of: string, int,
     *                        long, short, float, double.
     * @return Response
     * @throws IOException              in case of a network error
     * @throws IllegalArgumentException in case data type for features or label are
     *                                  not supported.
     */
    @SuppressWarnings({ "unchecked" })
    <T> Response logPrediction(String modelId, final String modelVersion, final String predictionId,
            final T predictionLabel, final Map<String, ?>... features) throws IOException, IllegalArgumentException;

    /**
     * logActual builds and submits an Actual record to the Arize API.
     * 
     * @param <T>          Boxed type for actualLabel. Supported boxed types are:
     *                     boolean, string, int, long, short, float, double.
     * @param modelId      Unique identifier for a given model.
     * @param predictionId Unique indentifier for a given actual record. This value
     *                     is used to match the record to their original prediction.
     * @param actualLabel  The actual "truth" label for the original prediction.
     *                     Supported boxed types are: boolean, string, int, long,
     *                     short, float, double.
     * @return Response
     * @throws IOException              in case of a network error
     * @throws IllegalArgumentException in case data type for features or label are
     *                                  not supported.
     */
    <T> Response logActual(final String modelId, final String predictionId, final T actualLabel)
            throws IOException, IllegalArgumentException;

    /**
     * logPrediction builds and submits a Prediction record to the Arize API.
     * 
     * @param <T>             Boxed type for predictionLabel. Supported boxed types
     *                        are: boolean, string, int, long, short, float, double.
     * @param modelId         Unique identifier for a given model.
     * @param modelVersion    Optional identifier used to group together a subset of
     *                        predictions and actuals for a given modelId.
     * @param predictionId    Unique indentifier for a given prediction. This value
     *                        is used to match latent actual labels to their
     *                        original prediction.
     * @param predictionLabel The predicted value for a given set of model inputs.
     *                        Supported boxed types are: boolean, string, int, long,
     *                        short, float, double.
     * @param timestamp       Optional long unix epoch time in milliseconds only
     *                        used to overwrite timestamp for a prediction.
     * @param features        Optional {@link Map} varargs containing human readable
     *                        and debuggable model features. Map key must be
     *                        {@link String} and values are one of: string, int,
     *                        long, short, float, double.
     * @return Response
     * @throws IOException              in case of a network error
     * @throws IllegalArgumentException in case data type for features or label are
     *                                  not supported.
     */
    @SuppressWarnings({ "unchecked" })
    <T> Response logPrediction(String modelId, final String modelVersion, final String predictionId,
            final T predictionLabel, final long timeOverwrite, final Map<String, ?>... features)
            throws IOException, IllegalArgumentException;
}