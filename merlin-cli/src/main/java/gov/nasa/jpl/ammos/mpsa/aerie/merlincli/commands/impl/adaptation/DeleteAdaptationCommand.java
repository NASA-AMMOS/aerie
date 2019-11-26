package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

/**
 * Command to delete an adaptation
 */
public class DeleteAdaptationCommand implements Command {

    private String adaptationId;
    private int status;

    public DeleteAdaptationCommand(String adaptationId) {
        this.adaptationId = adaptationId;
        this.status = -1;
    }

    @Override
    public void execute() {
        HttpDelete request = new HttpDelete(String.format("http://localhost:27182/api/adaptations/%s", this.adaptationId));

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(request);

            this.status = response.getStatusLine().getStatusCode();

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public int getStatus() {
        return status;
    }
}

