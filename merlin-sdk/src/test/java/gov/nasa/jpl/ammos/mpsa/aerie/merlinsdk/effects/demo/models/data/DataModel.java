package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.models.data;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DataModel {
  private final Map<String, DataBin> bins;

  public DataModel() {
    this.bins = new HashMap<>();
  }

  public DataModel(final DataModel other) {
    this.bins = new HashMap<>(other.bins.size());
    for (final var entry : other.bins.entrySet()) {
      final var key = Objects.requireNonNull(entry.getKey());
      final var initialValues = Objects.requireNonNull(entry.getValue());

      this.bins.put(key, new DataBin(initialValues));
    }
  }

  public void step(final Duration duration) {
    for (final var bin : this.bins.values()) bin.step(duration);

    // TODO: check bins for overflow and move their overflow volume around.
  }

  public DataBin getDataBin(final String name) {
    return this.bins.computeIfAbsent(name, k -> new DataBin());
  }

  @Override
  public String toString() {
    return this.bins.toString();
  }
}
