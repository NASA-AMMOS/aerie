package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import com.google.gson.Gson;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpHandler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstance;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstanceParameter;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap.parseToken;

/**
 * Command for updating an activity by specifying attributes and/or parameters
 * in a <name>=<value> format. Some attributes, such as activityId, may not be
 * modified.
 */
public class UpdateActivityCommand implements Command {

    private HttpHandler httpClient;
    private String planId;
    private String activityId;
    private int status;

    /* Activity instance to be pushed */
    ActivityInstance updateInstance;

    public UpdateActivityCommand(HttpHandler httpClient, String planId, String activityId, String[] tokens) throws InvalidTokenException {
        this.httpClient = httpClient;
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
                    case "startTimestamp":
                        updateInstance.setStartTimestamp(tokenMap.getValue());
                        break;
                    case "name":
                        updateInstance.setName(tokenMap.getValue());
                        break;
                    default:
                        throw new InvalidTokenException(token, String.format("'%s' is not a valid attribute", tokenMap.getName()));
                }
            }
        }
    }

    public void execute() {
        String body = new Gson().toJson(this.updateInstance, ActivityInstance.class);

        String url = String.format("http://localhost:27183/api/plans/%s/activity_instances/%s", this.planId, activityId);
        HttpPatch request = new HttpPatch(url);
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        try {
            request.setEntity(new StringEntity(body));

            HttpResponse response = this.httpClient.execute(request);

            this.status = response.getStatusLine().getStatusCode();

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public int getStatus() {
        return this.status;
    }
}
