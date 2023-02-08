package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Objects;
import java.util.Set;

public final class ShiftBy implements Expression<Windows> {
  public final Expression<Windows> windows;
  public final Expression<Duration> fromStart;
  public final Expression<Duration> fromEnd;

  public ShiftBy(final Expression<Windows> left, final Expression<Duration> fromStart, final Expression<Duration> fromEnd) {
    this.windows = left;
    this.fromStart = fromStart;
    this.fromEnd = fromEnd;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    final var windows = this.windows.evaluate(results, bounds, environment);
    return windows.shiftBy(this.fromStart.evaluate(results, bounds, environment), this.fromEnd.evaluate(results, bounds, environment));
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.windows.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(shift %s by %s %s)",
        prefix,
        this.windows.prettyPrint(prefix + "  "),
        this.fromStart.toString(),
        this.fromEnd.toString()
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ShiftBy)) return false;
    final var o = (ShiftBy)obj;

    return Objects.equals(this.windows, o.windows) &&
           Objects.equals(this.fromStart, o.fromStart) &&
           Objects.equals(this.fromEnd, o.fromEnd);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.windows, this.fromStart, this.fromEnd);
  }
}
