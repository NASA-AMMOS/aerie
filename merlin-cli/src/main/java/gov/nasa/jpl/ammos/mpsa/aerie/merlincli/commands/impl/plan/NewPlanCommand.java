package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Command for creating a new plan
 */
public class NewPlanCommand implements Command {

    private RestTemplate restTemplate;
    private String path;
    private String body;
    private int status;
    private String id;

    public NewPlanCommand(RestTemplate restTemplate, String path) throws IOException {
        this.restTemplate = restTemplate;
        this.path = path;
        this.status = -1;

        this.body = new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
    }

    @Override
    public void execute() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity requestBody = new HttpEntity(this.body, headers);
        try {
            ResponseEntity response = restTemplate.exchange("http://localhost:27183/api/plans", HttpMethod.POST, requestBody, String.class);
            this.status = response.getStatusCodeValue();
            this.id = response.getHeaders().getFirst("location");

        }
        catch (HttpClientErrorException | HttpServerErrorException e) {
            this.status = e.getStatusCode().value();
        }
    }

    public int getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }
}
