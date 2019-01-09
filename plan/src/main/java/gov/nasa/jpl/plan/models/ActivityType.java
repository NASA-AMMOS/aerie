package gov.nasa.jpl.plan.models;


import java.util.ArrayList;

/*
Sample ActivityType from adaptation
{
    "InitialConditionActivity": {
        "listeners": [],
        "activityClass": "class gov.nasa.jpl.activity.InitialConditionActivity",
        "parameters": [
            {
                "name": "initialValues",
                "type": "java.util.Map<java.lang.String, java.lang.Comparable>"
            }
        ]
    }
}
*/

// Models an ActivityType which was fetched from the Adaptation Service
public class ActivityType {

    private String activityClass;
    private ArrayList<String> listeners;
    private ArrayList<ActivityTypeParameter> parameters;

    public ActivityType() {

    }

    public ActivityType(String activityClass, ArrayList<String> listeners, ArrayList<ActivityTypeParameter> parameters) {
        this.setActivityClass(activityClass);
        this.setListeners(listeners);
        this.setParameters(parameters);
    }

    public String getActivityClass() {
        return activityClass;
    }

    public void setActivityClass(String activityClass) {
        this.activityClass = activityClass;
    }

    public ArrayList<String> getListeners() {
        return listeners;
    }

    public void setListeners(ArrayList<String> listeners) {
        this.listeners = listeners;
    }

    public ArrayList<ActivityTypeParameter> getParameters() {
        return parameters;
    }

    public void setParameters(ArrayList<ActivityTypeParameter> parameters) {
        this.parameters = parameters;
    }
}
