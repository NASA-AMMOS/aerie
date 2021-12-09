package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.constraints.time.Window;
import gov.nasa.jpl.aerie.constraints.time.Windows;
import gov.nasa.jpl.aerie.insight.config.Configuration;
import gov.nasa.jpl.aerie.insight.generated.GeneratedMissionModelFactory;
import gov.nasa.jpl.aerie.insight.mappers.InSightValueMappers;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.util.List;

/**
 * Some utility functions used in tests
 */
public class TestUtility {
  public static final int latest = 1;
  public static final String LOCAL_AERIE = "http://localhost:8080/v1/graphql";
  public static final int MISSION_MODEL_ID = latest;

  public static void printPlan(Plan plan) {

    List<ActivityInstance> acts = plan.getActivitiesByTime();
    ActivityInstance last = null;
    for (var act : acts) {
      System.out.println(act.toString());
    }

  }

  public static MissionModelWrapper getMerlinSightMissionModel(final PlanningHorizon horizon){
    final var builder = new MissionModelBuilder();
    final var configuration = InSightValueMappers.configuration().serializeValue(Configuration.defaultConfiguration());
    final var factory = new GeneratedMissionModelFactory();
    final var model = factory.instantiate(configuration, builder);
    final var mission = builder.build(model, factory.getTaskSpecTypes());
    return new MissionModelWrapper(mission,horizon);
  }

  public static boolean activityStartingAtTime(Plan plan, Duration time, ActivityType activityType) {

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
  public static boolean atLeastOneActivityOfTypeInTW(Plan plan, Windows tw, ActivityType activityType) {
    for (Window interval : tw) {
      if (!atLeastOneActivityOfTypeInRange(plan, interval, activityType)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if there is at least one activity of type activityType in the time range tw in the plan
   */
  public static boolean atLeastOneActivityOfTypeInRange(Plan plan, Window interval, ActivityType activityType) {

    List<ActivityInstance> acts = plan.getActivitiesByTime();
    for (ActivityInstance act : acts) {
      if (act.getType().equals(activityType)
          && act.getStartTime().compareTo(interval.start) >= 0
          && act.getEndTime().compareTo(interval.end) <= 0) {
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
