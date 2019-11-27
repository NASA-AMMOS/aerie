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
 * Command for creating a new plan
 */
public class NewPlanCommand implements Command {

    private HttpHandler httpClient;
    private String path;
    private String body;
    private int status;
    private String id;

    public NewPlanCommand(HttpHandler httpClient, String path) throws IOException {
        this.httpClient = httpClient;
        this.path = path;
        this.status = -1;

        this.body = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
    }

    @Override
    public void execute() {
        HttpPost request = new HttpPost("http://localhost:27183/api/plans");
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        try {
            request.setEntity(new StringEntity(this.body));

            HttpResponse response = this.httpClient.execute(request);

            this.status = response.getStatusLine().getStatusCode();

            if (status == 201) {
                this.id = response.getFirstHeader("location").toString();
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public int getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }
}
