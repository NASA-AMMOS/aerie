package gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.BanananationResources;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.BiteBananaActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.ParameterTestActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities.PeelBananaActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.mappers.BiteBananaActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.mappers.ParameterTestActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated.mappers.PeelBananaActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ThreadedTask;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.DynamicCell;

import java.util.HashMap;
import java.util.Map;

public final class ActivityTypes {
  private ActivityTypes() {}

  public static <$Schema>
  Map<String, TaskSpecType<$Schema, ?>>
  get(final DynamicCell<Context<$Schema>> rootContext, final BanananationResources<$Schema> container)
  {
    final var activityTypes = new HashMap<String, TaskSpecType<$Schema, ?>>();

    {
      final var type = new ActivityType<$Schema, BiteBananaActivity>(new BiteBananaActivityMapper()) {
        @Override
        public <$Timeline extends $Schema> Task<$Timeline> createTask(final BiteBananaActivity activity) {
          return new ThreadedTask<>(
              rootContext,
              () -> activity
                  .new EffectModel<$Schema>()
                  .runWith(rootContext.get(), container));
        }
      };
      activityTypes.put(type.getName(), type);
    }

    {
      final var type = new ActivityType<$Schema, PeelBananaActivity>(new PeelBananaActivityMapper()) {
        @Override
        public <$Timeline extends $Schema> Task<$Timeline> createTask(final PeelBananaActivity activity) {
          return new ThreadedTask<>(
              rootContext,
              () -> activity
                  .new EffectModel<$Schema>()
                  .runWith(rootContext.get(), container));
        }
      };
      activityTypes.put(type.getName(), type);
    }

    {
      final var type = new ActivityType<$Schema, ParameterTestActivity>(new ParameterTestActivityMapper()) {
        @Override
        public <$Timeline extends $Schema> Task<$Timeline> createTask(final ParameterTestActivity activity) {
          return $ -> TaskStatus.completed();
        }
      };
      activityTypes.put(type.getName(), type);
    }

    return activityTypes;
  }
}
