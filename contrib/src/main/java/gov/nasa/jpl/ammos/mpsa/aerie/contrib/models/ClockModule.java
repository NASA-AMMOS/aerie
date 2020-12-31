package gov.nasa.jpl.ammos.mpsa.aerie.contrib.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;

import java.time.Instant;

public final class ClockModule<$Schema> extends Module<$Schema> {
  private final LinearIntegrationModule<$Schema> ticks;
  private final Instant startTime;

  public ClockModule(
      final ResourcesBuilder.Cursor<$Schema> builder,
      final Instant startTime)
  {
    super(builder);
    this.startTime = startTime;
    this.ticks = new LinearIntegrationModule<>(builder, 0.0, 1000.0);
  }

  public double getElapsedMilliseconds() {
    return ticks.volume.get();
  }

  public Instant getTime() {
    final var ticksCount = ticks.volume.get();
    final var milli = (long) ticksCount;
    final var nano = (long) ((ticksCount - milli) * 1_000_000);

    return startTime.plusMillis(milli).plusNanos(nano);
  }
}
