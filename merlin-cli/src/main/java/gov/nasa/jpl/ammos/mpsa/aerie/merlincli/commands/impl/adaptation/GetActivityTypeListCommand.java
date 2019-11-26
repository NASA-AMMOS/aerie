package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JSONUtilities.prettify;

public class GetActivityTypeListCommand implements Command {

    private String adaptationId;
    private String responseBody;
    private int status;

    public GetActivityTypeListCommand(String adaptationId) {
        this.adaptationId = adaptationId;
        this.status = -1;
    }

    public void execute() {
        String url = String.format("http://localhost:27182/api/adaptations/%s/activities", this.adaptationId);
        HttpGet request = new HttpGet(url);

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(request);

            this.status = response.getStatusLine().getStatusCode();

            if (status == 200) {
                this.responseBody = prettify(response.getEntity().toString());
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
