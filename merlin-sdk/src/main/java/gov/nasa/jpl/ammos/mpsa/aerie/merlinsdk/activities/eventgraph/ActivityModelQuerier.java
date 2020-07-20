package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

import java.util.List;

public interface ActivityModelQuerier {
    List<String> getActivitiesOfType(String activityType);
    Window getCurrentInstanceWindow(String activityID);
    Windows getTypeWindows(String activityType);
}
