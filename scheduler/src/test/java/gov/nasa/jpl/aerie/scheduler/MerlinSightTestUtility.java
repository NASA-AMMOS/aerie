package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.insight.config.Configuration;
import gov.nasa.jpl.aerie.insight.generated.GeneratedMissionModelFactory;
import gov.nasa.jpl.aerie.insight.mappers.InSightValueMappers;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;

import java.util.List;

/**
 * Some utility functions used in tests
 */
public class MerlinSightTestUtility {
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
}
