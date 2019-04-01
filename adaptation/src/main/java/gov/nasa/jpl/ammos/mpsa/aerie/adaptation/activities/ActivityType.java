package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.activities;

// TODO: DELETE ME
// TODO: Determine if activity is required here
//import gov.nasa.jpl.activity.Activity;

//import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ActivityType {

    private Class activityClass;
    private String typeName;
    private List<Map<String, String>> parameters;
    private List<PropertyChangeListener> listeners;

    public ActivityType(String typeName, Class activityClass, List<Map<String, String>> parameterNames) {
        this.typeName = typeName;
        this.activityClass = activityClass;
        this.parameters = parameterNames;
        listeners = new ArrayList<>();
    }

    public Class getActivityClass() {
        return activityClass;
    }

    public List<Map<String, String>> getParameters() {
        return parameters;
    }

    public List getListeners() {
        return listeners;
    }

    /*
    A definition of an Activity is imperative. For now, we will use the definition as implemented in
    Blackbird. For this reason, we have to add the dependency in the pom for the SDK.
     */
    // TODO: Determine if activity is required here
//    private void notifyListeners(String activityName, Activity act) {
//        for (PropertyChangeListener name : listeners) {
//            name.propertyChange(new PropertyChangeEvent(this, activityName, null, act));
//        }
//    }
}