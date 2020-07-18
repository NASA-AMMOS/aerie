package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;

import java.util.List;

public final class ActivityEffectEvaluator extends ActivityEventProjection<List<ActivityEffect>> {

    public ActivityEffectEvaluator() {
        super(new ActivityEffectTrait());
    }

    @Override
    public List<ActivityEffect> endActivity(String activityID) {
        return List.of(ActivityEffect.addEnd(activityID));
    }

    @Override
    public List<ActivityEffect> startActivity(String activityID, SerializedActivity activity) {
        return List.of(ActivityEffect.addStart(activityID, activity));
    }
}
