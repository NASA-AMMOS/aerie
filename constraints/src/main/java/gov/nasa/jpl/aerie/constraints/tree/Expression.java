package gov.nasa.jpl.aerie.constraints.tree;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;

import java.util.Set;

public interface Expression<T> {
  T evaluate(final SimulationResults results, final Interval bounds, final EvaluationEnvironment environment);
  String prettyPrint(final String prefix);
  /** Add the resources referenced by this expression to the given set. **/
  void extractResources(Set<String> names);

  default T evaluate(final SimulationResults results, final EvaluationEnvironment environment){
    return this.evaluate(results, results.bounds, environment);
  }

  default T evaluate(final SimulationResults results, final Interval bounds){
    return this.evaluate(results, bounds, new EvaluationEnvironment());
  }

  default T evaluate(final SimulationResults results) {
    return this.evaluate(results, new EvaluationEnvironment());
  }
  default String prettyPrint() {
    return this.prettyPrint("");
  }
}
