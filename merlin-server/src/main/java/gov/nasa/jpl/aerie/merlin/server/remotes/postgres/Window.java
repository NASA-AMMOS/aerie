package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;

public record Window(Timestamp start, Timestamp end) {
  public Duration duration() {
    return Duration.of(start.microsUntil(end), MICROSECONDS);
  }
}
