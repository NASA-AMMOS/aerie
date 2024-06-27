package gov.nasa.jpl.aerie.merlin.protocol.model.htn;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public sealed interface TaskNetworkTemporalConstraint {
  record Before(ActivityReference activity1, ActivityReference activity2, Duration endToStartGap) implements TaskNetworkTemporalConstraint{}
  record Equals(ActivityReference activity1, ActivityReference activity2) implements TaskNetworkTemporalConstraint{}
  record Meets(ActivityReference activity1, ActivityReference activity2) implements TaskNetworkTemporalConstraint{}
  record Overlaps(ActivityReference activity1, ActivityReference activity2, Duration startToEndGap) implements TaskNetworkTemporalConstraint{}
  record Contains(ActivityReference activity1, ActivityReference activity2, Duration startToStartGap, Duration endToEndGap) implements TaskNetworkTemporalConstraint{}
  record Starts(ActivityReference activity1, ActivityReference activity2, Duration startToStartGap) implements TaskNetworkTemporalConstraint{}
  record FinishesAt(ActivityReference activity1, ActivityReference activity2) implements TaskNetworkTemporalConstraint{}
  record FinishesWithin(ActivityReference activity1, ActivityReference activity2, Duration endToEndGap) implements TaskNetworkTemporalConstraint{}

  static TaskNetworkTemporalConstraint.Before before(final ActivityReference activity1, final ActivityReference activity2,
                                             final Duration endToStartGap){
    return new TaskNetworkTemporalConstraint.Before(activity1, activity2, endToStartGap);
  }

  static TaskNetworkTemporalConstraint.Equals equals(final ActivityReference activity1, final ActivityReference activity2){
    return new TaskNetworkTemporalConstraint.Equals(activity1, activity2);
  }

  static TaskNetworkTemporalConstraint.Meets meets(final ActivityReference activity1, final ActivityReference activity2){
    return new TaskNetworkTemporalConstraint.Meets(activity1, activity2);
  }

  static TaskNetworkTemporalConstraint.Overlaps overlaps(final ActivityReference activity1, final ActivityReference activity2,
                                                 final Duration startToEndGap){
    return new TaskNetworkTemporalConstraint.Overlaps(activity1, activity2, startToEndGap);
  }

  static TaskNetworkTemporalConstraint.Contains contains(final ActivityReference activity1,
                                                    final ActivityReference activity2,
                                                 final Duration startToStartGap, final Duration endToEndGap){
    return new TaskNetworkTemporalConstraint.Contains(activity1, activity2, startToStartGap, endToEndGap);
  }

  static TaskNetworkTemporalConstraint.Starts starts(final ActivityReference activity1,
                                                    final ActivityReference activity2,
                                                 final Duration startToStartGap){
    return new TaskNetworkTemporalConstraint.Starts(activity1, activity2, startToStartGap);
  }

  static TaskNetworkTemporalConstraint.FinishesAt finishesAt(final ActivityReference activity1,
                                                     final ActivityReference activity2){
    return new TaskNetworkTemporalConstraint.FinishesAt(activity1, activity2);
  }

  static TaskNetworkTemporalConstraint.FinishesWithin finishesWithin(final ActivityReference activity1,
                                                             final ActivityReference activity2, final Duration endToEndGap){
    return new TaskNetworkTemporalConstraint.FinishesWithin(activity1, activity2, endToEndGap);
  }
}
