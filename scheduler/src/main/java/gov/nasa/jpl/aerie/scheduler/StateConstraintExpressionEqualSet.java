package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;

public class StateConstraintExpressionEqualSet extends StateConstraintExpression {


  StateConstraintExpressionDisjunction sced;
  ExternalState<?> state;

  public <T extends Comparable<T>> StateConstraintExpressionEqualSet(ExternalState<T> state, List<T> values) {
    super(null);
    List<StateConstraintExpression> list = new ArrayList<StateConstraintExpression>();
    for (var value : values) {
      var ec = StateConstraintExpression.buildEqualConstraint(state, value);
      list.add(new StateConstraintExpression(ec));
    }
    this.state = state;
    this.sced = new StateConstraintExpressionDisjunction(list);
  }

  public ExternalState<?> getState() {
    return state;
  }


  @Override
  public TimeWindows findWindows(Plan plan, TimeWindows windows) {
    return sced.findWindows(plan, windows);
  }
}
