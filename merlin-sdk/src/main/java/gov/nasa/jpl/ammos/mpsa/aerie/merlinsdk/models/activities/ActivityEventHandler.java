package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;

public interface ActivityEventHandler<Result> {
    Result endActivity(String activityID);
    Result startActivity(String activityID, SerializedActivity activity);
}
