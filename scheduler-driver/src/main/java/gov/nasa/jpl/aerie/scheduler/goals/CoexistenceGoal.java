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
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplateDisjunction;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions.DurationExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeExpression;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * describes the desired coexistence of an activity with another
 */
public class CoexistenceGoal extends ActivityTemplateGoal {

  private TimeExpression startExpr;
  private TimeExpression endExpr;
  private DurationExpression durExpr;
  private String alias;

  /**
   * the pattern used to locate anchor activity instances in the plan
   */
  protected Expression<Spans> expr;

  /**
   * used to check this hasn't changed, as if it did, that's probably unanticipated behavior
   */
  protected Spans evaluatedExpr;
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
      goal.expr = forEach;

      goal.startExpr = startExpr;

      goal.endExpr = endExpr;

      goal.durExpr = durExpression;

      goal.alias = alias;

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
  public java.util.Collection<Conflict> getConflicts(Plan plan, final SimulationResults simulationResults) { //TODO: check if interval gets split and if so, notify user?

    //NOTE: temporalContext IS A WINDOWS OVER WHICH THE GOAL APPLIES, USUALLY SOMETHING BROAD LIKE A MISSION PHASE
    //NOTE: expr IS A WINDOWS OVER WHICH A COEXISTENCEGOAL APPLIES, FOR EXAMPLE THE WINDOWS CORRESPONDING TO 5 SECONDS AFTER EVERY BASICACTIVITY IS SCHEDULED
    //NOTE: IF temporalContext IS SMALLER THAN expr OR SOMEHOW BISECTS IT, ODDS ARE THIS ISN'T ANTICIPATED USER BEHAVIOR. GENERALLY, ANALYZEWHEN SHOULDN'T BE PROVIDING
    //        A SMALLER WINDOW, AND HONESTLY DOESN'T MAKE SENSE TO USE ON TOP BUT IS SUPPORTED TO MAKE CODE MORE CONSISTENT. IF ONE NEEDS TO USE ANALYZEWHEN ON TOP
    //        OF COEXISTENCEGOAL THEY SHOULD PROBABLY REFACTOR THEIR COEXISTENCE GOAL. ONE SUCH USE WOULD BE IF THE COEXISTENCEGOAL WAS SPECIFIED IN TERMS OF
    //        AN ACTIVITYEXPRESSION AND THEN ANALYZEWHEN WAS A MISSION PHASE, ALTHOUGH IT IS POSSIBLE TO JUST SPECIFY AN EXPRESSION<WINDOWS> THAT COMBINES THOSE.

    //unwrap temporalContext
    final var windows = getTemporalContext().evaluate(simulationResults);

    //make sure it hasn't changed
    if (this.initiallyEvaluatedTemporalContext != null && !windows.includes(this.initiallyEvaluatedTemporalContext)) {
      throw new UnexpectedTemporalContextChangeException("The temporalContext Windows has changed from: " + this.initiallyEvaluatedTemporalContext.toString() + " to " + windows.toString());
    }
    else if (this.initiallyEvaluatedTemporalContext == null) {
      this.initiallyEvaluatedTemporalContext = windows;
    }

    final var anchors = expr.evaluate(simulationResults).intersectWith(windows);

    //make sure expr hasn't changed either as that could yield unexpected behavior
    if (this.evaluatedExpr != null && !anchors.isCollectionSubsetOf(this.evaluatedExpr)) {
      throw new UnexpectedTemporalContextChangeException("The expr Windows has changed from: " + this.expr.toString() + " to " + anchors.toString());
    }
    else if (this.initiallyEvaluatedTemporalContext == null) {
      this.evaluatedExpr = anchors;
    }

    // can only check if bisection has happened if you can extract the interval from expr like you do in computeRange but without the final windows parameter,
    //    then use that and compare it to local variable windows to check for bisection;
    //    I can add that, but it doesn't seem necessary for now.

    //the rest is the same if no such bisection has happened
    final var conflicts = new java.util.LinkedList<Conflict>();
    for (var window : anchors) {
      boolean disj = false;
      ActivityExpression.AbstractBuilder actTB = null;
      if (this.desiredActTemplate instanceof ActivityCreationTemplateDisjunction) {
        disj = true;
        actTB = new ActivityCreationTemplateDisjunction.OrBuilder();
      } else if (this.desiredActTemplate != null) {
        actTB = new ActivityCreationTemplate.Builder();
      }
      assert actTB != null;
      actTB.basedOn(this.desiredActTemplate);

      if(this.startExpr != null) {
        Interval startTimeRange = null;
        startTimeRange = this.startExpr.computeTime(simulationResults, plan, window.interval());
        actTB.startsIn(startTimeRange);
      }
      if(this.endExpr != null) {
        Interval endTimeRange = null;
        endTimeRange = this.endExpr.computeTime(simulationResults, plan, window.interval());
        actTB.endsIn(endTimeRange);
      }
      /* this will override whatever might be already present in the template */
      if(durExpr!=null){
        var durRange = this.durExpr.compute(window.interval(), simulationResults);
        actTB.durationIn(Interval.between(durRange, durRange));
      }

      ActivityCreationTemplate temp;
      if (disj) {
        temp = (ActivityCreationTemplateDisjunction) actTB.build();
      } else {
        temp = (ActivityCreationTemplate) actTB.build();
      }
      final var existingActs = plan.find(temp, simulationResults, createEvaluationEnvironmentFromAnchor(window));

      var missingActAssociations = new ArrayList<ActivityInstance>();
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
          conflicts.add(new MissingActivityTemplateConflict(this, this.temporalContext.evaluate(simulationResults), temp, createEvaluationEnvironmentFromAnchor(window)));
        } else {
          conflicts.add(new MissingAssociationConflict(this, missingActAssociations));
        }
      }

    }//for(anchorAct)

    return conflicts;
  }

  private EvaluationEnvironment createEvaluationEnvironmentFromAnchor(Segment<Optional<Spans.Metadata>> span){
    if(span.value().isPresent()){
      final var metadata = span.value().get();
      return new EvaluationEnvironment(Map.of(this.alias, metadata.activityInstance()),
                                       Map.of(),
                                       Map.of(),
                                       Map.of());
    } else{
      return new EvaluationEnvironment();
    }
  }

  /**
   * ctor creates an empty goal without details
   *
   * client code should use builders to instance goals
   */
  protected CoexistenceGoal() { }

}
