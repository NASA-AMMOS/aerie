package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;

import java.util.ArrayList;
import java.util.List;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap.parseToken;

public class PlanDetail {
    private String _id;
    private String adaptationId;
    private String startTimestamp;
    private String name;
    private String endTimestamp;
    private List<ActivityInstance> activityInstances = new ArrayList<ActivityInstance>();

    public PlanDetail() {
    }

    public PlanDetail(String _id, String adaptationId, String endTimestamp, String name, String startTimestamp,
            ArrayList<ActivityInstance> activityInstances) {
        this._id = _id;
        this.adaptationId = adaptationId;
        this.startTimestamp = startTimestamp;
        this.name = name;
        this.endTimestamp = endTimestamp;
        this.setActivityInstances(activityInstances);
    }

    public static PlanDetail fromTokens(String[] tokens) throws InvalidTokenException {
        PlanDetail plan = new PlanDetail();

        for (String token : tokens) {
            TokenMap tokenMap = parseToken(token);
            switch(tokenMap.getName()) {
                case "adaptationId":
                    plan.setAdaptationId(tokenMap.getValue());
                    break;
                case "startTimestamp":
                    plan.setStartTimestamp(tokenMap.getValue());
                    break;
                case "endTimestamp":
                    plan.setEndTimestamp(tokenMap.getValue());
                    break;
                case "name":
                    plan.setName(tokenMap.getValue());
                    break;
                default:
                    throw new InvalidTokenException(token, String.format("'%s' is not a valid token key", tokenMap.getName()));
            }
        }

        return plan;
    }

    public List<ActivityInstance> getActivityInstances() {
        return activityInstances;
    }

    public void setActivityInstances(List<ActivityInstance> activityInstances) {
        this.activityInstances = activityInstances;
    }

    public String getID() {
        return this._id;
    }

    public void setID(String id) {
        this._id = id;
    }

    public String getAdaptationId() {
        return this.adaptationId;
    }

    public void setAdaptationId(String adaptationId) {
        this.adaptationId = adaptationId;
    }

    public String getStartTimestamp() {
        return this.startTimestamp;
    }

    public void setStartTimestamp(String startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndTimestamp() {
        return this.endTimestamp;
    }

    public void setEndTimestamp(String endTimestamp) {
        this.endTimestamp = endTimestamp;
    }
}
