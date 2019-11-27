package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl.plan;

import com.google.gson.Gson;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.HttpHandler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanDetail;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;

import java.io.IOException;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap.parseToken;

/**
 * Command for updating a plan
  */
public class UpdatePlanCommand implements Command {

    private HttpHandler httpClient;
    private String planId;
    private int status;

    /* Plan detail to push */
    PlanDetail planDetail;

    public UpdatePlanCommand(HttpHandler httpClient, String planId, String[] tokens) throws InvalidTokenException {
        this.httpClient = httpClient;
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

        HttpPatch request = new HttpPatch(String.format("http://localhost:27183/api/plans/%s", this.planId));
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
