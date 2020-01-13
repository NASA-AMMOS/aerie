package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;

import java.io.IOException;

/**
 * Command to delete an activity from a plan
 */
public class DeleteActivityCommand implements Command {

    private HttpHandler httpClient;
    private String planId;
    private String activityId;
    private int status;

    public DeleteActivityCommand(HttpHandler httpClient, String planId, String outName) {
        this.httpClient = httpClient;
        this.planId = planId;
        this.activityId = outName;
        this.status = -1;
    }

    @Override
    public void execute() {
        String url = String.format("http://localhost:27183/api/plans/%s/activity_instances/%s", this.planId, this.activityId);
        HttpDelete request = new HttpDelete(url);

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
