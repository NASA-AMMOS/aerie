package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;

public interface DefaultActivityEventHandler<Result> extends ActivityEventHandler<Result> {
    Result unhandled();

    @Override
    default Result endActivity(String activityID) { return  this.unhandled(); }

    @Override
    default Result startActivity(String activityID, SerializedActivity activity) { return  this.unhandled(); }
}
