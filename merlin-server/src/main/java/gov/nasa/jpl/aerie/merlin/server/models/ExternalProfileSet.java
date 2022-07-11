package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.RealDynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.remotes.postgres.Window;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public final record ExternalProfileSet(
    Map<String, Pair<Window, List<Pair<Duration, RealDynamics>>>> realProfiles,
    Map<String, Pair<Window, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>>> discreteProfiles
) {
  public static ExternalProfileSet of(
      Map<String, Pair<Window, List<Pair<Duration, RealDynamics>>>> realProfiles,
      Map<String, Pair<Window, Pair<ValueSchema, List<Pair<Duration, SerializedValue>>>>> discreteProfiles
  ) {
    return new ExternalProfileSet(realProfiles, discreteProfiles);
  }
}
