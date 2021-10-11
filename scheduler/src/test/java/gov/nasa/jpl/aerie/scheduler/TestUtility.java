package gov.nasa.jpl.aerie.scheduler;

import java.util.List;

/**
 * Some utility functions used in tests
 */
public class TestUtility {
  public static final String latest = "604a565e85af0954f98b9ee0";
  public static final String working = "5fd7ea8ed1b75c65c5fd10f4";


  public static final String LOCAL_AERIE = "http://localhost:27184";
  public static final String ADAPTATION_ID = latest;

  public static void visPlan(Plan plan) {
    PlanVisualizer.visualizePlan(plan);
    try {
      Thread.sleep(1000000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void printPlan(Plan plan) {

    List<ActivityInstance> acts = plan.getActivitiesByTime();
    ActivityInstance last = null;
    for (var act : acts) {
      System.out.println(act.toString());
    }

  }


  public static boolean activityStartingAtTime(Plan plan, Time time, ActivityType activityType) {

    List<ActivityInstance> acts = plan.getActivitiesByTime();
    for (ActivityInstance act : acts) {
      if (act.getType().equals(activityType) && act.getStartTime().compareTo(time) == 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if there is at least one activity of type activityType in timewindows tw in the plan
   */
  public static boolean atLeastOneActivityOfTypeInTW(Plan plan, TimeWindows tw, ActivityType activityType) {
    for (Range<Time> interval : tw.getRangeSet()) {
      if (!atLeastOneActivityOfTypeInRange(plan, interval, activityType)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if there is at least one activity of type activityType in the time range tw in the plan
   */
  public static boolean atLeastOneActivityOfTypeInRange(Plan plan, Range<Time> interval, ActivityType activityType) {

    List<ActivityInstance> acts = plan.getActivitiesByTime();
    for (ActivityInstance act : acts) {
      if (act.getType().equals(activityType)
          && act.getStartTime().compareTo(interval.getMinimum()) >= 0
          && act.getEndTime().compareTo(interval.getMaximum()) <= 0) {
        return true;
      }
    }
    return false;
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
      if (actI.equals(act)) {
        return true;
      }
    }
    return false;
  }


}
