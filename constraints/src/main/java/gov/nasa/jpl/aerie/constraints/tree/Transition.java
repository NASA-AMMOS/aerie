package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record Transition(
    DiscreteResource profile,
    SerializedValue oldState,
    SerializedValue newState) implements WindowsExpression {

  @Override
  public Windows evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    return this.profile.evaluate(results, bounds, environment).transitions(oldState, newState, bounds);
  }

  @Override
  public void extractResources(final Set<String> names) {
    this.profile.extractResources(names);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(transition %s\n%s  %s\n%s  %s)",
        prefix,
        this.profile.prettyPrint(prefix + "  "),
        prefix,
        oldState,
        prefix,
        newState
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof final Transition o)) return false;

    return Objects.equals(this.profile, o.profile) &&
           Objects.equals(this.oldState, o.oldState) &&
           Objects.equals(this.newState, o.newState);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.profile, this.oldState, this.newState);
  }
}
