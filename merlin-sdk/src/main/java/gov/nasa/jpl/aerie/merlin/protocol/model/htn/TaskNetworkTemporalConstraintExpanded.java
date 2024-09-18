package gov.nasa.jpl.aerie.merlin.protocol.model.htn;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public sealed interface TaskNetworkTemporalConstraintExpanded {
  record Before(ActivityReference activity1, ActivityReference activity2, Duration endToStartGap) implements
      TaskNetworkTemporalConstraintExpanded {}
  record Equals(ActivityReference activity1, ActivityReference activity2) implements
      TaskNetworkTemporalConstraintExpanded {}
  record Meets(ActivityReference activity1, ActivityReference activity2) implements
      TaskNetworkTemporalConstraintExpanded {}
  record Overlaps(ActivityReference activity1, ActivityReference activity2, Duration startToEndGap) implements
      TaskNetworkTemporalConstraintExpanded {}
  record Contains(ActivityReference activity1, ActivityReference activity2, Duration startToStartGap, Duration endToEndGap) implements
      TaskNetworkTemporalConstraintExpanded {}
  record Starts(ActivityReference activity1, ActivityReference activity2, Duration startToStartGap) implements
      TaskNetworkTemporalConstraintExpanded {}
  record FinishesAt(ActivityReference activity1, ActivityReference activity2) implements
      TaskNetworkTemporalConstraintExpanded {}
  record FinishesWithin(ActivityReference activity1, ActivityReference activity2, Duration endToEndGap) implements
      TaskNetworkTemporalConstraintExpanded {}

  static Before before(final ActivityReference activity1, final ActivityReference activity2,
                                             final Duration endToStartGap){
    return new Before(activity1, activity2, endToStartGap);
  }

  static Equals equals(final ActivityReference activity1, final ActivityReference activity2){
    return new Equals(activity1, activity2);
  }

  static Meets meets(final ActivityReference activity1, final ActivityReference activity2){
    return new Meets(activity1, activity2);
  }

  static Overlaps overlaps(final ActivityReference activity1, final ActivityReference activity2,
                                                 final Duration startToEndGap){
    return new Overlaps(activity1, activity2, startToEndGap);
  }

  static Contains contains(final ActivityReference activity1,
                                                    final ActivityReference activity2,
                                                 final Duration startToStartGap, final Duration endToEndGap){
    return new Contains(activity1, activity2, startToStartGap, endToEndGap);
  }

  static Starts starts(final ActivityReference activity1,
                                                    final ActivityReference activity2,
                                                 final Duration startToStartGap){
    return new Starts(activity1, activity2, startToStartGap);
  }

  static FinishesAt finishesAt(final ActivityReference activity1,
                                                     final ActivityReference activity2){
    return new FinishesAt(activity1, activity2);
  }

  static FinishesWithin finishesWithin(final ActivityReference activity1,
                                                             final ActivityReference activity2, final Duration endToEndGap){
    return new FinishesWithin(activity1, activity2, endToEndGap);
  }
}
