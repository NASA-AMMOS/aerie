package gov.nasa.jpl.aerie.scheduler;

/**
 * filter in intervals if constraint expression @expr is ever violated during it
 */
public class FilterEverViolated extends FilterFunctional {

  private StateConstraintExpression expr;

  public FilterEverViolated(StateConstraintExpression expr) {
    this.expr = expr;
  }

  @Override
  public boolean shouldKeep(Plan plan, Range<Time> range) {
    return !(expr.findWindows(plan, TimeWindows.of(range)).equals(TimeWindows.of(range)));
  }
}
