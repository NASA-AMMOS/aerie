package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.time.Duration;

import java.util.Optional;

public interface Condition<$Schema> {
  Optional<Duration> nextSatisfied(final Checkpoint<? extends $Schema> now, final Duration atLatest);
}
