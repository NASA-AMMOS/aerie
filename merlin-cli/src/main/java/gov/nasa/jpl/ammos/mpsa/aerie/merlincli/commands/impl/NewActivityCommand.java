package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.impl;

import com.google.gson.Gson;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.Command;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.commands.UndoableCommand;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.ActivityInstanceArray;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.PlanDetail;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.List;

public class NewActivityCommand implements Command, UndoableCommand {

    private ActivityInstanceArray activityInstances;
    private String activity;
    private String planId;

    public NewActivityCommand(String activity, String planId) {
        this.activity = activity;
        this.planId = planId;

    }

    public NewActivityCommand(String activity, String planId, ActivityInstanceArray activityInstances) {
        // Parameters are not added here. Instead, the adapter creates the activity instance and then passes it
        // as an argument to the constructor of the command to be executed.
        this.activity = activity;
        this.planId = planId;
        this.activityInstances = activityInstances;
    }

    @Override
    public void execute() {

        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target("http://localhost:27183/api/plans/" + planId);


        PlanDetail planDetail = new PlanDetail();
        planDetail.setId(this.planId);
        planDetail.setActivityInstances(activityInstances.getActivityInstances());

        String json = new Gson().toJson(planDetail, PlanDetail.class);
        Response response = target.request().put(Entity.entity(json, "application/json"));
        response.close();

        // First, get the plan being used (or container of activities)
//        if(activityType != null) {
//            // Execute decomposition (if any)
//            addActivitiesToPlan(activityType.executeDecomposition());
//        }
    }

    private void addActivitiesToPlan(List<ActivityInstanceArray> activityTypeList) {
        for (ActivityInstanceArray at : activityTypeList) {
            // TODO: Add logic to insert to the datastore to the correct plan.
        }
    }

    @Override
    public void undo() {
        // call the plan service, pop the latest operation from the stack and execute.
    }

    @Override
    public void redo() {

    }
}
