package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Command to get an activity from a plan
 */
public class GetActivityCommand implements Command {

    private RestTemplate restTemplate;
    private String planId;
    private String activityId;
    private String responseBody;
    private int status;

    public GetActivityCommand(RestTemplate restTemplate, String planId, String activityId) {
        this.restTemplate = restTemplate;
        this.planId = planId;
        this.activityId = activityId;
        this.status = -1;
    }

    @Override
    public void execute() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity requestBody = new HttpEntity(null, headers);
        try {
            String url = String.format("http://localhost:27183/api/plans/%s/activity_instances/%s", this.planId, this.activityId);
            ResponseEntity response = restTemplate.exchange(url, HttpMethod.GET, requestBody, String.class);
            this.status = response.getStatusCodeValue();

            if (status == 200) {
                this.responseBody = prettify(response.getBody().toString());
            }

        }
        catch (HttpClientErrorException | HttpServerErrorException e) {
            this.status = e.getStatusCode().value();
        }
    }

    // TODO: This function is defined in multiple commands, it should be moved to a separate file commands include.
    private String prettify(String json) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(json);
        return gson.toJson(je);
    }

    public int getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
