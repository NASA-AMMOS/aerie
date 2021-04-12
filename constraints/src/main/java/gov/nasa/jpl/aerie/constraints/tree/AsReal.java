package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.InputMismatchException;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public final class AsReal implements Expression<LinearProfile> {
  private final Expression<DiscreteProfile> expression;

  public AsReal(final Expression<DiscreteProfile> expression) {
    this.expression = expression;
  }

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    final var profile = expression.evaluate(results, environment);
    final var linearPieces = new ArrayList<LinearProfilePiece>(profile.profilePieces.size());
    for (final var piece : profile.profilePieces) {
      final var value = piece.value.asReal().orElseThrow(
          () -> new InputMismatchException("Discrete Profile of non-real type cannot be converted to linear"));
      linearPieces.add(new LinearProfilePiece(piece.window, value, 0));
    }

    return new LinearProfile(linearPieces);
  }

  @Override
  public String prettyPrint(final String prefix) {
    return this.expression.prettyPrint(prefix);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AsReal)) return false;
    final var o = (AsReal)obj;

    return Objects.equals(this.expression, o.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.expression);
  }
}
