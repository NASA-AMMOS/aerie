package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.DirectiveTypeRegistry;
import gov.nasa.jpl.aerie.merlin.driver.MissionModel;
import gov.nasa.jpl.aerie.merlin.driver.MissionModelBuilder;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerModel;
import gov.nasa.jpl.aerie.scheduler.model.SchedulingActivityDirective;
import gov.nasa.jpl.aerie.scheduler.model.Plan;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Some utility functions used in tests
 */
public class AerieLanderTestUtility {
  public static void printPlan(Plan plan) {
    List<SchedulingActivityDirective> acts = plan.getActivitiesByTime();
    SchedulingActivityDirective last = null;
    for (var act : acts) {
      System.out.println(act.toString());
    }
  }

  public static MissionModel<?> getMerlinSightMissionModel(){
    final var builder = new MissionModelBuilder();
    final var configuration = gov.nasa.jpl.aerielander.config.Configuration.defaultConfiguration();
    final var modelType = new gov.nasa.jpl.aerielander.generated.GeneratedModelType();
    final var registry = DirectiveTypeRegistry.extract(modelType);
    final var model = modelType.instantiate(Instant.EPOCH, configuration, builder);
    return builder.build(model, registry, Map.of());
  }

  static SchedulerModel getMerlinSightSchedulerModel() {
    return new gov.nasa.jpl.aerielander.generated.GeneratedSchedulerModel();
  }
}
