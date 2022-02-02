package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

/**
 * Class allowing to define state query expression for instantiation of parameters
 */
public class StateQueryParam implements VariableArgumentComputer {

  public final QueriableState state;
  public final TimeExpression timeExpr;

  public StateQueryParam(QueriableState state, TimeExpression timeExpression) {
    this.state = state;
    this.timeExpr = timeExpression;
  }

  public SerializedValue getValue(Plan plan, Window win) {
    var time = timeExpr.computeTime(plan, win);
    if (!time.isSingleton()) {
      throw new RuntimeException(" Time expression in StateQueryParam case must be singleton");
    }
    return state.getValueAtTime(time.start);  }
}
