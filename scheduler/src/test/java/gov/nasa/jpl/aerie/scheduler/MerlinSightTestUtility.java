package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerModel;

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

  public static MissionModel<?> getMerlinSightMissionModel(){
    final var builder = new MissionModelBuilder();
    final var configuration = gov.nasa.jpl.aerielander.config.Configuration.defaultConfiguration();
    final var factory = new gov.nasa.jpl.aerielander.generated.GeneratedMissionModelFactory();
    final var registry = DirectiveTypeRegistry.extract(factory);
    final var model = factory.instantiate(registry.registry(), configuration, builder);
    final var mission = builder.build(model, factory.getConfigurationType(), registry.taskSpecTypes());
    return mission;
   //return null;
  }

  static SchedulerModel getMerlinSightSchedulerModel() {
    return new gov.nasa.jpl.aerielander.generated.GeneratedSchedulerModel();
  }
}
