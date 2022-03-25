package com.arize;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper class holding future response
 */
public class Response {

    /**
     * Private store for properties
     */
    private final Future<HttpResponse> future;
    private HttpResponse response;

    /**
     * Wrapper for a Future HttpResponse to abstract away the underlying protocol
     *
     * @param future Future-wrapped HttpResponse
     */
    protected Response(final Future<HttpResponse> future) {
        this.future = future;
    }

    /**
     * Waits if necessary for at most the given time for the computation to
     * complete.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public void resolve(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        this.response = future.get(timeout, unit);
    }

    /**
     * Waits if necessary for the computation to complete.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void resolve() throws InterruptedException, ExecutionException {
        this.response = future.get();
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves the
     * response code.
     *
     * @return ResponseCode The response code for the api call
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public ResponseCode getResponseCode() throws InterruptedException, ExecutionException {
        if (this.response == null) {
            this.response = future.get();
        }
        switch (this.response.getStatusLine().getStatusCode()) {
            case 200:
                return ResponseCode.OK;
            case 400:
                return ResponseCode.BAD_REQUEST;
            case 403:
                return ResponseCode.AUTHENTICATION_ERROR;
            case 404:
                return ResponseCode.NOT_FOUND;
            default:
                return ResponseCode.UNEXPECTED_FAILURE;
        }
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves the
     * response body.
     *
     * @return The body contents of the response object
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public String getResponseBody() throws IOException, InterruptedException, ExecutionException {
        if (this.response == null) {
            this.response = future.get();
        }
        final HttpEntity entity = response.getEntity();
        return entity == null ? null : EntityUtils.toString(entity, StandardCharsets.UTF_8);
    }

    /**
     * Attempts to cancel execution of this api call.
     *
     * @return boolean
     */
    public boolean cancel() {
        return this.future.cancel(true);
    }

    /**
     * Returns true if this task was cancelled before it completed normally.
     *
     * @return boolean
     */
    public boolean isCancelled() {
        return this.future.isCancelled();
    }

    /**
     * Returns true if this task completed.
     *
     * @return boolean
     */
    public boolean isDone() {
        return this.future.isDone();
    }

    public enum ResponseCode {
        OK, NOT_FOUND, AUTHENTICATION_ERROR, BAD_REQUEST, UNEXPECTED_FAILURE
    }

}
