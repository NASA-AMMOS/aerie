package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;

import java.util.Map;

public interface Expression<T> {
  T evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment);
  String prettyPrint(final String prefix);

  default String prettyPrint() {
    return this.prettyPrint("");
  }
}
