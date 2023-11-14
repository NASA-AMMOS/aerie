package gov.nasa.jpl.aerie.contrib.time;

import gov.nasa.jpl.aerie.merlin.protocol.model.TimeSystem;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;

public record LmstSimple(SecondsStyle secondsStyle, Instant originTai, LMST originLmst) implements TimeSystem<LmstSimple.LMST, Duration> {
  static final double scalingFactor = 1.02749125;
  enum SecondsStyle {
    EARTH,
    MARS
  }

  public LmstSimple() {
    this(SecondsStyle.EARTH, Instant.EPOCH, new LMST(SecondsStyle.EARTH, 0, 0, 0, 0, 0));
  }

  public record LMST(SecondsStyle secondsStyle, int sols, int hours, int minutes, int seconds, int microseconds) {
    public LMST {
      if (sols < 0) throw new IllegalArgumentException("sols value out of bounds: " + sols);
      if (hours < 0) throw new IllegalArgumentException("hours value out of bounds: " + hours);
      if (minutes < 0) throw new IllegalArgumentException("minutes value out of bounds: " + minutes);
      if (seconds < 0) throw new IllegalArgumentException("seconds value out of bounds: " + seconds);
      if (microseconds < 0 || microseconds >= 1_000_000) throw new IllegalArgumentException("Microseconds value out of bounds: " + microseconds);
    }

    public static LMST ofEarthMicros(SecondsStyle secondsStyle, long earthMicros) {
      final var earthMicrosPerSol = (long) (scalingFactor * 24 * 60 * 60 * 1000 * 1000L);
      final var sols = earthMicros / earthMicrosPerSol;
      var remainingEarthMicros = earthMicros % earthMicrosPerSol;

      if (secondsStyle == SecondsStyle.EARTH) {
        final var earthHours = remainingEarthMicros / (60 * 60 * 1_000_000L);
        remainingEarthMicros -= earthHours * (60 * 60 * 1_000_000L);

        final var earthMinutes = remainingEarthMicros / (60 * 1_000_000L);
        remainingEarthMicros -= earthMinutes * (60 * 1_000_000L);

        final var earthSeconds = remainingEarthMicros / 1_000_000L;
        remainingEarthMicros -= earthSeconds * 1_000_000L;

        return new LMST(
            SecondsStyle.EARTH,
            (int) sols,
            (int) earthHours,
            (int) earthMinutes,
            (int) earthSeconds,
            (int) remainingEarthMicros);
      }

      if (secondsStyle == SecondsStyle.MARS) {
        var marsMicros = (long) (remainingEarthMicros / scalingFactor);
        final var marsHours = marsMicros / (60 * 60 * 1_000_000L);
        marsMicros -= marsHours * (60 * 60 * 1_000_000L);

        final var marsMinutes = marsMicros / (60 * 1_000_000L);
        marsMicros -= marsMinutes * (60 * 1_000_000L);

        final var marsSeconds = marsMicros / 1_000_000L;
        marsMicros -= marsSeconds * 1_000_000L;

        return new LMST(
            SecondsStyle.MARS,
            (int) sols,
            (int) marsHours,
            (int) marsMinutes,
            (int) marsSeconds,
            (int) marsMicros);
      }

      throw new Error("Unhandled secondsStyle " + secondsStyle);
    }

    public long toEarthMicros() {
      final var earthMicrosPerSol = (long) (scalingFactor * 24 * 60 * 60 * 1000 * 1000L);

      /*
      If EARTH style:
      - length of sol is 24:39:35.244000, which is 88775244000 microseconds
      - all other quantities are already correctly scaled
      */
      if (secondsStyle == SecondsStyle.EARTH) {
        return microseconds
        + (seconds * 1_000_000L)
        + (minutes * 60L * 1_000_000L)
        + (hours * 60L * 60L * 1_000_000L)
        + (sols * earthMicrosPerSol);
      }

      /*
      If MARS style:
      - length of sol is 24:00:00.000000, which is 86400000000 microseconds
      - all quantities need to be scaled
       */
      if (secondsStyle == SecondsStyle.MARS) {
        return ((long) ((microseconds
               + (seconds * 1_000_000L)
               + (minutes * 60L * 1_000_000L)
               + (hours * 60L * 60L * 1_000_000L)) * scalingFactor)
                + (sols * earthMicrosPerSol));
      }

      throw new Error("Unhandled secondsStyle " + secondsStyle);
    }

    @Override
    public String toString() {
      return String.format("Sol-%04dM%02d:%02d:%02d.%06d", sols, hours, minutes, seconds, microseconds);
    }
  }

  @Override
  public LMST fromTai(final Instant taiEpoch) {
    // TODO losing microsecond precision
    final var taiOriginMicros = originTai.toEpochMilli() / 1_000.0;
    final var taiEpochMicros = taiEpoch.toEpochMilli() / 1_000.0;
    return LMST.ofEarthMicros(secondsStyle, (long) (originLmst.toEarthMicros() + (taiEpochMicros - taiOriginMicros)));
  }

  @Override
  public Instant toTai(final LMST lmst) {
    return originTai.plusNanos(lmst.toEarthMicros() * 1000L);
  }

  @Override
  public Duration fromDuration(final Duration duration) {
    return duration;
  }

  @Override
  public Duration toDuration(final Duration duration) {
    return duration;
  }

  @Override
  public String displayEpoch(final LMST lmst) {
    return lmst.toString();
  }
}
