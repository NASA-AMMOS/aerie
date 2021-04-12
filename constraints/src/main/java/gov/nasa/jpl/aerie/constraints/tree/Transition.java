package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;

import java.util.Map;
import java.util.Objects;

public final class Transition implements Expression<Windows> {
  private final Expression<DiscreteProfile> profile;
  private final SerializedValue oldState;
  private final SerializedValue newState;

  public Transition(final Expression<DiscreteProfile> profile, final SerializedValue oldState, final SerializedValue newState) {
    this.profile = profile;
    this.oldState = oldState;
    this.newState = newState;
  }

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return this.profile.evaluate(results, environment).transitions(oldState, newState, results.bounds);
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
    if (!(obj instanceof Transition)) return false;
    final var o = (Transition)obj;

    return Objects.equals(this.profile, o.profile) &&
           Objects.equals(this.oldState, o.oldState) &&
           Objects.equals(this.newState, o.newState);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.profile, this.oldState, this.newState);
  }
}
