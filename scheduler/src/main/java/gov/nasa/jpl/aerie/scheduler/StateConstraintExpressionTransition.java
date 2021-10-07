package gov.nasa.jpl.aerie.scheduler;

public class StateConstraintExpressionTransition extends StateConstraintExpression {

  private StateConstraintExpressionEqualSet from;
  private StateConstraintExpressionEqualSet to;

  public StateConstraintExpressionTransition(
      StateConstraintExpressionEqualSet from,
      StateConstraintExpressionEqualSet to)
  {
    super(null);
    //states should be the same
    assert (from.state.equals(to.state));
    this.from = from;
    this.to = to;
  }

  @Override
  public TimeWindows findWindows(Plan plan, TimeWindows windows) {
    TimeWindows res = new TimeWindows();
    var fromtw = from.findWindows(plan, windows);
    var totw = to.findWindows(plan, windows);

    for (var rangeFrom : fromtw.getRangeSet()) {
      for (var rangeTo : totw.getRangeSet()) {
        if (rangeFrom.isBefore(rangeTo) && rangeFrom.isAdjacent(rangeTo)) {
          res.union(new Range<Time>(rangeFrom.getMaximum()));
        }
      }
    }
    return res;
  }


}
