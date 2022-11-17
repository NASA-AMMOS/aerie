package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.profile.Profile;
import gov.nasa.jpl.aerie.constraints.profile.Windows;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.Set;

public record ShiftBy(
    Expression<Profile<Boolean>> windows,
    Duration fromStart,
    Duration fromEnd) implements Expression<Windows> {

  @Override
  public Windows evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment) {
    final var windows = (Windows) this.windows.evaluate(results, bounds, environment);
    return windows.shiftEdges(this.fromStart, this.fromEnd);
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
}
