package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpHandler;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Command to update a plan using a file
 * This is the only way to update the activity instance list from the CLI
 */
public class UpdatePlanFileCommand implements Command {

    private HttpHandler httpClient;
    private String planId;
    private String body;
    private int status;

    public UpdatePlanFileCommand(HttpHandler httpClient, String planId, String path) throws IOException {
        this.httpClient = httpClient;
        this.planId = planId;
        this.status = -1;

        this.body = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
    }

    @Override
    public void execute() {
        HttpPatch request = new HttpPatch(String.format("http://localhost:27183/api/plans/%s", this.planId));
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        try {
            request.setEntity(new StringEntity(this.body));

            HttpResponse response = httpClient.execute(request);

            this.status = response.getStatusLine().getStatusCode();

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public int getStatus() {
        return status;
    }
}
