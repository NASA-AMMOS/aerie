package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

/**
 * Command to delete a plan
 */
public class DeletePlanCommand implements Command {

    private String planId;
    private int status;

    public DeletePlanCommand(String planId) {
        this.planId = planId;
        this.status = -1;
    }

    @Override
    public void execute() {
        HttpDelete request = new HttpDelete(String.format("http://localhost:27183/api/plans/%s", this.planId));

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
