package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl;

import com.google.gson.Gson;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstanceArray;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanDetail;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

public class UpdatePlanCommand implements Command {

    private ActivityInstanceArray activityInstances;
    private String planId;
    private String startTimestamp;
    private String endTimestamp;
    private String adaptationId;

    public UpdatePlanCommand(String planId, String startTimestamp, String endTimestamp, String adaptationId, ActivityInstanceArray instances) {
        this.planId = planId;
        this.adaptationId = adaptationId;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.activityInstances = instances;
    }

    @Override
    public void execute() {
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target("http://localhost:27183/api/plans/" + planId);

        PlanDetail planDetail = new PlanDetail();
        planDetail.setId(this.planId);
        planDetail.setAdaptationId(adaptationId);
        planDetail.setStartTimestamp(startTimestamp);
        planDetail.setEndTimestamp(endTimestamp);
        planDetail.setActivityInstances(activityInstances.getActivityInstances());

        String json = new Gson().toJson(planDetail, PlanDetail.class);
        Response response = target.request().put(Entity.entity(json, "application/json"));
        response.close();
    }
}
