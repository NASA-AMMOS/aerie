package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JSONUtilities.writeJson;

/**
 * Command to download a plan to a file
 */
public class DownloadPlanCommand implements Command {

    private RestTemplate restTemplate;

    private String planId;
    private String outName;
    private int status;

    public DownloadPlanCommand(RestTemplate restTemplate, String planId, String outName) {
        this.restTemplate = restTemplate;
        this.planId = planId;
        this.outName = outName;
        this.status = -1;
    }

    @Override
    public void execute() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity requestBody = new HttpEntity(null, headers);
        try {
            String url = String.format("http://localhost:27183/api/plans/%s", this.planId);
            ResponseEntity response = restTemplate.exchange(url, HttpMethod.GET, requestBody, String.class);
            this.status = response.getStatusCode().value();

            if (status == 200) {
                writeJson(response.getBody().toString(), this.outName);
            }
        }
        catch (HttpClientErrorException | HttpServerErrorException e) {
            this.status = e.getStatusCode().value();
        }
    }

    public int getStatus() {
        return status;
    }
}
