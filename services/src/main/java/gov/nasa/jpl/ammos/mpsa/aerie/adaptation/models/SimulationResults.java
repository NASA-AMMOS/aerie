package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.List;
import java.util.Map;

public final class SimulationResults {
  public final List<Duration> timestamps;
  public final Map<String, List<SerializedParameter>> timelines;

  public SimulationResults(final List<Duration> timestamps, final Map<String, List<SerializedParameter>> timelines) {
    this.timestamps = timestamps;
    this.timelines = timelines;
  }
}
