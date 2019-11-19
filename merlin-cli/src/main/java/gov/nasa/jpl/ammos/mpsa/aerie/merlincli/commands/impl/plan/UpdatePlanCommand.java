package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import com.google.gson.Gson;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap.parseToken;

/**
 * Command for updating a plan
  */
public class UpdatePlanCommand implements Command {

    private RestTemplate restTemplate;
    private String planId;
    private int status;

    /* Plan detail to push */
    PlanDetail planDetail;

    public UpdatePlanCommand(RestTemplate restTemplate, String planId, String[] tokens) throws InvalidTokenException {
        this.restTemplate = restTemplate;
        this.planId = planId;

        planDetail = new PlanDetail();
        planDetail.setActivityInstances(null);

        for (String token : tokens) {
            TokenMap tokenMap = parseToken(token);
            switch(tokenMap.getName()) {
                case "adaptationId":
                    planDetail.setAdaptationId(tokenMap.getValue());
                    break;
                case "startTimestamp":
                    planDetail.setStartTimestamp(tokenMap.getValue());
                    break;
                case "endTimestamp":
                    planDetail.setEndTimestamp(tokenMap.getValue());
                    break;
                default:
                    throw new InvalidTokenException(token, String.format("'%s' is not a valid attribute", tokenMap.getName()));
            }
        }
    }

    @Override
    public void execute() {
        String body = new Gson().toJson(planDetail, PlanDetail.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity requestBody = new HttpEntity(body, headers);
        try {
            String url = String.format("http://localhost:27183/api/plans/%s", this.planId);
            ResponseEntity response = restTemplate.exchange(url, HttpMethod.PATCH, requestBody, String.class);
            this.status = response.getStatusCodeValue();
        }
        catch (HttpClientErrorException | HttpServerErrorException e) {
            this.status = e.getStatusCode().value();
        }
    }

    public int getStatus() {
        return this.status;
    }
}
