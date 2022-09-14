package gov.nasa.jpl.aerie.scheduler.constraints.scheduling;

import gov.nasa.jpl.aerie.constraints.model.SimulationResults;
import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Segment;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.scheduler.constraints.activities.ActivityExpression;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.conflicts.Conflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityInstanceConflict;
import gov.nasa.jpl.aerie.scheduler.conflicts.MissingActivityTemplateConflict;

import java.util.List;
import java.util.stream.Collectors;

public class BinaryMutexConstraint extends GlobalConstraint {

  ActivityType actType;
  ActivityType otherActType;

  public static BinaryMutexConstraint buildMutexConstraint(ActivityType type1, ActivityType type2) {
    BinaryMutexConstraint mc = new BinaryMutexConstraint();
    mc.fill(type1, type2);
    return mc;
  }

  protected void fill(ActivityType type1, ActivityType type2) {
    this.actType = type1;
    this.otherActType = type2;
  }


  public Windows findWindows(Plan plan, Windows windows, Conflict conflict, SimulationResults simulationResults) {
    if (conflict instanceof MissingActivityInstanceConflict) {
      return findWindows(plan, windows, ((MissingActivityInstanceConflict) conflict).getInstance().getType(), simulationResults);
    } else if (conflict instanceof MissingActivityTemplateConflict) {
      return findWindows(plan, windows, ((MissingActivityTemplateConflict) conflict).getGoal().getActTemplate().getType(), simulationResults);
    } else {
      throw new IllegalArgumentException("method implemented for two types of conflict");
    }
  }


  private Windows findWindows(Plan plan, Windows windows, ActivityType actToBeScheduled, SimulationResults simulationResults) {
    Windows validWindows = new Windows(windows);
    if (!(actToBeScheduled.equals(actType) || actToBeScheduled.equals(otherActType))) {
      //not concerned by this constraint
      return validWindows;
    }
    ActivityType actToBeSearched = actToBeScheduled.equals(actType) ? otherActType : actType;
    final var actSearch = new ActivityExpression.Builder()
        .ofType(actToBeSearched).build();

    final var acts = new java.util.LinkedList<>(plan.find(actSearch, simulationResults));
    List<Interval> rangesActs = acts.stream().map(a -> Interval.betweenClosedOpen(a.getStartTime(), a.getEndTime())).collect(
        Collectors.toList());

    validWindows = validWindows.set(rangesActs, false);

    return validWindows;
  }


  //Non-incremental checking
  //TODO: does not help finding where to put acts
  @Override
  public ConstraintState isEnforced(Plan plan, Windows windows, SimulationResults simulationResults) {

    Windows violationWindows = new Windows(Interval.FOREVER, false);


    for (final var interval: windows.iterateEqualTo(true)) {
      final var actSearch = new ActivityExpression.Builder()
          .ofType(actType).startsOrEndsIn(interval).build();
      final var otherActSearch = new ActivityExpression.Builder()
          .ofType(otherActType).startsOrEndsIn(interval).build();
      final var acts = new java.util.LinkedList<>(plan.find(actSearch, simulationResults));
      final var otherActs = new java.util.LinkedList<>(plan.find(otherActSearch, simulationResults));

      List<Interval> rangesActs = acts.stream().map(a -> Interval.betweenClosedOpen(a.getStartTime(), a.getEndTime())).toList();
      Windows twActs = new Windows(false).set(rangesActs, true);
      List<Interval> rangesOtherActs = otherActs
          .stream()
          .map(a -> Interval.betweenClosedOpen(a.getStartTime(), a.getEndTime()))
          .toList();
      Windows twOtherActs = new Windows(false).set(rangesOtherActs, true);

      final var intervalWindows = new Windows(false).set(interval, true);
      //intersection with current interval to be sure we are not analyzing intersections happenning outside
      violationWindows = new Windows(twActs).and(twOtherActs).and(intervalWindows).or(violationWindows);
    }
    ConstraintState cState;
    if (!violationWindows.stream().noneMatch(Segment::value)) {
      cState = new ConstraintState(this, true, violationWindows);
    } else {
      cState = new ConstraintState(this, false, null);
    }
    return cState;
  }

}
