package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.List;

public interface ActivityModelQuerier {
    List<String> getActivitiesOfType(String activityType);
    Window getCurrentInstanceWindow(String activityID);
    List<Window> getTypeWindows(String activityType);
}
