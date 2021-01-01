package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.resources.real;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Approximator;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.DelimitedDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.time.Window;
import gov.nasa.jpl.ammos.mpsa.aerie.time.Windows;

import java.util.List;
import java.util.Optional;

public final class RealResourceSolver<$Schema>
    implements ResourceSolver<$Schema, RealResource<$Schema>, RealDynamics, RealCondition>
{
  @Override
  public DelimitedDynamics<RealDynamics> getDynamics(
      final RealResource<$Schema> resource,
      final History<? extends $Schema> now)
  {
    return resource.getDynamics(now);
  }

  @Override
  public Approximator<RealDynamics> getApproximator() {
    return Approximator.real(dynamics -> List.of(DelimitedDynamics.persistent(dynamics)));
  }

  @Override
  public Optional<Duration>
  firstSatisfied(final RealDynamics dynamics, final RealCondition condition, final Window selection) {
    for (final var window : this.whenSatisfied(dynamics, condition, selection)) {
      return Optional.of(window.start);
    }

    return Optional.empty();
  }


  public Windows whenSatisfied(final RealDynamics dynamics, final RealCondition condition, final Window selection) {
    final var windows = new Windows();

    if (dynamics.rate == 0) {
      windows.add((condition.includesPoint(dynamics.initial)) ? selection : Window.EMPTY);
    } else if (dynamics.rate > 0) {
      for (final var interval : condition.ascendingOrder()) {
        // It starts too high (and it's going higher); no intersection.
        if (dynamics.initial > interval.max) continue;

        final var start = (interval.min - dynamics.initial) / dynamics.rate;
        final var end = (interval.max - dynamics.initial) / dynamics.rate;
        final var window = Window.roundIn(start, end, Duration.SECONDS);

        if (!window.overlaps(selection)) continue;

        windows.add(window);
      }
    } else {
      for (final var interval : condition.descendingOrder()) {
        // It starts too low (and it's going lower); no intersection.
        if (dynamics.initial < interval.min) continue;

        final var start = (interval.max - dynamics.initial) / dynamics.rate;
        final var end = (interval.min - dynamics.initial) / dynamics.rate;
        final var window = Window.roundIn(start, end, Duration.SECONDS);

        if (!window.overlaps(selection)) continue;

        windows.add(window);
      }
    }

    return windows;
  }
}
