package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Simple expression that produces a window at all times.
 */
public final class True implements Expression<Windows> {

  public True() {}

  @Override
  public Windows evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment) {
    return new Windows(results.bounds);
  }

  @Override
  public void extractResources(final Set<String> names) {
  }

  @Override
  public String prettyPrint(final String prefix) {
    return "True";
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof True;
  }
}
