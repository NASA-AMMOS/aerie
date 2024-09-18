package gov.nasa.jpl.aerie.merlin.protocol.model.htn;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

public sealed interface TaskNetworkTemporalConstraint {
  record Meets(ActivityReference activity1, ActivityReference activity2) implements TaskNetworkTemporalConstraint{}
  static Meets meets(final ActivityReference activity1, final ActivityReference activity2){
    return new Meets(activity1, activity2);
  }
}
