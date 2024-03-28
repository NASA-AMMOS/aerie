package gov.nasa.jpl.aerie.merlin.framework.dependency;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public sealed interface TemporalDependency {
  enum ActivityTimepointBound { ActivityStart, ActivityEnd };
  record ActivityTimepoint(ActivityTimepointBound activityTimepoint, Duration offset) implements TemporalDependency {}
  record ActivityTimepointInterval(ActivityTimepoint start, ActivityTimepoint end) implements TemporalDependency {}

  static TemporalDependency duringActivity(){
    return new ActivityTimepointInterval(atStart(), atEnd());
  }

  static TemporalDependency after(final ActivityTimepoint activityDependentTimepoint){
    return new ActivityTimepointInterval(activityDependentTimepoint, atEnd());
  }

  static TemporalDependency before(final ActivityTimepoint activityDependentTimepoint){
    return new ActivityTimepointInterval(atStart(), activityDependentTimepoint);
  }

  static ActivityTimepoint atStart(){
    return new ActivityTimepoint(ActivityTimepointBound.ActivityStart, Duration.ZERO);
  }

  static ActivityTimepoint atEnd(){
    return new ActivityTimepoint(ActivityTimepointBound.ActivityEnd, Duration.ZERO);
  }

  static ActivityTimepoint offsetAfterStart(final Duration fixed){
      return new ActivityTimepoint(ActivityTimepointBound.ActivityStart, fixed);
  }

  static ActivityTimepoint offsetBeforeEnd(final Duration fixed){
    return new ActivityTimepoint(ActivityTimepointBound.ActivityEnd, fixed);
  }
}
