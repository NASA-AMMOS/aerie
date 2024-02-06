package gov.nasa.jpl.aerie.scheduler.goals;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.constraints.time.Spans;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityTemplateConflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingAssociationConflict;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeExpression;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

/**
 * describes the desired coexistence of an activity with another
 */
public class CoexistenceGoal extends ActivityTemplateGoal {

  private TimeExpression startExpr;
  private TimeExpression endExpr;
  private String alias;

  /**
   * the pattern used to locate anchor activity instances in the plan
   */
  protected Expression<Spans> expr;

  /**
   * the builder can construct goals piecemeal via a series of method calls
   */
  public static class Builder extends ActivityTemplateGoal.Builder<Builder> {

    public Builder forEach(Expression<Spans> expression) {
      forEach = expression;
      return getThis();
    }

    protected Expression<Spans> forEach;

    public Builder startsAt(TimeExpression timeExpression) {
      startExpr = timeExpression;
      return getThis();
    }

    protected DurationExpression durExpression;
    public Builder durationIn(DurationExpression durExpr){
      this.durExpression = durExpr;
      return getThis();
    }

    protected TimeExpression startExpr;

    public Builder endsAt(TimeExpression timeExpression) {
      endExpr = timeExpression;
      return getThis();
    }

    protected TimeExpression endExpr;


    public Builder startsAt(TimeAnchor anchor) {
      startExpr = TimeExpression.fromAnchor(anchor);
      return getThis();
    }

    public Builder endsAt(TimeAnchor anchor) {
      endExpr = TimeExpression.fromAnchor(anchor);
      return getThis();
    }

    public Builder endsBefore(TimeExpression expr) {
      endExpr = TimeExpression.endsBefore(expr);
      return getThis();
    }

    public Builder startsAfterEnd() {
      startExpr = TimeExpression.afterEnd();
      return getThis();
    }

    public Builder startsAfterStart() {
      startExpr = TimeExpression.afterStart();
      return getThis();
    }

    public Builder endsBeforeEnd() {
      endExpr = TimeExpression.beforeEnd();
      return getThis();
    }

    public Builder endsAfterEnd() {
      endExpr = TimeExpression.afterEnd();
      return getThis();
    }

    String alias;
    public Builder aliasForAnchors(String alias){
      this.alias = alias;
      return getThis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CoexistenceGoal build() { return fill(new CoexistenceGoal()); }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Builder getThis() { return this; }

    /**
     * populates the provided goal with specifiers from this builder and above
     *
     * typically called by any derived builder classes to fill in the
     * specifiers managed at this builder level and above
     *
     * @param goal IN/OUT a goal object to be filled with specifiers from this
     *     level of builder and above
     * @return the provided object, with details filled in
     */
    protected CoexistenceGoal fill(CoexistenceGoal goal) {
      //first fill in any general specifiers from parents
      super.fill(goal);

      if (forEach == null) {
        throw new IllegalArgumentException(
            "creating coexistence goal requires non-null \"forEach\" anchor template");
      }
      if (alias == null) {
        throw new IllegalArgumentException(
            "creating coexistence goal requires non-null \"alias\" name");
      }
      goal.expr = forEach;

      goal.startExpr = startExpr;

      goal.endExpr = endExpr;

      goal.durExpr = durExpression;

      goal.alias = alias;

      if(name==null){
        goal.name = "CoexistenceGoal_forEach_"+forEach.prettyPrint()+"_thereExists_"+this.thereExists.type().getName();
      }

      return goal;
    }

  }//Builder

