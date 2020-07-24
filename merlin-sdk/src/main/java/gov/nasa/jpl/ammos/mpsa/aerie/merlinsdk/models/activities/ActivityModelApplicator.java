package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Applicator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.List;

public class ActivityModelApplicator implements Applicator<List<ActivityEffect>, ActivityModel> {

    @Override
    public ActivityModel initial() {
        return new ActivityModel();
    }

    @Override
    public ActivityModel duplicate(final ActivityModel activityModel) {
        return new ActivityModel(activityModel);
    }

    @Override
    public void step(ActivityModel activityModel, Duration duration) {
        activityModel.step(duration);
    }

    @Override
    public void apply(ActivityModel activityModel, List<ActivityEffect> effects) {

        for (var effect : effects) {

            effect.visit(new ActivityEffect.VoidVisitor() {
                @Override
                public void addStart(String activityID, SerializedActivity activityType) {
                    activityModel.activityStart(activityID, activityType);
                }

                @Override
                public void addEnd(String activityID) {
                    activityModel.activityEnd(activityID);
                }

                @Override
                public void empty() {
                }
            });

        }
    }
}

