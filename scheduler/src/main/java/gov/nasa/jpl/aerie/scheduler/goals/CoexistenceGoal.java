package gov.nasa.jpl.aerie.scheduler.goals;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplateDisjunction;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.constraints.durationexpressions.DurationExpression;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateConstraintExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeAnchor;
import gov.nasa.jpl.aerie.scheduler.constraints.timeexpressions.TimeExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.TimeRangeExpression;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityInstanceConflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingAssociationConflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.UnsatisfiableMissingActivityConflict;

import java.util.ArrayList;

/**
 * describes the desired coexistence of an activity with another
 */
public class CoexistenceGoal extends ActivityTemplateGoal {

  private TimeExpression startExpr;
  private TimeExpression endExpr;
  private DurationExpression durExpr;

  /**
   * the builder can construct goals piecemeal via a series of method calls
   */
  public static class Builder extends ActivityTemplateGoal.Builder<Builder> {

    public Builder forEach(ActivityExpression actExpr) {
      forEach = new TimeRangeExpression.Builder().from(actExpr).build();
      return getThis();
    }

    public Builder forEach(StateConstraintExpression scExpr) {
      forEach = new TimeRangeExpression.Builder().from(scExpr).build();
      return getThis();
    }

    public Builder forEach(TimeRangeExpression expr) {
      forEach = expr;
      return getThis();
    }

    protected TimeRangeExpression forEach;

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
  public java.util.Collection<Conflict> getConflicts(Plan plan) {
    final var conflicts = new java.util.LinkedList<Conflict>();

    Windows anchors = expr.computeRange(plan, Windows.forever());
    for (var window : anchors) {

      boolean disj = false;
      ActivityExpression.AbstractBuilder actTB = null;
      if (this.desiredActTemplate instanceof ActivityCreationTemplateDisjunction) {
        disj = true;
        actTB = new ActivityCreationTemplateDisjunction.OrBuilder();
      } else if (this.desiredActTemplate != null) {
        actTB = new ActivityCreationTemplate.Builder();
      }
      actTB.basedOn(this.desiredActTemplate);

      if(this.startExpr != null) {
        Window startTimeRange = null;
        startTimeRange = this.startExpr.computeTime(plan, window);
        actTB.startsIn(startTimeRange);
      }
      if(this.endExpr != null) {
        Window endTimeRange = null;
        endTimeRange = this.endExpr.computeTime(plan, window);
        actTB.endsIn(endTimeRange);
      }
      /* this will override whatever might be already present in the template */
      if(durExpr!=null){
        var durRange = this.durExpr.compute(window);
        actTB.durationIn(Window.between(durRange, durRange));
      }

      ActivityCreationTemplate temp;
      if (disj) {
        temp = (ActivityCreationTemplateDisjunction) actTB.build();
      } else {
        temp = (ActivityCreationTemplate) actTB.build();

      }
      final var existingActs = plan.find(temp);

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
          final var actName = getName() + "_" + java.util.UUID.randomUUID();
          ActivityInstance act;
          var stateConstraints = getStateConstraints();
          var temporalContext = new Windows(this.temporalContext);
          if (getStateConstraints() != null) {
            var valid = stateConstraints.findWindows(plan, temporalContext);
            act = temp.createActivity(actName, valid, false);
          } else {
            act = temp.createActivity(actName, temporalContext, false);
          }
          if (act == null) {
            conflicts.add(new UnsatisfiableMissingActivityConflict(this));
          } else {
            conflicts.add(new MissingActivityInstanceConflict(this, act));
          }
          //  conflicts.add( new MissingActivityTemplateConflict(
          //          this, TimeWindows.of( startTimeRange ) ) );
        } else {
          conflicts.add(new MissingAssociationConflict(this, missingActAssociations));
          //REVIEW: will need to record associations to check for joint/sole ownership,
          //        but that assignment will itself be a combinatoric problem
          //REVIEW: should record determined associations more permanent, eg for UI
        }
      }

    }//for(anchorAct)

    return conflicts;
  }

  /**
   * ctor creates an empty goal without details
   *
   * client code should use builders to instance goals
   */
  protected CoexistenceGoal() { }

  /**
   * the pattern used to locate anchor activity instances in the plan
   */
  protected TimeRangeExpression expr;


}
