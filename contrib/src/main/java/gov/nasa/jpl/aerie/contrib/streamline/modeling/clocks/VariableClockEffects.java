package gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;

import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.effect;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.VariableClock.*;
import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

public final class VariableClockEffects {
  private VariableClockEffects() {}

  public static void pause(CellResource<VariableClock> stopwatch) {
    stopwatch.emit("Pause", effect(c -> pausedStopwatch(c.extract())));
  }

  public static void start(CellResource<VariableClock> stopwatch) {
    stopwatch.emit("Start", effect(c -> runningStopwatch(c.extract())));
  }

  public static void reset(CellResource<VariableClock> stopwatch) {
    stopwatch.emit("Reset", effect(c -> pausedStopwatch(ZERO)));
  }

  public static void restart(CellResource<VariableClock> stopwatch) {
    stopwatch.emit("Restart", effect(c -> runningStopwatch(ZERO)));
  }
}
