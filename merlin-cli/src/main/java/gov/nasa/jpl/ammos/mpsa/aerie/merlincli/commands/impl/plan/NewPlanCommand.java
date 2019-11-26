package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Command for creating a new plan
 */
public class NewPlanCommand implements Command {

    private String path;
    private String body;
    private int status;
    private String id;

    public NewPlanCommand(String path) throws IOException {
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

            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(request);

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
