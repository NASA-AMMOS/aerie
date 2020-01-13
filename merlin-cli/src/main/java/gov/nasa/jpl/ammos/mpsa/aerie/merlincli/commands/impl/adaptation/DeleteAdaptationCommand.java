package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;

import java.io.IOException;

/**
 * Command to delete an adaptation
 */
public class DeleteAdaptationCommand implements Command {

    private HttpHandler httpClient;
    private String adaptationId;
    private int status;

    public DeleteAdaptationCommand(HttpHandler httpClient, String adaptationId) {
        this.httpClient = httpClient;
        this.adaptationId = adaptationId;
        this.status = -1;
    }

    @Override
    public void execute() {
        HttpDelete request = new HttpDelete(String.format("http://localhost:27182/api/adaptations/%s", this.adaptationId));

        try {
            HttpResponse response = this.httpClient.execute(request);

            this.status = response.getStatusLine().getStatusCode();

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public int getStatus() {
        return status;
    }
}

