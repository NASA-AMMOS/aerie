package gov.nasa.jpl.aerie.scheduler.constraints.resources;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

public class StateConstraintExpressionTransition extends StateConstraintExpression {

  private final StateConstraintExpressionEqualSet from;
  private final StateConstraintExpressionEqualSet to;

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
  public Windows findWindows(Plan plan, Windows windows) {
    Windows res = new Windows();
    var fromtw = from.findWindows(plan, windows);
    var totw = to.findWindows(plan, windows);

    for (var rangeFrom : fromtw) {
      for (var rangeTo : totw) {
        if (rangeFrom.isStrictlyBefore(rangeTo) && rangeFrom.adjacent(rangeTo)) {
          res.add(Window.at(rangeFrom.end));
        }
      }
    }
    return res;
  }


}
