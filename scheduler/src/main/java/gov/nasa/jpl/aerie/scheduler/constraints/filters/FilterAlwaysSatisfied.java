package gov.nasa.jpl.aerie.scheduler.constraints.filters;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateConstraintExpression;

public class FilterAlwaysSatisfied extends FilterFunctional {

  private final StateConstraintExpression expr;

  public FilterAlwaysSatisfied(StateConstraintExpression expr) {
    this.expr = expr;
  }

  @Override
  public boolean shouldKeep(Plan plan, Window range) {
    var valid = expr.findWindows(plan, new Windows(range));
    return valid.equals(new Windows(range));
  }
}
