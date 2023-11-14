package gov.nasa.jpl.aerie.merlin.protocol.model;

import java.time.Instant;

public interface TimeSystem<Epoch, Duration> {
  Epoch fromTai(Instant utcEpoch);
  Instant toTai(Epoch epoch);
  Duration fromDuration(gov.nasa.jpl.aerie.merlin.protocol.types.Duration duration);
  gov.nasa.jpl.aerie.merlin.protocol.types.Duration toDuration(Duration duration);
  String displayEpoch(Epoch epoch);
}
