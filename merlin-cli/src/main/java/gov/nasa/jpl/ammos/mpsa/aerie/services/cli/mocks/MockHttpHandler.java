package gov.nasa.jpl.ammos.mpsa.aerie.services.cli.mocks;

import gov.nasa.jpl.ammos.mpsa.aerie.services.cli.models.HttpHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

public class MockHttpHandler implements HttpHandler {
    private HttpUriRequest lastRequest;

    // Allow test script to store a response to be served to the next request
    private HttpResponse nextResponse;

    public void setNextResponse(HttpResponse response) {
        this.nextResponse = response;
    }

    public HttpUriRequest getLastRequest() {
        return this.lastRequest;
    }

    @Override
    public HttpResponse execute(final HttpUriRequest httpUriRequest) {
        this.lastRequest = httpUriRequest;
        return this.nextResponse;
    }
}
