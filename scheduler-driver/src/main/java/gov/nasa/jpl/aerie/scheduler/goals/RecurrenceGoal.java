package gov.nasa.jpl.aerie.scheduler.goals;

import gov.nasa.jpl.aerie.constraints.model.EvaluationEnvironment;
import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerModel;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityConflict;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityTemplateConflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingAssociationConflict;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * describes the desired recurrence of an activity every time period
 */
public class RecurrenceGoal extends ActivityTemplateGoal {

  /**
   * A tuple-like type to store two allowable durations for the recurrence interval - a minimum and a maximum,
   * which will eventually be supported by the scheduler. Default behavior for now should be to set min and max equal.
   * @param min the minimum allowable duration to use (for the recurrence interval)
   * @param max the maximum allowable duration to use (for the recurrence interval)
   */
  public record MinMaxAllowableRecurrenceInterval(Duration min, Duration max) {}

  /**
   * the builder can construct goals piecemeal via a series of method calls
   */
  public static class Builder extends ActivityTemplateGoal.Builder<Builder> {

    /**
     * specifies the fixed interval over which the activity should repeat
     *
     * this interval is understood to be a minimum (ie more frequent repetition
     * does not impede the goal's satisfaction)
     *
     * this specifier is required. it replaces any previous specification.
     *
     * @param interval IN the duration over which a matching activity instance
     *     must exist in order to satisfy the goal
     * @return this builder, ready for additional specification
     */
    public Builder repeatingEvery(Duration interval) {
      this.every = new MinMaxAllowableRecurrenceInterval(interval, interval);
      return getThis();
    }

    protected MinMaxAllowableRecurrenceInterval every;

    /**
     * {@inheritDoc}
     */
    @Override
    public RecurrenceGoal build() { return fill(new RecurrenceGoal()); }

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
    protected RecurrenceGoal fill(RecurrenceGoal goal) {
      //first fill in any general specifiers from parents
      super.fill(goal);

      if (every == null) {
        throw new IllegalArgumentException("creating recurrence goal requires non-null \"every\" duration interval");
      }
      if (every.min.isNegative()) {
        throw new IllegalArgumentException("Duration passed to RecurrenceGoal as the goal's minimum recurrence interval cannot be negative!");
      }
      else if (every.max.isNegative()) {
        throw new IllegalArgumentException("Duration passed to RecurrenceGoal as the goal's maximum recurrence interval cannot be negative!");
      }
      goal.recurrenceInterval = every;

      if(name==null){
        goal.name = "RecurrenceGoal_one_"+this.thereExists.type().getName()+"_every_["+goal.recurrenceInterval.min+","+goal.recurrenceInterval.max+"]";
      }

      return goal;
    }

  }//Builder


