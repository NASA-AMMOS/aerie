package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;

public abstract class ActivityEffect {

    public enum EffectType{
        START,
        END
    }

    public abstract ActivityEffect visit(Visitor visitor);
    public abstract String getActivityID();
    public abstract EffectType getEffectType();

    public interface Visitor {
        ActivityEffect addStart(String activityID, SerializedActivity activityName);
        ActivityEffect addEnd(String activityID);
        ActivityEffect empty();
    }

    public interface VoidVisitor {
        void addStart(String activityID, SerializedActivity activityName);
        void addEnd(String activityID);
        void empty();
    }

    public static ActivityEffect addStart(String activityID, SerializedActivity activityName) {
        return new ActivityEffect() {

            @Override
            public ActivityEffect visit(final Visitor visitor){
                return visitor.addStart(activityID, activityName);
            }

            @Override
            public String getActivityID() {
                return activityID;
            }

            @Override
            public EffectType getEffectType() {
                return EffectType.START;
            }
        };
    }

    public static ActivityEffect addEnd(String activityID) {
        return new ActivityEffect() {
            @Override
            public ActivityEffect visit(final Visitor visitor){
                return visitor.addEnd(activityID);
            }

            @Override
            public String getActivityID() {
                return activityID;
            }

            @Override
            public EffectType getEffectType() {
                return EffectType.END;
            }
        };
    }

    public final void visit(final VoidVisitor visitor){
        this.visit(new Visitor() {
            @Override
            public ActivityEffect addStart(String activityID, SerializedActivity activityName) {
                visitor.addStart(activityID, activityName);
                return null;
            }

            @Override
            public ActivityEffect addEnd(String activityID) {
                visitor.addEnd(activityID);
                return null;
            }

            @Override
            public ActivityEffect empty() {
                visitor.empty();
                return null;
            }
        });
    }
}
