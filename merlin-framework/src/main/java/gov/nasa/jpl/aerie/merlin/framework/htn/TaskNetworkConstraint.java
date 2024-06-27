package gov.nasa.jpl.aerie.merlin.framework.htn;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public sealed interface TaskNetworkConstraint {
  record Precedes(ActivityReference activity1, ActivityReference activity2) implements TaskNetworkConstraint{}
  record Before(ActivityReference activity1, ActivityReference activity2, Duration howMuch) implements TaskNetworkConstraint{}

  record EqualPrecondition<T>(String nameResource, T value) implements TaskNetworkConstraint {}

  static Precedes precedes(final ActivityReference activity1, final ActivityReference activity2){
    return new Precedes(activity1, activity2);
  }

  static Before before(final ActivityReference activity1, final ActivityReference activity2, final Duration howMuch){
    return new Before(activity1, activity2, howMuch);
  }
}
