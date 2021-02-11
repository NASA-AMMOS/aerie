package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.SimulationTimeline;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;
import gov.nasa.jpl.aerie.time.Windows;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;

public final class ConditionSolver<$Schema, $Timeline extends $Schema>
    implements Condition.Visitor<$Schema, Windows>
{
  private final List<Pair<Duration, History<$Timeline>>> trace;
  private final Window planWindow;

  public ConditionSolver(
      final List<Pair<Duration, History<$Timeline>>> trace,
      final Window planWindow)
  {
    this.trace = Objects.requireNonNull(trace);
    this.planWindow = Objects.requireNonNull(planWindow);
  }

  @Override
  public <R, D, C> Windows atom(final ResourceSolver<$Schema, R, D, C> solver, final R resource, final C condition) {
    final var profile = SimulationDriver.computeProfile(this.trace, solver, resource);

    return WindowAccumulator.solve(this.planWindow, solver, condition, profile);
  }

  @Override
  public Windows not(final Windows x) {
    final var result = new Windows(this.planWindow);
    result.subtractAll(x);
    return result;
  }

  @Override
  public Windows and(final Windows x, final Windows y) {
    final var result = new Windows(x);
    result.intersectWith(y);
    return result;
  }

  @Override
  public Windows or(final Windows x, final Windows y) {
    final var result = new Windows(x);
    result.addAll(y);
    return result;
  }
}
