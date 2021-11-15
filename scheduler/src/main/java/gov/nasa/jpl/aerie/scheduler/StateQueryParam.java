package gov.nasa.jpl.aerie.scheduler;

/**
 * Class allowing to define state query expression for instantiation of parameters
 */
public class StateQueryParam {

  public QueriableState<?> state;
  public TimeExpression timeExpr;

  public StateQueryParam(QueriableState<?> state, TimeExpression timeExpression) {
    this.state = state;
    this.timeExpr = timeExpression;
  }

}
