package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

/**
 * filter in intervals if constraint expression @expr is ever violated during it
 */
public class FilterEverViolated extends FilterFunctional {

  private final Expression<Windows> expr;

  public FilterEverViolated(final Expression<Windows> expr) {
    this.expr = expr;
  }

  @Override
  public boolean shouldKeep(final SimulationResults simulationResults, final Plan plan, final Window range) {
    return !(expr.evaluate(simulationResults, range, null).equals(new Windows(range)));
  }
}
