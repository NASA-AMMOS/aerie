package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;

/**
 * describes the desired coexistence of an activity with another
 */
public class CoexistenceGoal extends ActivityTemplateGoal {

  private TimeExpression startExpr;
  private TimeExpression endExpr;

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

    public Builder startsAfter(TimeAnchor anchor) {
      startExpr = TimeExpression.afterStart();
      return getThis();
    }

    public Builder endsBefore(TimeAnchor anchor) {
      endExpr = TimeExpression.beforeEnd();
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

      //System.out.println("Anchor for coexist act : " + window.toString());

      //ActivityExpression.AbstractBuilder<?, ? extends ActivityCreationTemplate> actTB = this.desiredActTemplate.getNewBuilder();
      boolean disj = false;
      ActivityExpression.AbstractBuilder actTB = null;
      if (this.desiredActTemplate instanceof ActivityCreationTemplateDisjunction) {
        disj = true;
        actTB = new ActivityCreationTemplateDisjunction.OrBuilder();
      } else if (this.desiredActTemplate instanceof ActivityCreationTemplate) {
        actTB = new ActivityCreationTemplate.Builder();
      }

      //ActivityCreationTemplate.Builder actTB = new ActivityCreationTemplate.Builder();
      actTB.basedOn(this.desiredActTemplate);

      Window startTimeRange = null;


      if (this.startExpr != null && this.endExpr != null) {

        startTimeRange = this.startExpr.computeTime(plan, window);
        actTB.startsIn(startTimeRange);
        Window endTimeRange = this.endExpr.computeTime(plan, window);
        actTB.endsIn(endTimeRange);

      }
      //second part is useless but makes it more readeable
      else if (this.startExpr != null && this.endExpr == null) {
        startTimeRange = this.startExpr.computeTime(plan, window);
        actTB.startsIn(startTimeRange);
        Window rangeDur = desiredActTemplate.getDurationRange();
        if (rangeDur != null) {
          var endTime = Window.between(
              startTimeRange.start.plus(rangeDur.start),
              startTimeRange.end.plus(rangeDur.end));
          actTB.endsIn(endTime);
        }

      } else if (this.startExpr == null && this.endExpr != null) {
        Window endTimeRange = this.endExpr.computeTime(plan, window);
        actTB.endsIn(endTimeRange);
        Window rangeDur = desiredActTemplate.getDurationRange();
        if (rangeDur != null) {
          startTimeRange = Window.between(
              endTimeRange.start.minus(rangeDur.end),
              endTimeRange.end.minus(rangeDur.start));
          actTB.startsIn(startTimeRange);
        }

      } else {
        //all is null. default behavior is starts or ends in the interval
        startTimeRange = Window.between(
            window.start.minus(desiredActTemplate.getDurationRange().end),
            window.end);
        actTB.startsOrEndsIn(Window.between(startTimeRange.start, startTimeRange.end));
      }

      ActivityCreationTemplate temp;
      if (disj) {
        temp = (ActivityCreationTemplateDisjunction) actTB.build();
      } else {
        temp = (ActivityCreationTemplate) actTB.build();

      }
      final var existingActs = plan.find(temp);

      //TODO: enforcement of Solely custody strategy

      //create conflict if no matching target activity found
      if (existingActs.isEmpty()) {
        final var actName = getName() + "_" + java.util.UUID.randomUUID();
        ActivityInstance act;
        var stateConstraints = getStateConstraints();
        if (getStateConstraints() != null) {
          var valid = stateConstraints.findWindows(plan,Windows.forever());
          act = temp.createActivity(actName, valid);
        } else {
          act = temp.createActivity(actName, Windows.forever());

        }
        if (act == null) {
          conflicts.add(new UnsatisfiableMissingActivityConflict(this));
        } else {
          conflicts.add(new MissingActivityInstanceConflict(this, act));
        }
        //  conflicts.add( new MissingActivityTemplateConflict(
        //          this, TimeWindows.of( startTimeRange ) ) );
      } else {
        //REVIEW: will need to record associations to check for joint/sole ownership,
        //        but that assignment will itself be a combinatoric problem
        //REVIEW: should record determined associations more permanent, eg for UI
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
