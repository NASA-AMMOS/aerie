package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Schema;

import java.util.Map;

// TODO: Automatically generate at compile time.
public final class FooAdaptation<$Schema>
    implements Adaptation<$Schema, FooEvent, FooActivityInstance>
{
  private final FooResources<$Schema> container;
  private final gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Resources<$Schema, FooEvent> resources;

  private FooAdaptation(
      final FooResources<$Schema> container,
      final gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Resources<$Schema, FooEvent> resources)
  {
    this.container = container;
    this.resources = resources;
  }

  public static FooAdaptation<?> create() {
    return create(Schema.<FooEvent>builder());
  }

  private static <$Schema> FooAdaptation<$Schema> create(final Schema.Builder<$Schema, FooEvent> builder) {
    final ResourcesBuilder<$Schema, FooEvent> resourcesBuilder = new ResourcesBuilder<>(builder);
    final var container = new FooResources<>(resourcesBuilder);
    return new FooAdaptation<>(container, resourcesBuilder.build());
  }

  public @Override
  gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Resources<$Schema, FooEvent>
  getResources()
  {
    return this.resources;
  }

  public @Override
  Map<String, ActivityType<FooActivityInstance>>
  getActivityTypes()
  {
    return FooActivityInstance.getActivityTypes();
  }

  public @Override
  <$Timeline extends $Schema>
  FooTask<$Timeline>
  createActivityTask(FooActivityInstance activity)
  {
    return new FooTask<>(this.container, activity);
  }
}
