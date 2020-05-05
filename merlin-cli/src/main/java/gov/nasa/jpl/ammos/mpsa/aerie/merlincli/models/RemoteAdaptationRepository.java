package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.ApiContractViolationException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.HttpUtilities;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JsonUtilities;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import java.io.File;
import java.io.IOException;

public class RemoteAdaptationRepository implements AdaptationRepository {

    private final String baseURL = "http://localhost:27182/adaptations";
    private final String activityPath = "activities";
    private HttpHandler httpClient;

    public RemoteAdaptationRepository(HttpHandler httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String createAdaptation(Adaptation adaptation, File adaptationJar) throws InvalidAdaptationException {
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.addBinaryBody("file", adaptationJar);
        if (adaptation.getName() != null) entityBuilder.addTextBody("name", adaptation.getName());
        if (adaptation.getVersion() != null) entityBuilder.addTextBody("version", adaptation.getVersion());
        if (adaptation.getMission() != null) entityBuilder.addTextBody("mission", adaptation.getMission());
        if (adaptation.getOwner() != null) entityBuilder.addTextBody("owner", adaptation.getOwner());

        HttpResponse response;
        try {
            HttpPost request = new HttpPost(baseURL);
            request.setEntity(entityBuilder.build());

            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_CREATED:
                if (!response.containsHeader("location")) throw new ApiContractViolationException("No ID found for created adaptation");

                // ID for the created entity is in the location header
                return response.getFirstHeader("location").getValue();

            case HttpStatus.SC_BAD_REQUEST:
                // Because the JSON is built by the CLI, we don't know if the user is at fault
                throw new InvalidAdaptationException(
                        HttpUtilities.getErrorMessage(response, "Adaptation creation failed due to unknown reason")
                );

            case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                throw new InvalidAdaptationException(
                        HttpUtilities.getErrorMessage(response, "Invalid Adaptation")
                );

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from adaptation service");
        }
    }

    @Override
    public void deleteAdaptation(String adaptationId) throws AdaptationNotFoundException {
        HttpResponse response;
        try {
            HttpDelete request = new HttpDelete(String.format("%s/%s", baseURL, adaptationId));
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                break;

            case HttpStatus.SC_NOT_FOUND:
                throw new AdaptationNotFoundException(
                        HttpUtilities.getErrorMessage(response, "Adaptation not found")
                );

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from adaptation service");
        }
    }

    @Override
    public Adaptation getAdaptation(String adaptationId) throws AdaptationNotFoundException {
        HttpResponse response;
        try {
            HttpGet request = new HttpGet(String.format("%s/%s", baseURL, adaptationId));
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                try {
                    return JsonUtilities.parseAdaptationJson(response.getEntity().getContent());
                } catch (IOException e) {
                    throw new Error(e);
                }

            case HttpStatus.SC_NOT_FOUND:
                throw new AdaptationNotFoundException(
                        HttpUtilities.getErrorMessage(response, "Adaptation not found")
                );

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from adaptation service");
        }
    }

    @Override
    public String getAdaptationList() {
        HttpResponse response;
        try {
            HttpGet request = new HttpGet(baseURL);
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                try {
                    return new String(response.getEntity().getContent().readAllBytes());
                } catch (IOException e) {
                    throw new Error(e);
                }

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from adaptation service");
        }
    }

    @Override
    public String getActivityTypes(String adaptationId) throws AdaptationNotFoundException {
        HttpResponse response;
        try {
            HttpGet request = new HttpGet(String.format("%s/%s/%s", baseURL, adaptationId, activityPath));
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                try {
                    return JsonUtilities.prettify(new String(response.getEntity().getContent().readAllBytes()));
                } catch (IOException e) {
                    throw new Error(e);
                }

            case HttpStatus.SC_NOT_FOUND:
                throw new AdaptationNotFoundException(
                        HttpUtilities.getErrorMessage(response, "Adaptation not found")
                );

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from adaptation service");
        }
    }

    @Override
    public String getActivityType(String adaptationId, String activityType) throws ActivityTypeNotDefinedException {
        HttpResponse response;
        try {
            HttpGet request = new HttpGet(String.format("%s/%s/%s/%s", baseURL, adaptationId, activityPath, activityType));
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                try {
                    return JsonUtilities.prettify(new String(response.getEntity().getContent().readAllBytes()));
                } catch (IOException e) {
                    throw new Error(e);
                }

            case HttpStatus.SC_NOT_FOUND:
                // TODO: When the adaptation service is updated to distinguish between
                //       AdaptationNotFound or ActivityTypeNotFound errors, update this
                throw new ActivityTypeNotDefinedException(
                        HttpUtilities.getErrorMessage(response, "Activity type or adaptation not found")
                );

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from adaptation service");
        }
    }
}
