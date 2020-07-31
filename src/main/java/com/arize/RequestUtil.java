package com.arize;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

public class RequestUtil {

    protected static HttpPost buildRequest(final String body) {
        HttpPost req = new HttpPost();
        req.setEntity(new StringEntity(body, Charset.forName("UTF-8")));
        return req;
    }

    public static int getResponseCode(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

    public static String getResponseBody(HttpResponse response) throws IOException {
        final HttpEntity entity = response.getEntity();
        return entity == null ? null : EntityUtils.toString(entity, StandardCharsets.UTF_8);
    }

}