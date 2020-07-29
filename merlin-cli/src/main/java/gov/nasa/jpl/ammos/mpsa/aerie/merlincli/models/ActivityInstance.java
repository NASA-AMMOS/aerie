package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.InvalidTokenException;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedValue;

import java.util.Map;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.TokenMap.parseToken;

public class ActivityInstance {

    private String activityType;
    private String startTimestamp;
    private String name;
    private Map<String, SerializedValue> parameters;

    public ActivityInstance() {
        this.activityType = null;
        this.startTimestamp = null;
        this.name = null;
        this.parameters = null;
    }

    public ActivityInstance(String activityType, String startTimestamp, String name) {
        this.activityType = activityType;
        this.startTimestamp = startTimestamp;
        this.name = name;
    }

    public static ActivityInstance fromTokens(String[] tokens) throws InvalidTokenException {
        ActivityInstance activityInstance = new ActivityInstance();

        for (String token : tokens) {
            TokenMap tokenMap = parseToken(token);
            switch(tokenMap.getName()) {
                case "startTimestamp":
                    activityInstance.setStartTimestamp(tokenMap.getValue());
                    break;
                case "name":
                    activityInstance.setName(tokenMap.getValue());
                    break;
                default:
                    throw new InvalidTokenException(token, String.format("'%s' is not a valid attribute", tokenMap.getName()));
            }
        }

        return activityInstance;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setStartTimestamp(String startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public String getStartTimestamp() {
        return startTimestamp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setParameters(Map<String, SerializedValue> parameters) {
        this.parameters = parameters;
    }

    public Map<String, SerializedValue> getParameters() {
        return Map.copyOf(this.parameters);
    }
}
