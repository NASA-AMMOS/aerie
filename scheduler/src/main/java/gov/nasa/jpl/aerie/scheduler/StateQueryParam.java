package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;

/**
 * Class allowing to define state query expression for instantiation of parameters
 */
public class StateQueryParam<T> {

  public QueriableState<T> state;
  public TimeExpression timeExpr;

  public StateQueryParam(QueriableState<T> state, TimeExpression timeExpression) {
    this.state = state;
    this.timeExpr = timeExpression;
  }

  public T getValue(Plan plan, Window win) {
    var time = timeExpr.computeTime(plan, win);
    if (!time.isSingleton()) {
      throw new RuntimeException(" Time expression in StateQueryParam case must be singleton");
    }
    return state.getValueAtTime(time.start);
  }
}
