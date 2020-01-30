package gov.nasa.jpl.ammos.mpsa.aerie.simulation.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;

import java.util.List;

public final class SimulationResults {
  public final List<Instant> timestamps;
  public final List<List<SerializedParameter>> timelines;

  public SimulationResults(final List<Instant> timestamps, final List<List<SerializedParameter>> timelines) {
    this.timestamps = timestamps;
    this.timelines = timelines;
  }
}
