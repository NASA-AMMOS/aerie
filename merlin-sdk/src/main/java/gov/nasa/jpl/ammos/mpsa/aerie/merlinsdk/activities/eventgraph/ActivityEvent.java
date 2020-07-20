package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;

import java.util.Objects;

public abstract class ActivityEvent {
    private ActivityEvent() {}

    public abstract <Result> Result visit(ActivityEventHandler<Result> visitor);

    public static ActivityEvent startActivity(final String activityID, final SerializedActivity activity) {
        Objects.requireNonNull(activityID);
        Objects.requireNonNull(activity);
        return new ActivityEvent() {
            @Override
            public <Result> Result visit(ActivityEventHandler<Result> visitor) {
                return visitor.startActivity(activityID, activity);
            }
        };
    }

    public static ActivityEvent endActivity(final String activityID) {
        Objects.requireNonNull(activityID);
        return new ActivityEvent() {
            @Override
            public <Result> Result visit(ActivityEventHandler<Result> visitor) {
                return visitor.endActivity(activityID);
            }
        };
    }

    @Override
    public final String toString() {
        return this.visit(new ActivityEventHandler<>() {

            @Override
            public String startActivity(String activityID, SerializedActivity activity) {
                return String.format("start(%s, %s)",activityID,activity);
            }

            @Override
            public String endActivity(String activityID) {
                return  String.format("end(%s)",activityID);
            }
        });
    }
}
