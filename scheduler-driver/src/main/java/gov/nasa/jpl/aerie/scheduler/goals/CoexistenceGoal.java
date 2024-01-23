package gov.nasa.jpl.aerie.scheduler.goals;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.constraints.time.Spans;
import gov.nasa.jpl.aerie.constraints.tree.Expression;
import gov.nasa.jpl.aerie.merlin.driver.ActivityDirectiveId;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityTemplateConflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingAssociationConflict;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions.DurationExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeExpressionRelative;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirectiveId;
import org.apache.commons.collections4.BidiMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

/**
 * describes the desired coexistence of an activity with another
 */
public class CoexistenceGoal extends ActivityTemplateGoal {

  private TimeExpressionRelative startExpr;
  private TimeExpressionRelative endExpr;
  private DurationExpression durExpr;
  private String alias;
  private boolean allowReuseExistingActivity;
  private boolean allowActivityUpdate;
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

    public Builder startsAt(TimeExpressionRelative TimeExpressionRelative) {
      startExpr = TimeExpressionRelative;
      return getThis();
    }

    protected DurationExpression durExpression;
    public Builder durationIn(DurationExpression durExpr){
      this.durExpression = durExpr;
      return getThis();
    }

    protected TimeExpressionRelative startExpr;

    public Builder endsAt(TimeExpressionRelative TimeExpressionRelative) {
      endExpr = TimeExpressionRelative;
      return getThis();
    }

    protected TimeExpressionRelative endExpr;


    public Builder startsAt(TimeAnchor anchor) {
      startExpr = TimeExpressionRelative.fromAnchor(anchor);
      return getThis();
    }

    public Builder endsAt(TimeAnchor anchor) {
      endExpr = TimeExpressionRelative.fromAnchor(anchor);
      return getThis();
    }

    public Builder endsBefore(TimeExpressionRelative expr) {
      endExpr = TimeExpressionRelative.endsBefore(expr);
      return getThis();
    }

    public Builder startsAfterEnd() {
      startExpr = TimeExpressionRelative.afterEnd();
      return getThis();
    }

    public Builder startsAfterStart() {
      startExpr = TimeExpressionRelative.afterStart();
      return getThis();
    }

    public Builder endsBeforeEnd() {
      endExpr = TimeExpressionRelative.beforeEnd();
      return getThis();
    }

    public Builder endsAfterEnd() {
      endExpr = TimeExpressionRelative.afterEnd();
      return getThis();
    }

    String alias;
    public Builder aliasForAnchors(String alias){
      this.alias = alias;
      return getThis();
    }

    boolean allowReuseExistingActivity;
    public Builder createPersistentAnchor(boolean createPersistentAnchor){
      this.allowReuseExistingActivity = createPersistentAnchor;
      return getThis();
    }

