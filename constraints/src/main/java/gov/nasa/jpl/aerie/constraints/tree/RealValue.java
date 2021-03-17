package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.List;
import java.util.Map;

public final class RealValue implements Expression<LinearProfile> {
  private final double value;

  public RealValue(final double value) {
    this.value = value;
  }

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return new LinearProfile(
        List.of(
            new LinearProfilePiece(results.bounds, this.value, 0.0)
        )
    );
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(value %s)",
        prefix,
        this.value
    );
  }
}
