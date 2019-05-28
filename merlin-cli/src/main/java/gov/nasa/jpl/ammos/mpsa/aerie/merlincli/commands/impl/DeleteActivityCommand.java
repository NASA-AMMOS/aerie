package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Command to delete an activity from a plan
 */
public class DeleteActivityCommand implements Command {

    private RestTemplate restTemplate;
    private String planId;
    private String activityId;
    private int status;

    public DeleteActivityCommand(RestTemplate restTemplate, String planId, String outName) {
        this.restTemplate = restTemplate;
        this.planId = planId;
        this.activityId = outName;
        this.status = -1;
    }

    @Override
    public void execute() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity requestBody = new HttpEntity(null, headers);
        try {
            String url = String.format("http://localhost:27183/api/plans/%s/activity_instances/%s", this.planId, this.activityId);
            ResponseEntity response = restTemplate.exchange(url, HttpMethod.DELETE, requestBody, String.class);
            this.status = response.getStatusCodeValue();
        }
        catch (HttpClientErrorException | HttpServerErrorException e) {
            this.status = e.getStatusCode().value();
        }
    }

    public int getStatus() {
        return status;
    }
}