    boolean allowActivityUpdate;
    public Builder allowActivityUpdate(boolean allowActivityUpdate){
      this.allowActivityUpdate = allowActivityUpdate;
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

      goal.allowReuseExistingActivity = allowReuseExistingActivity;

      goal.allowActivityUpdate = allowActivityUpdate;

      if(name==null){
        goal.name = "CoexistenceGoal_forEach_"+forEach.prettyPrint("")+"_thereExists_"+this.thereExists.type().getName();
      }

      return goal;
    }

  }//Builder

  public boolean isAllowReuseExistingActivity() {
    return allowReuseExistingActivity;
  }

  public boolean isAllowActivityUpdate() {
    return allowActivityUpdate;
  }
  /**
   * {@inheritDoc}
   *
   * collects conflicts wherein a matching anchor activity was found
   * but there was no corresponding target activity instance (and one
   * should probably be created!)
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public java.util.Collection<Conflict> getConflicts(Plan plan, final SimulationResults simulationResults, final Optional<BidiMap<SchedulingActivityDirectiveId, ActivityDirectiveId>> mapSchedulingIdsToActivityIds, final EvaluationEnvironment evaluationEnvironment) { //TODO: check if interval gets split and if so, notify user?

    //NOTE: temporalContext IS A WINDOWS OVER WHICH THE GOAL APPLIES, USUALLY SOMETHING BROAD LIKE A MISSION PHASE
    //NOTE: expr IS A WINDOWS OVER WHICH A COEXISTENCEGOAL APPLIES, FOR EXAMPLE THE WINDOWS CORRESPONDING TO 5 SECONDS AFTER EVERY BASICACTIVITY IS SCHEDULED
    //NOTE: IF temporalContext IS SMALLER THAN expr OR SOMEHOW BISECTS IT, ODDS ARE THIS ISN'T ANTICIPATED USER BEHAVIOR. GENERALLY, ANALYZEWHEN SHOULDN'T BE PROVIDING
    //        A SMALLER WINDOW, AND HONESTLY DOESN'T MAKE SENSE TO USE ON TOP BUT IS SUPPORTED TO MAKE CODE MORE CONSISTENT. IF ONE NEEDS TO USE ANALYZEWHEN ON TOP
    //        OF COEXISTENCEGOAL THEY SHOULD PROBABLY REFACTOR THEIR COEXISTENCE GOAL. ONE SUCH USE WOULD BE IF THE COEXISTENCEGOAL WAS SPECIFIED IN TERMS OF
    //        AN ACTIVITYEXPRESSION AND THEN ANALYZEWHEN WAS A MISSION PHASE, ALTHOUGH IT IS POSSIBLE TO JUST SPECIFY AN EXPRESSION<WINDOWS> THAT COMBINES THOSE.

    //unwrap temporalContext
    final var windows = getTemporalContext().evaluate(simulationResults, evaluationEnvironment);

    //make sure it hasn't changed
    if (this.initiallyEvaluatedTemporalContext != null && !windows.includes(this.initiallyEvaluatedTemporalContext)) {
      throw new UnexpectedTemporalContextChangeException("The temporalContext Windows has changed from: " + this.initiallyEvaluatedTemporalContext.toString() + " to " + windows.toString());
    }
    else if (this.initiallyEvaluatedTemporalContext == null) {
      this.initiallyEvaluatedTemporalContext = windows;
    }

    final var anchors = expr.evaluate(simulationResults, evaluationEnvironment).intersectWith(windows);

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
      ActivityExpression.Builder activityFinder = null;
      ActivityExpression.Builder activityCreationTemplate = null;
      if (this.desiredActTemplate != null) {
        activityFinder = new ActivityExpression.Builder();
        activityCreationTemplate = new ActivityExpression.Builder();
      }
      assert activityFinder != null;
      activityFinder.basedOn(this.matchActTemplate);
      activityCreationTemplate.basedOn(this.desiredActTemplate);
      if (this.startExpr != null) {
        Interval startTimeRange = null;
        startTimeRange = this.startExpr.computeTime(simulationResults, plan, window.interval());
        //startTimeRange = this.startExpr.computeTimeRelativeAbsolute(simulationResults, plan, window.interval(), !(this.createPersistentAnchor || this.allowActivityUpdate));
        activityFinder.startsIn(startTimeRange);
        activityCreationTemplate.startsIn(startTimeRange);
      }
      if (this.endExpr != null) {
        Interval endTimeRange = null;
        endTimeRange = this.endExpr.computeTime(simulationResults, plan, window.interval());
        activityFinder.endsIn(endTimeRange);
        activityCreationTemplate.endsIn(endTimeRange);
      }
      /* this will override whatever might be already present in the template */
      if (durExpr != null) {
        var durRange = this.durExpr.compute(window.interval(), simulationResults);
        activityFinder.durationIn(durRange);
        activityCreationTemplate.durationIn(durRange);
      }

      final var activitiesFound = plan.find(
          activityFinder.build(),
          simulationResults,
          createEvaluationEnvironmentFromAnchor(evaluationEnvironment, window));

      var planEvaluation = plan.getEvaluation();
      var associatedActivitiesToThisGoal = planEvaluation.forGoal(this).getAssociatedActivities();
      var alreadyOneActivityAssociated = false;
      for (var act : activitiesFound) {
        //has already been associated to this goal
        if (associatedActivitiesToThisGoal.contains(act)) {
          alreadyOneActivityAssociated = true;
          break;
        }
      }
      if (!alreadyOneActivityAssociated) {
        SchedulingActivityDirectiveId anchorIdTo = null;
        if (window.value().isPresent() && mapSchedulingIdsToActivityIds.isPresent()){
          ActivityDirectiveId actId = new ActivityDirectiveId(window.value().get().activityInstance().id);
          anchorIdTo = mapSchedulingIdsToActivityIds.get().inverseBidiMap().get(actId);
        }
        var missingActAssociationsWithAnchor = new ArrayList<SchedulingActivityDirective>();
        var missingActAssociationsWithoutAnchor = new ArrayList<SchedulingActivityDirective>();
        /*
        If activities that can satisfy the goal have been found, then create two arraylist to distinguish between:
         1) those activities that also satisfy the anchoring  (e.g. anchorId value equals the SchedulingActivityDirectiveId of the "for each" activity directive in the goal
         2) activities without the anchorId set
         */
        for (var act : activitiesFound) {
          if (planEvaluation.canAssociateMoreToCreatorOf(act)) {
            if (anchorIdTo != null && act.anchorId() != null && act.anchorId().id() == anchorIdTo.id())
              missingActAssociationsWithAnchor.add(act);
            else
              missingActAssociationsWithoutAnchor.add(act);
          }
        }

        /* The truth table that determines the type of conflict is shown below. The variables considered in the table are:
        1. allowReuseExistingActivity (user specified): True if the user allows to reuse activities already existing in the plan to satisfy the goal
        2. allowActivityUpdate (user specified): True if the user allows the scheduler to modify activities already existing in the plan to satisfy the goal.
        The modification consists on adding an anchor if necessary and making its starting time relative to the goal activity directive to which it will be anchored
        3. missingActAssociationsWithAnchor: True if there are activities in the plan that can be directly associated (without requiring any modification) to satisfy the goal
        4. missingActAssociationsWithoutAnchor: True if there are activities in the plan that can be associated to satisfy the goal, but require to be modified by adding to them the anchor.

        allowReuseExistingActivity	allowActivityUpdate	missingActAssociationsWithAnchor	missingActAssociationsWithoutAnchor 	type conflict
              0	                      0	                  0	                                0	                              MissingActivityTemplateConflict
              0	                      0	                  0	                                1	                              MissingActivityTemplateConflict
              0	                      0	                  1	                                0	                              MissingActivityTemplateConflict
              0	                      0	                  1	                                1	                              MissingActivityTemplateConflict
              0	                      1	                  0	                                0	                              MissingActivityTemplateConflict
              0	                      1	                  0	                                1	                              MissingActivityTemplateConflict
              0	                      1	                  1	                                0	                              MissingActivityTemplateConflict
              0	                      1	                  1	                                1	                              MissingActivityTemplateConflict
              1	                      0	                  0	                                0	                              MissingActivityTemplateConflict
              1	                      0	                  0	                                1	                              MissingActivityTemplateConflict
              1	                      0	                  1	                                0	                              MissingAssociationConflict(this, missingActAssociationsWithoutAnchor,  Optional.empty())
              1	                      0	                  1	                                1	                              MissingAssociationConflict(this, missingActAssociationsWithoutAnchor,  Optional.empty())
              1	                      1	                  0	                                0	                              MissingActivityTemplateConflict
              1	                      1	                  0	                                1	                              MissingAssociationConflict(this, missingActAssociationsWithoutAnchor,  Optional.of(anchorIdTo))
              1	                      1	                  1	                                0	                              MissingAssociationConflict(this, missingActAssociationsWithoutAnchor,  Optional.empty())
              1	                      1	                  1	                                1	                              MissingAssociationConflict(this, missingActAssociationsWithoutAnchor,  Optional.empty())
         */


        /* Conditions for MissingActivityTemplateConflict. Cover cases in which allowReuseExistingActivity is false, meaning that the scheduler always needs to create a new activity to satisfy the goal
        Cases: 0 x x x
        1. The user doesn't allow to use existing matching activities
        2. There are no activities that match the activity template
        3. The user doesn't allow to update existing matching activities without anchor and there exists matching activities without anchor
        */
        if(!allowReuseExistingActivity || (missingActAssociationsWithAnchor.isEmpty() && missingActAssociationsWithoutAnchor.isEmpty()) || (!allowActivityUpdate && missingActAssociationsWithAnchor.isEmpty())) {
          conflicts.add(new MissingActivityTemplateConflict(
              this,
              this.temporalContext.evaluate(simulationResults, evaluationEnvironment),
              activityCreationTemplate.build(),
              createEvaluationEnvironmentFromAnchor(evaluationEnvironment, window),
              1,
              anchorIdTo == null ? Optional.empty() : Optional.of(anchorIdTo),
              Optional.of(this.startExpr.getAnchor().equals(TimeAnchor.START)),
              Optional.empty()
          ));
        }

        /* Condition for MissingAssociationConflict with the anchorId in its parameters.
        Case: 1 1 0 1
        1. The user allows to update matching activities without anchors from the plan and there are only matching activities without anchors
        Notice that it has been implicitly checked in the previous if that allowReuseExistingActivity is 1 and that missingActAssociationsWithoutAnchor is not empty. If it were empty,
         as we are checking in this condition that missingActAssociationsWithAnchor is also empty, we would have entered in the previous if
        */
        else if(allowActivityUpdate && missingActAssociationsWithAnchor.isEmpty()){
          conflicts.add(new MissingAssociationConflict(this, missingActAssociationsWithoutAnchor,
                                                       anchorIdTo == null ? Optional.empty() : Optional.of(anchorIdTo),
                                                       Optional.of(this.startExpr.getAnchor().equals(TimeAnchor.START)))
          );
        }

        /* Condition fort MissingAssociationConflict without any anchorId in its parameters. Final condition for remaining cases, in which it is allowed to reuse matching activities already in the plan and there are activities with anchors already in the plan.
        Case: 1 x 1 x
         */
        else{
          conflicts.add(new MissingAssociationConflict(this, missingActAssociationsWithoutAnchor,
                                                       Optional.empty(),
                                                       Optional.of(this.startExpr.getAnchor().equals(TimeAnchor.START)))
          );
        }
      }
    }
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

  /**
   * ctor creates an empty goal without details
   *
   * client code should use builders to instance goals
   */
  protected CoexistenceGoal() { }
}
