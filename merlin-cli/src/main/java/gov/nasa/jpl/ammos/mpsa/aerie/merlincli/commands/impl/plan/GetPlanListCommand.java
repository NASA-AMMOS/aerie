package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpHandler;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JSONUtilities.prettify;

/**
 * Read the metadata of an adaptation
 */
public class GetPlanListCommand implements Command {

    private HttpHandler httpClient;
    private String responseBody;
    private int status;

    public GetPlanListCommand(HttpHandler httpClient) {
        this.httpClient = httpClient;
        this.status = -1;
    }

    @Override
    public void execute() {
        HttpGet request = new HttpGet("http://localhost:27183/api/plans");
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

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

    public int getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
