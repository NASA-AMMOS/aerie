package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;

import java.io.IOException;

/**
 * Command to delete a plan
 */
public class DeletePlanCommand implements Command {

    private HttpHandler httpClient;
    private String planId;
    private int status;

    public DeletePlanCommand(HttpHandler httpClient, String planId) {
        this.httpClient = httpClient;
        this.planId = planId;
        this.status = -1;
    }

    @Override
    public void execute() {
        HttpDelete request = new HttpDelete(String.format("http://localhost:27183/api/plans/%s", this.planId));

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
