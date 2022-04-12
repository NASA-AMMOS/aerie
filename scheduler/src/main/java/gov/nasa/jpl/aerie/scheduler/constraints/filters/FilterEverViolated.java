package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateConstraintExpression;

/**
 * filter in intervals if constraint expression @expr is ever violated during it
 */
public class FilterEverViolated extends FilterFunctional {

  private final StateConstraintExpression expr;

  public FilterEverViolated(StateConstraintExpression expr) {
    this.expr = expr;
  }

  @Override
  public boolean shouldKeep(Plan plan, Window range) {
    return !(expr.findWindows(plan, new Windows(range)).equals(new Windows(range)));
  }
}
