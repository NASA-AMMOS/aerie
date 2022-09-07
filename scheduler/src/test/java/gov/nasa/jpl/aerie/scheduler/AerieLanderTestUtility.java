package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerModel;
import gov.nasa.jpl.aerie.scheduler.model.ActivityInstance;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.time.Instant;
import java.util.List;

/**
 * Some utility functions used in tests
 */
public class AerieLanderTestUtility {
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
    final var model = factory.instantiate(Instant.EPOCH, configuration, builder);
    return builder.build(model, registry);
  }

  static SchedulerModel getMerlinSightSchedulerModel() {
    return new gov.nasa.jpl.aerielander.generated.GeneratedSchedulerModel();
  }
}
