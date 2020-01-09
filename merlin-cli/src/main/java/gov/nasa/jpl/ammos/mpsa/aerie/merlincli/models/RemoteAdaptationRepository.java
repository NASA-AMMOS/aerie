package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.AdaptationCreateFailureException;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
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
    public String createAdaptation(Adaptation adaptation, File adaptationJar) throws AdaptationCreateFailureException {
        HttpPost request = new HttpPost(baseURL);
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("name", adaptation.getName()));
        parameters.add(new BasicNameValuePair("version", adaptation.getVersion()));
        if (adaptation.getMission() != null) parameters.add(new BasicNameValuePair("mission", adaptation.getMission()));
        if (adaptation.getOwner() != null) parameters.add(new BasicNameValuePair("owner", adaptation.getOwner()));

        request.setEntity(EntityBuilder.create()
                .setFile(adaptationJar)
                .setParameters(parameters)
                .build());

        HttpResponse response;
        try {
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        int status = response.getStatusLine().getStatusCode();

        if (status == 201) {
            if (response.containsHeader("location")) {
                return response.getFirstHeader("location").toString();
            } else {
                throw new AdaptationCreateFailureException("No ID found for created adaptation");
            }
        } else {
            String message;
            try {
                message = new String(response.getEntity().getContent().readAllBytes());
            } catch(IOException e) {
                throw new Error(e);
            }
            throw new AdaptationCreateFailureException(message);
        }
    }

    // TODO: Pull this method into a utility class, it is used by both RemotePlanRepository and RemoteAdaptationRepository
    private void addJsonToRequest(HttpEntityEnclosingRequest request, String json) throws UnsupportedEncodingException {
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setEntity(new StringEntity(json));
    }
}
