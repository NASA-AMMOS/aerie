package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Interval;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.constraints.tree.ActivitySpan;
import gov.nasa.jpl.aerie.constraints.tree.ForEachActivitySpans;
import gov.nasa.jpl.aerie.constraints.tree.Not;
import gov.nasa.jpl.aerie.constraints.tree.Or;
import gov.nasa.jpl.aerie.constraints.tree.WindowsFromSpans;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.ActivityType;
import gov.nasa.jpl.aerie.scheduler.model.Plan;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingCondition;

import java.time.Instant;
import java.util.List;

public class TestUtility {

  public static boolean activityStartingAtTime(Plan plan, Duration time, ActivityType activityType) {
    List<ActivityInstance> acts = plan.getActivitiesByTime();
    for (ActivityInstance act : acts) {
      if (act.getType().equals(activityType) && act.startTime().compareTo(time) == 0) {
        return true;
      }
    }
    return false;
  }

  public static boolean containsActivity(Plan plan, Duration startTime, Duration endTime, ActivityType activityType) {
    List<ActivityInstance> acts = plan.getActivitiesByTime();
    for (ActivityInstance act : acts) {
      if (act.getType().equals(activityType) &&
          act.startTime().compareTo(startTime) == 0 &&
          act.getEndTime().compareTo(endTime) == 0) {
        return true;
      }
    }
    return false;
  }
  /**
   * Returns true if there is at least one activity of type activityType in timewindows tw in the plan
   */
  public static boolean atLeastOneActivityOfTypeInTW(Plan plan, Windows tw, ActivityType activityType) {
    for (Interval interval : tw.iterateEqualTo(true)) {
      if (!atLeastOneActivityOfTypeInRange(plan, interval, activityType)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if there is at least one activity of type activityType in the time range tw in the plan
   */
  public static boolean atLeastOneActivityOfTypeInRange(Plan plan, Interval interval, ActivityType activityType) {

    List<ActivityInstance> acts = plan.getActivitiesByTime();
    for (ActivityInstance act : acts) {
      if (act.getType().equals(activityType)
          && act.startTime().compareTo(interval.start) >= 0
          && act.getEndTime().compareTo(interval.end) <= 0) {
        return true;
      }
    }
    return false;
  }

  public static List<SchedulingCondition> createAutoMutexGlobalSchedulingCondition(final ActivityType activityType) {
    return List.of(
        new SchedulingCondition(
            new Not(
                new Or(
                    new WindowsFromSpans(
                        new ForEachActivitySpans(
                            activityType.getName(),
                            "span activity alias 0",
                            new ActivitySpan("span activity alias 0"))
                    )
                )
            ),
            List.of(activityType)),
        new SchedulingCondition(
            new Not(
                new Or(
                    new WindowsFromSpans(
                        new ForEachActivitySpans(
                            activityType.getName(),
                            "span activity alias 1",
                            new ActivitySpan("span activity alias 1"))
                    )
                )
            ),
            List.of(activityType)
        )
    );
  }

  /**
   * Returns true if plan does not contain specific activity instance act
   */
  public static boolean doesNotContainActivity(Plan plan, ActivityInstance act) {
    for (ActivityInstance actI : plan.getActivitiesByTime()) {
      if (actI.equals(act)) {
        return false;
      }
    }
    return true;
  }

    /**
   * Returns true if plan contains specific activity instance act
   */
  public static boolean containsExactlyActivity(Plan plan, ActivityInstance act) {
    for (ActivityInstance actI : plan.getActivitiesByTime()) {
      if (actI.equalsInProperties(act)) {
        return true;
      }
    }
    return false;
  }

  public static Instant timeFromEpochMillis(long millis){
    return Instant.ofEpochMilli(millis);
  }
  public static Instant timeFromEpochSeconds(int seconds){
    return timeFromEpochMillis(seconds * 1000L);
  }

}
