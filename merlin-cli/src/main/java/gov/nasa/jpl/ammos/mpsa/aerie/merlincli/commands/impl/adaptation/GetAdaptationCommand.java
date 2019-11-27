package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JSONUtilities.prettify;

/**
 * Read the metadata of an adaptation
 */
public class GetAdaptationCommand implements Command {

    private HttpHandler httpClient;
    private String adaptationId;
    private String responseBody;
    private int status;

    public GetAdaptationCommand(HttpHandler httpClient, String adaptationId) {
        this.httpClient = httpClient;
        this.adaptationId = adaptationId;
        this.status = -1;
    }

    @Override
    public void execute() {
        HttpGet request = new HttpGet(String.format("http://localhost:27182/api/adaptations/%s", this.adaptationId));

        try {
            HttpResponse response = this.httpClient.execute(request);

            this.status = response.getStatusLine().getStatusCode();

            if (status == 200) {
                this.responseBody = prettify(response.getEntity().toString());
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public int getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
