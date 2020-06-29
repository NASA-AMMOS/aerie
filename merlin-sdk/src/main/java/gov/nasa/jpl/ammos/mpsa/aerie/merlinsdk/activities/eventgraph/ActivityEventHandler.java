package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;

public interface ActivityEventHandler<Result> {
    Result endActivity(String activityID);
    Result startActivity(String activityID, SerializedActivity activity);
}
