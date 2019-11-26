package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JSONUtilities.prettify;

/**
 * Command to get an activity from a plan
 */
public class GetActivityCommand implements Command {

    private String planId;
    private String activityId;
    private String responseBody;
    private int status;

    public GetActivityCommand(String planId, String activityId) {
        this.planId = planId;
        this.activityId = activityId;
        this.status = -1;
    }

    @Override
    public void execute() {
        HttpGet request = new HttpGet(String.format("http://localhost:27183/api/plans/%s/activity_instances/%s", this.planId, this.activityId));

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

    public int getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
