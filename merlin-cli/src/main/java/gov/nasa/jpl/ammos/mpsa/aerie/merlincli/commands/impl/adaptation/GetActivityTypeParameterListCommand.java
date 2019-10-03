package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.adaptation;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JSONUtilities.prettify;

public class GetActivityTypeParameterListCommand implements Command {

    private RestTemplate restTemplate;
    private String adaptationId;
    private String activityTypeId;
    private String responseBody;
    private int status;

    public GetActivityTypeParameterListCommand(RestTemplate restTemplate, String adaptationId, String activityTypeId) {
        this.restTemplate = restTemplate;
        this.adaptationId = adaptationId;
        this.activityTypeId = activityTypeId;
        this.status = -1;
    }

    public void execute() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity requestBody = new HttpEntity(null, headers);
        try {
            String url = String.format("http://localhost:27182/api/adaptations/%s/activities/%s/parameters", this.adaptationId, this.activityTypeId);
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

    public int getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
