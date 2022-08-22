package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public final record ProfileSet(
    Map<String, Pair<ValueSchema, List<Pair<Duration, RealDynamics>>>> realProfiles,
    Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles
) {
  public static ProfileSet of(
      final Map<String, Pair<ValueSchema, List<Pair<Duration, RealDynamics>>>> realProfiles,
      final Map<String, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>> discreteProfiles
  ) {
    return new ProfileSet(realProfiles, discreteProfiles);
  }
}
