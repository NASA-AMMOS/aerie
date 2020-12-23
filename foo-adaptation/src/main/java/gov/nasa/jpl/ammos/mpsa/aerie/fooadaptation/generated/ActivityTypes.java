package gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.activities.FooActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated.mappers.FooActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ThreadedTask;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.DynamicCell;

import java.util.HashMap;
import java.util.Map;

// TODO: Automatically generate at compile time.
public final class ActivityTypes {
  private ActivityTypes() {}

  public static <$Schema>
  Map<String, TaskSpecType<$Schema, ?>>
  get(final DynamicCell<Context<$Schema>> rootContext, final FooResources<$Schema> container)
  {
    final var activityTypes = new HashMap<String, TaskSpecType<$Schema, ?>>();

    {
      final var type = new ActivityType<$Schema, FooActivity>(new FooActivityMapper()) {
        @Override
        public <$Timeline extends $Schema> Task<$Timeline> createTask(final FooActivity activity) {
          return new ThreadedTask<>(
              rootContext,
              () -> activity
                  .new EffectModel<$Schema>()
                  .runWith(rootContext.get(), container));
        }
      };
      activityTypes.put(type.getName(), type);
    }

    return activityTypes;
  }
}
