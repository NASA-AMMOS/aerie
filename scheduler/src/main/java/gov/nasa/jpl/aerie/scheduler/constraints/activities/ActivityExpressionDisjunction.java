package gov.nasa.jpl.aerie.scheduler.constraints.activities;

import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ActivityExpressionDisjunction extends ActivityExpression {


  final List<ActivityExpression> actExpressions;

  protected ActivityExpressionDisjunction(List<ActivityExpression> actExpressions) {
    this.actExpressions = new ArrayList<>(actExpressions);
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
      arguments = template.arguments;
      exprs = template.actExpressions;
      variableArguments = template.variableArguments;
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
      return new ActivityExpressionDisjunction(exprs);
    }

    List<ActivityExpression> exprs = new ArrayList<>();

    public OrBuilder or(ActivityExpression expr) {
      exprs.add(expr);
      return getThis();
    }
  }


}
