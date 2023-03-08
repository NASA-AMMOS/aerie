package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.server.models.Timestamp;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.MICROSECONDS;

public record PlanRecord(
    long id,
    long revision,
    String name,
    long missionModelId,
    Timestamp startTime,
    Timestamp endTime
) {
  Duration duration() {
    return Duration.of(startTime.microsUntil(endTime), MICROSECONDS);
  }
}
