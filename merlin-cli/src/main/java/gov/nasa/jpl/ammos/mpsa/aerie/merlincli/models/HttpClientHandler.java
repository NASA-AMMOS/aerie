package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

public class HttpClientHandler implements HttpHandler {
    private final HttpClient httpClient;

    public HttpClientHandler(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public HttpResponse execute(HttpUriRequest httpUriRequest) throws IOException {
        return this.httpClient.execute(httpUriRequest);
    }
}
