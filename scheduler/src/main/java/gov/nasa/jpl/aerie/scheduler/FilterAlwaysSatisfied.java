package gov.nasa.jpl.aerie.scheduler;

public class FilterAlwaysSatisfied extends FilterFunctional {

  private StateConstraintExpression expr;

  public FilterAlwaysSatisfied(StateConstraintExpression expr) {
    this.expr = expr;
  }

  @Override
  public boolean shouldKeep(Plan plan, Range<Time> range) {
    TimeWindows valid = expr.findWindows(plan, TimeWindows.of(range));
    return valid.equals(TimeWindows.of(range));
  }
}
