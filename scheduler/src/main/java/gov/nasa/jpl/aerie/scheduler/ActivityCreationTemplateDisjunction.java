package gov.nasa.jpl.aerie.scheduler;

import java.util.ArrayList;
import java.util.List;

public class ActivityCreationTemplateDisjunction extends ActivityCreationTemplate {


  List<ActivityCreationTemplate> acts;

  protected ActivityCreationTemplateDisjunction(List<ActivityCreationTemplate> acts) {
    assert (acts.size() > 0);
    this.acts = new ArrayList<ActivityCreationTemplate>(acts);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <B extends ActivityExpression.AbstractBuilder<B, AT>, AT extends ActivityExpression> ActivityExpression.AbstractBuilder<B, AT> getNewBuilder() {
    return (ActivityExpression.AbstractBuilder<B, AT>) new OrBuilder();
  }

  /**
   * generate a new activity instance based on template defaults
   *
   * @param name IN the activity instance identifier to associate to the
   *     newly constructed activity instance
   * @return a newly constructed activity instance with values chosen
   *     according to any specified template criteria, the first of the disjunction
   */
  @Override
  public @NotNull
  ActivityInstance createActivity(String name) {
    //TODO: returns first ACT of disjunction, change it
    return acts.get(0).createActivity(name);

  }

  /**
   * generate a new activity instance based on template defaults
   *
   * @param name IN the activity instance identifier to associate to the
   *     newly constructed activity instance
   * @return a newly constructed activity instance with values chosen
   *     according to any specified template criteria, the first of the disjunction
   */
  @Override
  public @NotNull
  ActivityInstance createActivity(String name, TimeWindows windows) {
    //TODO: returns first ACT of disjunction, change it
    return acts.get(0).createActivity(name, windows);

  }

  /**
   * @param act IN the activity to evaluate against the template criteria.
   *     not null.
   * @return true if the act instance matches one of the activity expression of the disjunction
   */
  @Override
  public boolean matches(@NotNull ActivityInstance act) {
    for (var expr : acts) {
      if (expr.matches(act)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Builder for creating disjunction of activity creation templates
   */
  public static class OrBuilder extends
      ActivityExpression.AbstractBuilder<OrBuilder, ActivityCreationTemplateDisjunction>
  {

    /**
     * {@inheritDoc}
     */
    public @NotNull
    OrBuilder getThis() {
      return this;
    }

    /**
     * bootstraps a new query builder based on existing template
     *
     * the new builder may then be modified without impacting the existing
     * template criteria, eg by adding additional new terms or replacing
     * existing terms
     *
     * @param template IN the template whose criteria should be duplicated
     *     into this builder. must not be null.
     * @return the same builder object updated with new criteria
     */
    @Override
    public @NotNull
    OrBuilder basedOn(@NotNull ActivityCreationTemplateDisjunction template) {
      type = template.type;
      startsIn = template.startRange;
      endsIn = template.endRange;
      durationIn = template.durationRange;
      startsOrEndsIn = template.startOrEndRange;
      nameMatches = (template.nameRE != null) ? template.nameRE.pattern() : null;
      parameters = template.parameters;
      exprs = template.acts;
      return getThis();
    }


    @Override
    public ActivityCreationTemplateDisjunction build() {
      for (var expr : exprs) {
        if (type != null) {
          //if(expr.type!=null){
          //    throw new IllegalArgumentException("Overdefined activity expression");
          // }
          expr.type = type;
        }
        if (startsIn != null) {
          //if(expr.startRange!=null){
          //    throw new IllegalArgumentException("Overdefined activity expression");
          // }
          expr.startRange = startsIn;
        }
        if (endsIn != null) {
          //if(expr.endRange!=null){
          //     throw new IllegalArgumentException("Overdefined activity expression");
          // }
          expr.endRange = endsIn;
        }
        if (durationIn != null) {
          //if(expr.durationRange!=null){
          //     throw new IllegalArgumentException("Overdefined activity expression");
          // }
          expr.durationRange = durationIn;
        }
        if (startsOrEndsIn != null) {
          //if(expr.startOrEndRange!=null){
          //    throw new IllegalArgumentException("Overdefined activity expression");
          //}
          expr.startOrEndRange = startsOrEndsIn;
        }
        if (nameMatches != null) {
          //if(expr.nameRE!=null){
          //    throw new IllegalArgumentException("Overdefined activity expression");
          // }
          throw new IllegalArgumentException("Todo");

        }
        if (parameters.size() > 0) {
          // if(expr.parameters.size()>0){
          //    throw new IllegalArgumentException("Overdefined activity expression");
          //}
          expr.parameters = parameters;
        }
      }

      ActivityCreationTemplateDisjunction dis = new ActivityCreationTemplateDisjunction(exprs);
      return dis;
    }

    protected boolean orBuilder = false;

    List<ActivityCreationTemplate> exprs = new ArrayList<ActivityCreationTemplate>();

    public OrBuilder or(ActivityCreationTemplate expr) {
      exprs.add(expr);
      return getThis();
    }
  }

}
