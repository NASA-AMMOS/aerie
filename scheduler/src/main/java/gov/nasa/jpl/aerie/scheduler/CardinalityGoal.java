package gov.nasa.jpl.aerie.scheduler;

import java.util.*;

/**
 * describes the desired coexistence of an activity with another
 */
public class CardinalityGoal extends ActivityTemplateGoal {
  /**
   * minimum/maximum total duration of activities
   */
  private Range<Duration> durationRange;

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

    Range<Duration> durationRange;
    Range<Integer> occurrenceRange;

    public Builder duration(Range<Duration> durationRange) {
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


      return goal;
    }

  }//Builder

  //GREEDY STRATEGIES FOR [durmin, durmax] for acts in [damin, damax]
  // 1)MIN_ACTS
  // for each window in decreasing order : schedule acts with maximization of duration for each

  //2) SPREADED
  //binary search of time ?


  /**
   * {@inheritDoc}
   *
   * collects conflicts wherein a matching anchor activity was found
   * but there was no corresponding target activity instance (and one
   * should probably be created!)
   */
  public Collection<Conflict> getConflicts(Plan plan) {
    final var conflicts = new LinkedList<Conflict>();

    TimeWindows range = this.expr.computeRange(plan, TimeWindows.spanMax());


    ActivityCreationTemplate actTB =
        new ActivityCreationTemplate.Builder().basedOn(this.desiredActTemplate).startsOrEndsIn(range).build();

    final var acts = new LinkedList<>(plan.find(actTB));
    acts.sort(Comparator.comparing(ActivityInstance::getStartTime));

    int nbActs = acts.size();
    Duration total = Duration.ofZero();
    for (var act : acts) {
      total = total.plus(act.getDuration());
    }

    StateConstraintExpression actConstraints = this.getActivityStateConstraints();
    StateConstraintExpression goalConstraints = this.getStateConstraints();

    Duration durToSchedule = Duration.ofSeconds(0);
    int nbToSchedule = 0;
    if (this.durationRange != null && !this.durationRange.contains(total)) {
      if (total.compareTo(this.durationRange.getMinimum()) < 0) {

        durToSchedule = this.durationRange.getMinimum().minus(total);
        //TODO:below, tranform into other type of conflict
        //not enough duration
        //conflicts.add(new DurationTooSmallConflict(this, getTemporalContext(), this.durationRange, total));
      } else if (total.compareTo(this.durationRange.getMaximum()) > 0) {
        throw new IllegalArgumentException("Need to decrease duration of activities from the plan, impossible");
        //too much duration
        //conflicts.add(new DurationTooBigConflict(this, getTemporalContext(), this.durationRange, total));

      }
    }
    if (this.occurrenceRange != null && !this.occurrenceRange.contains(nbActs)) {
      if (nbActs < this.occurrenceRange.getMinimum()) {
        // not enough occurrence
        //conflicts.add(new NotEnoughOccurrencesConflict(this, getTemporalContext(), this.occurrenceRange, nbActs));


        nbToSchedule = this.occurrenceRange.getMinimum() - nbActs;


      } else if (nbActs > this.occurrenceRange.getMaximum()) {
        throw new IllegalArgumentException("Need to remove activities from the plan, impossible");
        //too much occurrences
        //conflicts.add(new TooManyOccurrencesConflict(this, getTemporalContext(), this.occurrenceRange,nbActs));
      }

    }

    return getConflictsDurAndOccur(plan, durToSchedule, nbToSchedule);
  }


  private Collection<Conflict> getConflictsDurOnly(Plan plan) {
    return null;
  }


  public Range<Time> binaryBreadth(Time start, Time end, List<Range<Time>> ranges) {
    return null;
  }


  //GREEDY STRATEGY for [nmin,nmax] and [durmin, durmax] for acts in [damin, damax]
  //schedule nmin acts of damin
  //nmin is now good
  //extend acts one by one to reach durmin
  //if durmin is not attained and if we have attained damax for all acts : schedule k (k-1 actually and then switch to exact dur)
  // acts of damax  until durmin is attained

  public Collection<Conflict> getConflictsDurAndOccur(Plan plan, Duration durToSchedule, int nbToSchedule) {
    TimeWindows range = this.expr.computeRange(plan, TimeWindows.spanMax());
    final var conflicts = new LinkedList<Conflict>();


    StateConstraintExpression actConstraints = this.getActivityStateConstraints();
    StateConstraintExpression goalConstraints = this.getStateConstraints();
    Range<Duration> actPossibleDurations = this.desiredActTemplate.getDurationRange();

    //TODO: what if there is no duration range ?
    TimeWindows rangeAfterConstraints = range;
    if (actConstraints != null) {
      var actCstWins = actConstraints.findWindows(plan, range);
      rangeAfterConstraints.intersection(actCstWins);
    }
    if (goalConstraints != null) {
      var goalCstWins = goalConstraints.findWindows(plan, rangeAfterConstraints);
      rangeAfterConstraints.intersection(goalCstWins);
    }

    var rangesForSchedulings = rangeAfterConstraints.getRangeSet();
    Iterator<Range<Time>> itRanges = rangesForSchedulings.iterator();

    Map<Range<Time>, List<ActivityInstance>> instancesCreated = new TreeMap<Range<Time>, List<ActivityInstance>>();

    for (var r : rangesForSchedulings) {
      instancesCreated.put(r, new ArrayList<ActivityInstance>());
    }

    Duration scheduledDur = Duration.ofZero();
    int scheduled = 0;
    Range<Time> curRange = itRanges.next();
    var start = curRange.getMinimum();
    //while there is still missing some duration
    while (scheduled < nbToSchedule) {
      //if the current window can welcome an activity
      var durWindow = curRange.getMaximum().minus(curRange.getMinimum());

      if (durWindow.compareTo(actPossibleDurations.getMinimum()) >= 0) {
        //schedule the smallest activity possible
        var dur = actPossibleDurations.getMinimum();
        ActivityCreationTemplate.Builder builderAct = new ActivityCreationTemplate.Builder();
        builderAct.basedOn(this.desiredActTemplate);
        builderAct.startsIn(new Range<Time>(start));
        var inst = builderAct.duration(dur).build().createActivity("act_" + scheduled);
        instancesCreated.get(curRange).add(inst);
        scheduled += 1;
        scheduledDur = scheduledDur.plus(dur);
        durWindow = durWindow.minus(dur);
        start = start.plus(dur);
      } else {
        curRange = itRanges.next();
        start = curRange.getMinimum();
      }

    }
    Duration maxDurActs = actPossibleDurations.getMaximum();

    //if we extend all activities to their maximum dur, can we attain the objective ? else, we have to insert new activities
    var possibleByJustExtending = (maxDurActs.minus(actPossibleDurations.getMinimum()).times(nbToSchedule)).compareTo(
        durToSchedule.minus(scheduledDur)) >= 0;

    if (scheduledDur.compareTo(durToSchedule) < 0) {

      for (int i = 0; i < rangesForSchedulings.size(); i++) {
        var win = rangesForSchedulings.get(i);
        List<ActivityInstance> acts = instancesCreated.get(win);
        if (acts.size() > 0) {
          //extend last by maximum
          var lastAct = acts.get(acts.size() - 1);
          if (lastAct.getEndTime().compareTo(win.getMaximum()) < 0 && lastAct.getDuration().compareTo(maxDurActs) < 0) {
            //there is room for extension
            //and the act is not extended at the max
            //we extend it at the max
            var maxIncrease1 = win.getMaximum().minus(lastAct.getEndTime());
            var maxIncrease2 = maxDurActs.minus(lastAct.getDuration());
            //we have to take the minimum
            var increase = maxIncrease1.compareTo(maxIncrease2) < 0 ? maxIncrease1 : maxIncrease2;
            lastAct.setDuration(lastAct.getDuration().plus(increase));
            scheduledDur = scheduledDur.plus(increase);
          }

          if (scheduledDur.compareTo(durToSchedule) >= 0) {
            break;
          }
        }
      }
    }
    //if duration is still needed, we schedule more activities with max dur
    itRanges = rangesForSchedulings.iterator();
    curRange = itRanges.next();
    while (scheduledDur.compareTo(durToSchedule) < 0) {
      //if the current window can welcome an activity
      var listActsScheduled = instancesCreated.get(curRange);
      if (listActsScheduled.size() > 0) {
        var lastAct = listActsScheduled.get(listActsScheduled.size() - 1);
        start = lastAct.getEndTime();
      }
      var durWindow = curRange.getMaximum().minus(start);

      if (durWindow.compareTo(actPossibleDurations.getMinimum()) >= 0) {
        //schedule the biggest activity possible to avoiding hitting maximum occurrence
        var dur = actPossibleDurations.getMaximum().compareTo(durWindow) < 0 ? maxDurActs : durWindow;
        ActivityCreationTemplate.Builder builderAct = new ActivityCreationTemplate.Builder();
        builderAct.basedOn(this.desiredActTemplate);
        builderAct.startsIn(new Range<Time>(start));
        var inst = builderAct.duration(dur).build().createActivity("act_" + scheduled);
        instancesCreated.get(curRange).add(inst);
        scheduled += 1;
        scheduledDur = scheduledDur.plus(dur);
        durWindow = durWindow.minus(dur);
        start = start.plus(dur);
      } else {
        if (itRanges.hasNext()) {
          curRange = itRanges.next();
          start = curRange.getMinimum();
        } else {
          break;
        }
      }
    }
    for (var a : instancesCreated.entrySet()) {
      for (var act : a.getValue()) {
        conflicts.add(new MissingActivityInstanceConflict(this, act));
      }
    }

    //System.out.println(instancesCreated);
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
