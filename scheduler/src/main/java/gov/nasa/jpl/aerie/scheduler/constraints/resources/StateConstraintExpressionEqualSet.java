package gov.nasa.jpl.aerie.scheduler.constraints.resources;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.ArrayList;
import java.util.List;

public class StateConstraintExpressionEqualSet extends StateConstraintExpression {


  protected final StateConstraintExpressionDisjunction sced;
  protected final ExternalState state;

  public <T extends Comparable<T>> StateConstraintExpressionEqualSet(ExternalState state, List<SerializedValue> values) {
    super(null);
    List<StateConstraintExpression> list = new ArrayList<>();
    for (var value : values) {
      var ec = StateConstraintExpression.buildEqualConstraint(state, value);
      list.add(new StateConstraintExpression(ec));
    }
    this.state = state;
    this.sced = new StateConstraintExpressionDisjunction(list);
  }

  public ExternalState getState() {
    return state;
  }


  @Override
  public Windows findWindows(Plan plan, Windows windows) {
    return sced.findWindows(plan, windows);
  }
}
