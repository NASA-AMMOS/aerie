package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;

public class ActivityExpressionDisjunction extends ActivityExpression {


  List<ActivityExpression> actExpressions;

  protected ActivityExpressionDisjunction(List<ActivityExpression> actExpressions) {
    this.actExpressions = new ArrayList<ActivityExpression>(actExpressions);
  }


  @SuppressWarnings("unchecked")
  public <B extends AbstractBuilder<B, AT>, AT extends ActivityExpression> AbstractBuilder<B, AT> getNewBuilder() {
    return (AbstractBuilder<B, AT>) new OrBuilder();
  }

  /**
   * @param act IN the activity to evaluate against the template criteria.
   *     not null.
   * @return true if the act instance matches one of the activity expression of the disjunction
   */
  @Override
  public boolean matches(@NotNull ActivityInstance act) {
    for (var expr : actExpressions) {
      if (expr.matches(act)) {
        return true;
      }
    }

    return false;
  }

  public static class OrBuilder extends AbstractBuilder<OrBuilder, ActivityExpressionDisjunction> {

    @Override
    public OrBuilder basedOn(ActivityExpressionDisjunction template) {
      type = template.type;
      startsIn = template.startRange;
      endsIn = template.endRange;
      durationIn = template.durationRange;
      startsOrEndsIn = template.startOrEndRange;
      parameters = template.parameters;
      exprs = template.actExpressions;
      variableParameters = template.variableParameters;
      return getThis();
    }

    /**
     * {@inheritDoc}
     */
    public @NotNull
    OrBuilder getThis() {
      return this;
    }

    @Override
    public ActivityExpressionDisjunction build() {
      ActivityExpressionDisjunction dis = new ActivityExpressionDisjunction(exprs);
      return dis;
    }

    List<ActivityExpression> exprs = new ArrayList<ActivityExpression>();

    public OrBuilder or(ActivityExpression expr) {
      exprs.add(expr);
      return getThis();
    }
  }


}
