package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.InputMismatchException;
import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.DiscreteProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.ArrayList;
import java.util.Map;

public final class AsReal implements Expression<LinearProfile> {
  private final Expression<DiscreteProfile> source;

  public AsReal(final Expression<DiscreteProfile> source) {
    this.source = source;
  }

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    final var profile = source.evaluate(results, environment);
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
    return this.source.prettyPrint(prefix);
  }
}
