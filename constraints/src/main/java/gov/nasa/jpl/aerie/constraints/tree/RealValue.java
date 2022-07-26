package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record RealValue(double value) implements LinearProfileExpression {

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment) {
    return new LinearProfile(
        List.of(
            new LinearProfilePiece(bounds, this.value, 0.0)
        )
    );
  }

  @Override
  public void extractResources(final Set<String> names) {
  }

  @Override
  public String prettyPrint(final String prefix) {
    return String.format(
        "\n%s(value %s)",
        prefix,
        this.value
    );
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof final RealValue o)) return false;

    return Objects.equals(this.value, o.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.value);
  }
}
