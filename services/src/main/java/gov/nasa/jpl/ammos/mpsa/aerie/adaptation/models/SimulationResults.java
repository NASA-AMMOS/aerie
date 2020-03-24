package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SimulationResults {
  public final List<Instant> timestamps;
  public final Map<String, List<SerializedParameter>> timelines;

  public SimulationResults(final List<Instant> timestamps, final Map<String, List<SerializedParameter>> timelines) {
    this.timestamps = timestamps;
    this.timelines = timelines;
  }
}