  /**
   * {@inheritDoc}
   *
   * collects conflicts wherein a matching anchor activity was found
   * but there was no corresponding target activity instance (and one
   * should probably be created!)
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public java.util.Collection<Conflict> getConflicts(Plan plan, final SimulationResults simulationResults, final EvaluationEnvironment evaluationEnvironment) { //TODO: check if interval gets split and if so, notify user?
    //unwrap temporalContext
    final var windows = getTemporalContext().evaluate(simulationResults, evaluationEnvironment);

    final var anchors = expr.evaluate(simulationResults, evaluationEnvironment).intersectWith(windows);

    // can only check if bisection has happened if you can extract the interval from expr like you do in computeRange but without the final windows parameter,
    //    then use that and compare it to local variable windows to check for bisection;
    //    I can add that, but it doesn't seem necessary for now.

    //the rest is the same if no such bisection has happened
    final var conflicts = new java.util.LinkedList<Conflict>();
    for (var window : anchors) {
      ActivityExpression.Builder activityFinder = null;
      ActivityExpression.Builder activityCreationTemplate = null;
      if (this.desiredActTemplate != null) {
        activityFinder = new ActivityExpression.Builder();
        activityCreationTemplate = new ActivityExpression.Builder();
      }
      assert activityFinder != null;
      activityFinder.basedOn(this.matchActTemplate);
      activityCreationTemplate.basedOn(this.desiredActTemplate);
      if(this.startExpr != null) {
        Interval startTimeRange = null;
        startTimeRange = this.startExpr.computeTime(simulationResults, plan, window.interval());
        activityFinder.startsIn(startTimeRange);
        activityCreationTemplate.startsIn(startTimeRange);
      }
      if(this.endExpr != null) {
        Interval endTimeRange = null;
        endTimeRange = this.endExpr.computeTime(simulationResults, plan, window.interval());
        activityFinder.endsIn(endTimeRange);
        activityCreationTemplate.endsIn(endTimeRange);
      }
      /* this will override whatever might be already present in the template */
      if(durExpr!=null){
        var durRange = this.durExpr.compute(window.interval(), simulationResults);
        activityFinder.durationIn(durRange);
        activityCreationTemplate.durationIn(durRange);
      }

      final var existingActs = plan.find(
          activityFinder.build(),
          simulationResults,
          createEvaluationEnvironmentFromAnchor(evaluationEnvironment, window));

      var missingActAssociations = new ArrayList<SchedulingActivityDirective>();
      var planEvaluation = plan.getEvaluation();
      var associatedActivitiesToThisGoal = planEvaluation.forGoal(this).getAssociatedActivities();
      var alreadyOneActivityAssociated = false;
      for(var act : existingActs){
        //has already been associated to this goal
        if(associatedActivitiesToThisGoal.contains(act)){
          alreadyOneActivityAssociated = true;
          break;
        }
      }
      if(!alreadyOneActivityAssociated){
        for(var act : existingActs){
          if(planEvaluation.canAssociateMoreToCreatorOf(act)){
            missingActAssociations.add(act);
          }
        }
      }
      if (!alreadyOneActivityAssociated) {
        //create conflict if no matching target activity found
        if (existingActs.isEmpty()) {
          conflicts.add(new MissingActivityTemplateConflict(
              this,
              this.temporalContext.evaluate(simulationResults, evaluationEnvironment),
              activityCreationTemplate.build(),
              createEvaluationEnvironmentFromAnchor(evaluationEnvironment, window),
              1,
              Optional.empty()));
        } else {
          conflicts.add(new MissingAssociationConflict(this, missingActAssociations));
        }
      }

    }//for(anchorAct)

    return conflicts;
  }

  private EvaluationEnvironment createEvaluationEnvironmentFromAnchor(EvaluationEnvironment existingEnvironment, Segment<Optional<Spans.Metadata>> span){
    if(span.value().isPresent()){
      final var metadata = span.value().get();
      final var activityInstances = new HashMap<>(existingEnvironment.activityInstances());
      activityInstances.put(this.alias, metadata.activityInstance());
      return new EvaluationEnvironment(
          activityInstances,
          existingEnvironment.spansInstances(),
          existingEnvironment.intervals(),
          existingEnvironment.realExternalProfiles(),
          existingEnvironment.discreteExternalProfiles()
      );
    } else{
      assert this.alias != null;
      final var intervals = new HashMap<>(existingEnvironment.intervals());
      intervals.put(this.alias, span.interval());
      return new EvaluationEnvironment(
          existingEnvironment.activityInstances(),
          existingEnvironment.spansInstances(),
          intervals,
          existingEnvironment.realExternalProfiles(),
          existingEnvironment.discreteExternalProfiles()
      );
    }
  }

  @Override
  public void extractResources(final Set<String> names) {
    super.extractResources(names);
    this.expr.extractResources(names);
    if(this.startExpr != null) this.startExpr.extractResources(names);
    if(this.endExpr != null) this.endExpr.extractResources(names);
  }

  /**
   * ctor creates an empty goal without details
   *
   * client code should use builders to instance goals
   */
  protected CoexistenceGoal() { }
}
