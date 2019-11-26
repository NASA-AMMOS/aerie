package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.nio.file.Path;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JSONUtilities.writeJson;

/**
 * Command to download a plan to a file
 */
public class DownloadPlanCommand implements Command {

    private String planId;
    private String outName;
    private int status;

    public DownloadPlanCommand(String planId, String outName) {
        this.planId = planId;
        this.outName = outName;
        this.status = -1;
    }

    @Override
    public void execute() {
        HttpGet request = new HttpGet(String.format("http://localhost:27183/api/plans/%s", this.planId));

        try {

            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(request);

            this.status = response.getStatusLine().getStatusCode();

            if (status == 200) {
                writeJson(response.getEntity().toString(), Path.of(this.outName));
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public int getStatus() {
        return status;
    }
}
