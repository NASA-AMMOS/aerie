package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.ApiContractViolationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class RemoteAdaptationRepository implements AdaptationRepository {

    private final String baseURL = "http://localhost:27182/adaptations";
    private final String activityPath = "activities";
    private HttpHandler httpClient;

    public RemoteAdaptationRepository(HttpHandler httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String createAdaptation(Adaptation adaptation, File adaptationJar) throws InvalidAdaptationException {
        List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("name", adaptation.getName()));
        parameters.add(new BasicNameValuePair("version", adaptation.getVersion()));
        if (adaptation.getMission() != null) parameters.add(new BasicNameValuePair("mission", adaptation.getMission()));
        if (adaptation.getOwner() != null) parameters.add(new BasicNameValuePair("owner", adaptation.getOwner()));

        HttpEntity entity = EntityBuilder.create()
            .setFile(adaptationJar)
            .setParameters(parameters)
            .build();

        HttpResponse response;
        try {
            HttpPost request = new HttpPost(baseURL);
            request.setHeader(entity.getContentType());
            request.setEntity(entity);

            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_CREATED:
                if (response.containsHeader("location")) throw new ApiContractViolationException("No ID found for created adaptation");

                // ID for the created entity is in the location header
                return response.getFirstHeader("location").toString();

            case HttpStatus.SC_BAD_REQUEST:
                // This should not have happened; this method was responsible for serializing the adaptation.
                throw new Error("Adaptation serivce rejected the request body when posting an adaptation");

            case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                // TODO: Add information about what was wrong from the response
                throw new InvalidAdaptationException();

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from plan service");
        }
    }

    // TODO: Pull this method into a utility class, it is used by both RemotePlanRepository and RemoteAdaptationRepository
    private void addJsonToRequest(HttpEntityEnclosingRequest request, String json) throws UnsupportedEncodingException {
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setEntity(new StringEntity(json));
    }
}
