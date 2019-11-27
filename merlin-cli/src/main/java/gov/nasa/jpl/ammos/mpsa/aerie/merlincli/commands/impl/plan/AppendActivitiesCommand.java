package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpHandler;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Command to create a new activity based on a JSON file
 */
public class AppendActivitiesCommand implements Command {

    private HttpHandler httpClient;
    private String planId;
    private String body;
    private int status;

    public AppendActivitiesCommand(HttpHandler httpClient, String planId, String path) throws IOException {
        this.httpClient = httpClient;
        this.planId = planId;
        this.status = -1;

        this.body = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
    }

    @Override
    public void execute() {
        HttpPost request = new HttpPost(String.format("http://localhost:27183/api/plans/%s/activity_instances", this.planId));
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        try {
            request.setEntity(new StringEntity(this.body));

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