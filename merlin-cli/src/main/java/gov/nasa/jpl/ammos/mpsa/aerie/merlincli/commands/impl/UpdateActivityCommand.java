package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl;

import com.google.gson.Gson;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityInstanceParameter;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap.getDoubleTokenValue;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap.parseToken;

/**
 * Command for updating an activity by specifying attributes and/or parameters
 * in a <name>=<value> format. Some attributes, such as activityId, may not be
 * modified.
 */
public class UpdateActivityCommand implements Command {

    private RestTemplate restTemplate;
    private String planId;
    private String activityId;
    private int status;

    /* Activity instance to be pushed */
    ActivityInstance updateInstance;

    public UpdateActivityCommand(RestTemplate restTemplate, String planId, String activityId, String[] tokens) throws InvalidTokenException {
        this.restTemplate = restTemplate;
        this.planId = planId;
        this.activityId = activityId;

        updateInstance = new ActivityInstance();
        List<ActivityInstanceParameter> parameters = new ArrayList<>();
        updateInstance.setParameters(parameters);

        for (String token : tokens) {

            // If parameter, then parse it and add it to the parameter list
            if (token.startsWith("param:")) {
                TokenMap tokenMap = parseToken(token.substring(token.indexOf(":") + 1));
                ActivityInstanceParameter param = new ActivityInstanceParameter();
                param.setName(tokenMap.getName());
                param.setValue(tokenMap.getValue());
                parameters.add(param);
            }
            // Otherwise it's an attribute, parse it and add it to the instance
            else {
                TokenMap tokenMap = parseToken(token);
                switch(tokenMap.getName()) {
                    case "start":
                        updateInstance.setStart(getDoubleTokenValue(tokenMap));
                        break;
                    case "startTimestamp":
                        updateInstance.setStartTimestamp(tokenMap.getValue());
                        break;
                    case "end":
                        updateInstance.setEnd(getDoubleTokenValue(tokenMap));
                        break;
                    case "endTimestamp":
                        updateInstance.setEndTimestamp(tokenMap.getValue());
                        break;
                    case "duration":
                        updateInstance.setDuration(getDoubleTokenValue(tokenMap));
                        break;
                    case "intent":
                        updateInstance.setIntent(tokenMap.getValue());
                        break;
                    case "name":
                        updateInstance.setName(tokenMap.getValue());
                        break;
                    case "textColor":
                        updateInstance.setTextColor(tokenMap.getValue());
                        break;
                    case "backgroundColor":
                        updateInstance.setBackgroundColor(tokenMap.getValue());
                        break;
                    case "y":
                        updateInstance.setY(getDoubleTokenValue(tokenMap));
                        break;
                    default:
                        throw new InvalidTokenException(token, String.format("'%s' is not a valid attribute", tokenMap.getName()));
                }
            }
        }
    }

    public void execute() {
        String body = new Gson().toJson(this.updateInstance, ActivityInstance.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity requestBody = new HttpEntity(body, headers);
        try {
            String url = String.format("http://localhost:27183/api/plans/%s/activity_instances/%s", this.planId, activityId);
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
