package gov.nasa.jpl.aerie.scheduler.goals;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.conflicts.UnsatisfiableMissingActivityConflict;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityCreationTemplate;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.Range;
import gov.nasa.jpl.aerie.scheduler.constraints.resources.StateConstraintExpression;
import gov.nasa.jpl.aerie.scheduler.constraints.TimeRangeExpression;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityTemplateConflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingAssociationConflict;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * describes the desired coexistence of an activity with another
 */
public class CardinalityGoal extends ActivityTemplateGoal {

  private static final Logger logger = LoggerFactory.getLogger(CardinalityGoal.class);

  /**
   * minimum/maximum total duration of activities
   */
  private Window durationRange;

  /*
   *minimum/maximum number of occurrence of activities
   *  */
  private Range<Integer> occurrenceRange;

  /**
   * the pattern used to locate anchor activity instances in the plan
   */
  protected TimeRangeExpression expr;


  /**
   * the builder can construct goals piecemeal via a series of method calls
   */
  public static class Builder extends ActivityTemplateGoal.Builder<Builder> {

    public Builder inPeriod(ActivityExpression actExpr) {
      inPeriod = new TimeRangeExpression.Builder().from(actExpr).build();
      return getThis();
    }

    public Builder inPeriod(StateConstraintExpression scExpr) {
      inPeriod = new TimeRangeExpression.Builder().from(scExpr).build();
      return getThis();
    }

    public Builder inPeriod(TimeRangeExpression expr) {
      inPeriod = expr;
      return getThis();
    }

    protected TimeRangeExpression inPeriod;

    Window durationRange;
    Range<Integer> occurrenceRange;

    public Builder duration(Window durationRange) {
      this.durationRange = durationRange;
      return getThis();
    }

    public Builder occurences(Range<Integer> occurrenceRange) {
      this.occurrenceRange = occurrenceRange;
      return getThis();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CardinalityGoal build() { return fill(new CardinalityGoal()); }

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
    protected CardinalityGoal fill(CardinalityGoal goal) {
      //first fill in any general specifiers from parents
      super.fill(goal);

      if (inPeriod == null) {
        throw new IllegalArgumentException(
            "creating Cardinality goal requires non-null \"inPeriod\" anchor template");
      }
      goal.expr = inPeriod;

      if (durationRange != null) {
        goal.durationRange = durationRange;

      }
      if (occurrenceRange != null) {
        goal.occurrenceRange = occurrenceRange;
      }
      if(isUnsatisfiable()){
        throw new IllegalArgumentException("Goal is incorrectly parametrized and therefore unsatisfiable : minimum duration x minimum occurence > maximum duration");
      }
      return goal;
    }

    public boolean isUnsatisfiable(){
      if(this.durationRange != null && occurrenceRange != null && this.thereExists.getDurationRange() != null){
        if(this.thereExists.getDurationRange().start.times(occurrenceRange.getMinimum()).longerThan(durationRange.end)){
          return true;
        }
      }
      return false;
    }

  }//Builder


  /**
   * {@inheritDoc}
   *
   * collects conflicts wherein a matching anchor activity was found
   * but there was no corresponding target activity instance (and one
   * should probably be created!)
   */
  public Collection<Conflict> getConflicts(Plan plan) {
    Windows timeDomain = this.expr.computeRange(plan, Windows.forever());
    ActivityCreationTemplate actTB =
        new ActivityCreationTemplate.Builder().basedOn(this.desiredActTemplate).startsOrEndsIn(timeDomain).build();

    final var acts = new LinkedList<>(plan.find(actTB));
    acts.sort(Comparator.comparing(ActivityInstance::getStartTime));

    int nbActs = 0;
    Duration total = Duration.ZERO;
    var planEvaluation = plan.getEvaluation();
    var associatedActivitiesToThisGoal = planEvaluation.forGoal(this).getAssociatedActivities();
    for (var act : acts) {
      if (planEvaluation.canAssociateMoreToCreatorOf(act) || associatedActivitiesToThisGoal.contains(act)) {
        total = total.plus(act.getDuration());
        nbActs++;
      }
    }

    Duration durToSchedule = Duration.ZERO;
    int nbToSchedule = 0;
    if (this.durationRange != null && !this.durationRange.contains(total)) {
      if (total.compareTo(this.durationRange.start) < 0) {
        durToSchedule = this.durationRange.start.minus(total);
      } else if (total.compareTo(this.durationRange.end) > 0) {
        logger.warn("Need to decrease duration of activities from the plan, impossible because scheduler cannot remove activities");
        return List.of(new UnsatisfiableMissingActivityConflict(this));
      }
    }
    if (this.occurrenceRange != null && !this.occurrenceRange.contains(nbActs)) {
      if (nbActs < this.occurrenceRange.getMinimum()) {
        nbToSchedule = this.occurrenceRange.getMinimum() - nbActs;
      } else if (nbActs > this.occurrenceRange.getMaximum()) {
        logger.warn("Need to remove activities from the plan to satify cardinality goal, impossible");
        return List.of(new UnsatisfiableMissingActivityConflict(this));
      }
    }

    final var conflicts = new LinkedList<Conflict>();
    //at this point, have thrown exception if not satisfiable
    //compute the missing association conflicts
    for(var act:acts){
      if(!associatedActivitiesToThisGoal.contains(act) && planEvaluation.canAssociateMoreToCreatorOf(act)){
        //they ALL have to be associated
        conflicts.add(new MissingAssociationConflict(this, List.of(act)));
      }
    }
    //1) solve occurence part, we just need a certain number of activities
    for(int i = 0; i<nbToSchedule;i++){
      conflicts.add(new MissingActivityTemplateConflict(this, new Windows(timeDomain), this.desiredActTemplate));
    }
    /*
     * 2) solve duration part: we can't assume stuff about duration, we post one conflict. The scheduler will solve this conflict by inserting one
     * activity then the conflict will be reevaluated and if the scheduled duration so far is less than needed, another
     * conflict will be posted and so on
     * */
    if(nbToSchedule == 0 && durToSchedule.isPositive()){
      conflicts.add(new MissingActivityTemplateConflict(this, new Windows(timeDomain), this.desiredActTemplate));
    }

    return conflicts;
  }

  /**
   * /**
   * ctor creates an empty goal without details
   *
   * client code should use builders to instance goals
   */
  protected CardinalityGoal() { }


}
