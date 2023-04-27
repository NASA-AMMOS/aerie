package gov.nasa.jpl.aerie.scheduler.goals;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.PlanningHorizon;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.simulation.SimulationFacade;
import java.util.Optional;

/**
 * describes the desired existence of an activity matching a given template/preset
 */
public class ActivityTemplateGoal extends ActivityExistentialGoal {

  /**
   * the builder can construct goals piecemeal via a series of method calls
   */
  public abstract static class Builder<T extends Builder<T>>
      extends ActivityExistentialGoal.Builder<T> {

    /**
     * specifies the activity that must exist, and how to create it if necessary
     *
     * this specifier is required. it replaces any previous specification.
     *
     * @param template IN a pattern for matching satisfying activity instances
     *     with the ability to create new instances if necessary
     * @return this builder, ready for additional specification
     */
    public T thereExistsOne(ActivityCreationTemplate template) {
      thereExists = template;
      return getThis();
    }

    public T match(ActivityExpression expression) {
      matchingActTemplate = expression;
      return getThis();
    }

    protected ActivityCreationTemplate thereExists;
    protected ActivityExpression matchingActTemplate;

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivityTemplateGoal build() {
      return fill(new ActivityTemplateGoal());
    }

    /**
     * populates the provided goal with specifiers from this builder and above
     *
     * typically called by any derived builder classes to fill in the
     * specifiers managed at this builder level and above
     *
     * @param goal IN/OUT a goal object to be filled with specifiers from this
     *     level of builder and above
     * @return the provided goal object, with details filled in
     */
    protected ActivityTemplateGoal fill(ActivityTemplateGoal goal) {
      // first fill in any general specifiers from parent
      super.fill(goal);

      if (thereExists == null) {
        throw new IllegalArgumentException(
            "activity template goal requires non-null thereExists activity creation template");
      }
      goal.desiredActTemplate = thereExists;

      if (matchingActTemplate == null) {
        goal.matchActTemplate = thereExists;
      } else {
        goal.matchActTemplate = matchingActTemplate;
      }

      goal.initiallyEvaluatedTemporalContext = null;

      return goal;
    }
  } // Builder

  public ActivityCreationTemplate getActTemplate() {
    return desiredActTemplate;
  }

  /**
   * returns the set of constraints required by the created activity type
   *
   * these are type-level constraints only, and do not include any additional
   * constraints that may be applied to individual instances
   *
   * @return an unmodifiable collection of all the constraints implied by
   *     the goal's created activity type
   */
  public Expression<Windows> getActivityStateConstraints() {
    return desiredActTemplate.getType().getStateConstraints();
  }

  /**
   * creates a new activity instance that will increase this goal's satisfaction
   *
   * the activity is labeled as being created for this goal
   *
   * the activity is not inserted into any plan yet
   *
   * @return a new activity instance that will improve the satisfaction of
   *     this goal if it were inserted into a plan
   */
  public Optional<SchedulingActivityDirective> createActivity(
      SimulationFacade facade,
      Plan plan,
      PlanningHorizon planningHorizon,
      EvaluationEnvironment evaluationEnvironment) {
    // REVIEW: uuid probably overkill. random is especially bad for repeatability.
    final var actName = getName() + "_" + java.util.UUID.randomUUID();
    return desiredActTemplate.createActivity(
        actName, facade, plan, planningHorizon, evaluationEnvironment);
  }

  /**
   * ctor creates new empty goal without identification / specification
   *
   * client code should use derived type builders to instance goals
   */
  protected ActivityTemplateGoal() {}

  /**
   * the pattern used to create new instances if none already exist
   */
  protected ActivityCreationTemplate desiredActTemplate;

  /**
   * the pattern used to match with satisfying activity instances
   */
  protected ActivityExpression matchActTemplate;

  /**
   * checked by getConflicts every time it is invoked to see if the Window(s)
   * corresponding to when this goal has changed, which is unexpected behavior
   * that needs to be caught
   */
  protected Windows initiallyEvaluatedTemporalContext;
}
