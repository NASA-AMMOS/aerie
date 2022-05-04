package gov.nasa.jpl.aerie.scheduler.constraints.activities;

import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.NotNull;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ActivityCreationTemplateDisjunction extends ActivityCreationTemplate {


  List<ActivityCreationTemplate> acts;

  protected ActivityCreationTemplateDisjunction(List<ActivityCreationTemplate> acts) {
    assert (acts.size() > 0);
    this.acts = new ArrayList<>(acts);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <B extends AbstractBuilder<B, AT>, AT extends ActivityExpression> AbstractBuilder<B, AT> getNewBuilder() {
    return (AbstractBuilder<B, AT>) new OrBuilder();
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
  Optional<ActivityInstance> createActivity(String name, SimulationFacade facade, Plan plan, PlanningHorizon planningHorizon) {
    //TODO: returns first ACT of disjunction, change it
    return acts.get(0).createActivity(name, facade, plan, planningHorizon);

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
  Optional<ActivityInstance> createActivity(String name, Windows windows, boolean instantiateVariableArguments, SimulationFacade facade, Plan plan, PlanningHorizon planningHorizon) {
    for(var act : acts) {
      final var activityCreation = act.createActivity(name, windows, instantiateVariableArguments, facade, plan, planningHorizon);
      if(activityCreation.isPresent()){
        return activityCreation;
      }
    }
    return Optional.empty();
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
      AbstractBuilder<OrBuilder, ActivityCreationTemplateDisjunction>
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
      arguments = template.arguments;
      exprs = template.acts;
      return getThis();
    }


    @Override
    public ActivityCreationTemplateDisjunction build() {
      for (var expr : exprs) {
        if (type != null) {
          expr.type = type;
        }
        if (startsIn != null) {
          expr.startRange = startsIn;
        }
        if (endsIn != null) {
          expr.endRange = endsIn;
        }
        if (durationIn != null) {
          expr.durationRange = durationIn;
        }
        if (startsOrEndsIn != null) {
          expr.startOrEndRange = startsOrEndsIn;
        }
        if (arguments.size() > 0) {
          expr.arguments = arguments;
        }
      }

      return new ActivityCreationTemplateDisjunction(exprs);
    }

    protected boolean orBuilder = false;

    List<ActivityCreationTemplate> exprs = new ArrayList<>();

    public OrBuilder or(ActivityCreationTemplate expr) {
      exprs.add(expr);
      return getThis();
    }
  }

}
