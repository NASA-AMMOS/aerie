package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.ActivityInstance;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;

import java.util.Map;
import java.util.Set;

public interface Expression<T> {
  T evaluate(final SimulationResults results, final Window bounds, final Map<String, ActivityInstance> environment);
  String prettyPrint(final String prefix);
  /** Add the resources referenced by this expression to the given set. **/
  void extractResources(Set<String> names);

  default T evaluate(final SimulationResults results, final Map<String, ActivityInstance> environment){
    return this.evaluate(results, results.bounds, environment);
  }

  default T evaluate(final SimulationResults results, final Window bounds){
    return this.evaluate(results, results.bounds, Map.of());
  }

  default T evaluate(final SimulationResults results) {
    return this.evaluate(results, Map.of());
  }
  default String prettyPrint() {
    return this.prettyPrint("");
  }
}
