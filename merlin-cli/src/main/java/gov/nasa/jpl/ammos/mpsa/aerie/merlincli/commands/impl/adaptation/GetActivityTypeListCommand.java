package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JSONUtilities.prettify;

public class GetActivityTypeListCommand implements Command {

    private HttpHandler httpClient;
    private String adaptationId;
    private String responseBody;
    private int status;

    public GetActivityTypeListCommand(HttpHandler httpClient, String adaptationId) {
        this.httpClient = httpClient;
        this.adaptationId = adaptationId;
        this.status = -1;
    }

    public void execute() {
        String url = String.format("http://localhost:27182/api/adaptations/%s/activities", this.adaptationId);
        HttpGet request = new HttpGet(url);

        try {
            HttpResponse response = this.httpClient.execute(request);

            this.status = response.getStatusLine().getStatusCode();

            if (status == 200 && response.getEntity() != null) {
                String responseString = new String(response.getEntity().getContent().readAllBytes());
                this.responseBody = prettify(responseString);
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    // TODO: This function is defined in multiple commands, it should be moved to a separate file commands include.

    public int getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
