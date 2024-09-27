package gov.nasa.ammos.aerie.simulation.protocol;

import gov.nasa.jpl.aerie.merlin.protocol.model.ModelType;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.util.function.Supplier;

public interface Simulator {
  default Results simulate(Schedule schedule) {
    return simulate(schedule, () -> false);
  }
  Results simulate(Schedule schedule, Supplier<Boolean> isCancelled);

  interface Factory {
    <Config, Model> Simulator create(ModelType<Config, Model> modelType, Config config, Instant startTime, Duration duration);
  }
}
