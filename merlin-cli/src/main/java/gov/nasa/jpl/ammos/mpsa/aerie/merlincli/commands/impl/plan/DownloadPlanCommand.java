package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.nio.file.Path;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JSONUtilities.writeJson;

/**
 * Command to download a plan to a file
 */
public class DownloadPlanCommand implements Command {

    private HttpHandler httpClient;
    private String planId;
    private String outName;
    private int status;

    public DownloadPlanCommand(HttpHandler httpClient, String planId, String outName) {
        this.httpClient = httpClient;
        this.planId = planId;
        this.outName = outName;
        this.status = -1;
    }

    @Override
    public void execute() {
        HttpGet request = new HttpGet(String.format("http://localhost:27183/api/plans/%s", this.planId));

        try {
            HttpResponse response = this.httpClient.execute(request);

            this.status = response.getStatusLine().getStatusCode();

            if (status == 200 && response.getEntity() != null) {
                String responseString = new String(response.getEntity().getContent().readAllBytes());
                writeJson(responseString, Path.of(this.outName));
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public int getStatus() {
        return status;
    }
}