  /**
   * {@inheritDoc}
   *
   * collects conflicts wherein a matching target activity instance does not
   * exist over a timespan longer than the allowed range (and one should
   * probably be created!)
   */
  @Override
  public java.util.Collection<Conflict> getConflicts(
      @NotNull final Plan plan,
      final SimulationResults simulationResults,
      final EvaluationEnvironment evaluationEnvironment,
      final SchedulerModel schedulerModel) {
    final var conflicts = new java.util.LinkedList<Conflict>();

    //unwrap temporalContext
    final var tempWindowPlanHorizon = new Windows(false).set(List.of(this.planHorizon.getHor()), true);
    final var windows = tempWindowPlanHorizon.and(this.getTemporalContext().evaluate(simulationResults, evaluationEnvironment));

    //check repeat is larger than activity duration
    if(this.getActTemplate().type().getDurationType() instanceof DurationType.Fixed act){
      boolean durActVSRec = act.duration().longerThan(this.recurrenceInterval.min);
      if(durActVSRec){
        throw new UnexpectedTemporalContextChangeException("The goal is unsatisfiable as its duration is longer than the repeat duration");
      }
    }

    //make sure it hasn't changed
    if (this.initiallyEvaluatedTemporalContext != null && !windows.includes(this.initiallyEvaluatedTemporalContext)) {
      throw new UnexpectedTemporalContextChangeException("The temporalContext Windows has changed from: " + this.initiallyEvaluatedTemporalContext.toString() + " to " + windows);
    }
    else if (this.initiallyEvaluatedTemporalContext == null) {
      this.initiallyEvaluatedTemporalContext = windows;
    }

    //iterate through it and then within each iteration do exactly what you did before
    for (Interval subInterval : windows.iterateEqualTo(true)) {
      //collect all matching target acts ordered by start time
      //REVIEW: could collapse with prior template start time query too?
      final var satisfyingActSearch = new ActivityExpression.Builder()
          .basedOn(matchActTemplate)
          .startsIn(subInterval)
          .build();
      final var acts = new java.util.LinkedList<>(plan.find(satisfyingActSearch, simulationResults, new EvaluationEnvironment()));
      acts.sort(java.util.Comparator.comparing(SchedulingActivityDirective::startOffset));

      //walk through existing matching activities to find too-large gaps,
      //starting from the goal's own start time
      //REVIEW: some clever implementation with stream reduce / combine?
      final var actI = acts.iterator();
      final var lastStartT = subInterval.end;
      var prevStartT = subInterval.start;
      while (actI.hasNext() && prevStartT.compareTo(lastStartT) < 0) {
        final var act = actI.next();
        final var actStartT = act.startOffset();

        //check if the inter-activity gap is too large
        //REVIEW: should do any check based on min gap duration?
        final var strideDur = actStartT.minus(prevStartT);
        if (strideDur.compareTo(this.recurrenceInterval.max) > 0) {
          //fill conflicts for all the missing activities in that long span
          conflicts.addAll(makeRecurrenceConflicts(prevStartT, actStartT, evaluationEnvironment));

        } else {
          /*TODO: right now, we associate with all the activities that are satisfying but we should aim for the minimum
          set which itself is a combinatoric problem */
          var planEval = plan.getEvaluation();
          if (!planEval.forGoal(this).getAssociatedActivities().contains(act) && planEval.canAssociateMoreToCreatorOf(
              act)) {
            conflicts.add(new MissingAssociationConflict(this, List.of(act)));
          }
        }

        prevStartT =
            actStartT.plus(recurrenceInterval.max); //if we dont do this sum it'll just restart where the last event started and keep adding an instance of the last event. it required you to go out of bounds on the last event which was a problem, so this accommodates.
      }

      //fill in conflicts for all missing activities in the last span up to the
      //goal's own end time (also handles case of no matching acts at all)
      conflicts.addAll(makeRecurrenceConflicts(prevStartT, lastStartT, evaluationEnvironment));
    }

    return conflicts;
  }

  /**
   * ctor creates an empty goal without details
   *
   * client code should use builders to instance goals
   */
  protected RecurrenceGoal() { }

  /**
   * the allowable range of durations over which a target activity must repeat
   * Of type: MinMaxAllowableRecurrenceInterval
   *
   * REVIEW: need to work out semantics of min on recurrenceInterval
   */
  protected MinMaxAllowableRecurrenceInterval recurrenceInterval;

  /**
   * creates conflicts for missing activities in the provided span
   *
   * there may be more than one conflict if the span is larger than
   * the maximum allowed recurrence interval. there may be no conflicts
   * if the span is less than the maximum interval.
   *
   * @param start IN the start time of the span to fill with conflicts (inclusive)
   * @param end IN the end time of the span to fill with conflicts (exclusive)
   */
  private java.util.Collection<MissingActivityConflict> makeRecurrenceConflicts(Duration start, Duration end, final EvaluationEnvironment evaluationEnvironment)
  {
    final var conflicts = new java.util.LinkedList<MissingActivityConflict>();

    for (var intervalT = start.plus(recurrenceInterval.max);
         ;
         intervalT = intervalT.plus(recurrenceInterval.max)
    ) {
      final var windows = new Windows(false).set(Interval.betweenClosedOpen(intervalT.minus(recurrenceInterval.max), Duration.min(intervalT, end)), true);
      if(windows.iterateEqualTo(true).iterator().hasNext()){
        conflicts.add(new MissingActivityTemplateConflict(this, windows, this.getActTemplate(), evaluationEnvironment, 1, Optional.empty()));
      }
      if(intervalT.compareTo(end) >= 0){
        break;
      }
    }

    return conflicts;
  }
}
