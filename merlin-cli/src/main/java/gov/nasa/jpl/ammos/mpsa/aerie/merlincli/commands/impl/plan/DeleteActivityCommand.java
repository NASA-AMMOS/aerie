package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

/**
 * Command to delete an activity from a plan
 */
public class DeleteActivityCommand implements Command {

    private String planId;
    private String activityId;
    private int status;

    public DeleteActivityCommand(String planId, String outName) {
        this.planId = planId;
        this.activityId = outName;
        this.status = -1;
    }

    @Override
    public void execute() {
        String url = String.format("http://localhost:27183/api/plans/%s/activity_instances/%s", this.planId, this.activityId);
        HttpDelete request = new HttpDelete(url);

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
