package gov.nasa.jpl.aerie.scheduler.constraints.activities;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.NotNull;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ActivityCreationTemplateDisjunction extends ActivityCreationTemplate {

  List<ActivityCreationTemplate> activityCreationTemplates;

  protected ActivityCreationTemplateDisjunction(
      List<ActivityCreationTemplate> activityCreationTemplates) {
    assert (activityCreationTemplates.size() > 0);
    this.activityCreationTemplates = new ArrayList<>(activityCreationTemplates);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <B extends AbstractBuilder<B, AT>, AT extends ActivityExpression>
      AbstractBuilder<B, AT> getNewBuilder() {
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
  public @NotNull Optional<SchedulingActivityDirective> createActivity(
      String name,
      SimulationFacade facade,
      Plan plan,
      PlanningHorizon planningHorizon,
      EvaluationEnvironment evaluationEnvironment) {
    // TODO: returns first ACT of disjunction, change it
    return activityCreationTemplates
        .get(0)
        .createActivity(name, facade, plan, planningHorizon, evaluationEnvironment);
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
  public @NotNull Optional<SchedulingActivityDirective> createActivity(
      String name,
      Windows windows,
      SimulationFacade facade,
      Plan plan,
      PlanningHorizon planningHorizon,
      EvaluationEnvironment evaluationEnvironment) {
    for (var act : activityCreationTemplates) {
      final var activityCreation =
          act.createActivity(name, windows, facade, plan, planningHorizon, evaluationEnvironment);
      if (activityCreation.isPresent()) {
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
  public boolean matches(
      @NotNull SchedulingActivityDirective act,
      SimulationResults simulationResults,
      EvaluationEnvironment evaluationEnvironment) {
    for (var expr : activityCreationTemplates) {
      if (expr.matches(act, simulationResults, evaluationEnvironment)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Builder for creating disjunction of activity creation templates
   */
  public static class OrBuilder
      extends AbstractBuilder<OrBuilder, ActivityCreationTemplateDisjunction> {

    /**
     * {@inheritDoc}
     */
    public @NotNull OrBuilder getThis() {
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
    public @NotNull OrBuilder basedOn(@NotNull ActivityCreationTemplateDisjunction template) {
      type = template.type;
      startsIn = template.startRange;
      endsIn = template.endRange;
      durationIn = template.duration;
      startsOrEndsIn = template.startOrEndRange;
      arguments = template.arguments;
      activityCreationTemplates = template.activityCreationTemplates;
      return getThis();
    }

    @Override
    public ActivityCreationTemplateDisjunction build() {
      for (var expr : activityCreationTemplates) {
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
          expr.duration = durationIn;
        }
        if (startsOrEndsIn != null) {
          expr.startOrEndRange = startsOrEndsIn;
        }
        if (arguments.size() > 0) {
          expr.arguments = arguments;
        }
      }

      return new ActivityCreationTemplateDisjunction(activityCreationTemplates);
    }

    protected boolean orBuilder = false;

    List<ActivityCreationTemplate> activityCreationTemplates = new ArrayList<>();

    public OrBuilder or(ActivityCreationTemplate expr) {
      activityCreationTemplates.add(expr);
      return getThis();
    }
  }
}
