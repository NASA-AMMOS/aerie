package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.LinearProfile;
import gov.nasa.jpl.aerie.constraints.model.LinearProfilePiece;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RealValue implements Expression<LinearProfile> {
  public final double value;

  public RealValue(final double value) {
    this.value = value;
  }

  @Override
  public LinearProfile evaluate(final SimulationResults results, final Windows bounds, final Map<String, ActivityInstance> environment) {
    //this Windows will guaranteed be a single window. had to make it conform to a Windows because of a fix implemented in RealResource as that ones bounds SHOULD be Windows.
    return new LinearProfile(
        List.of(
            new LinearProfilePiece(Window.encapsulates(bounds), this.value, 0.0)
        )
    );
  }

  @Override
  public void extractResources(final Set<String> names) { }

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
    if (!(obj instanceof RealValue)) return false;
    final var o = (RealValue)obj;

    return Objects.equals(this.value, o.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.value);
  }
}
