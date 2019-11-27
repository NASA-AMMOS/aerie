package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

public interface HttpHandler {
    HttpResponse execute(HttpUriRequest httpUriRequest) throws IOException;
}
