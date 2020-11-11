package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Resources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Schema;

import java.util.Map;

// TODO: Automatically generate at compile time.
public final class FooAdaptation<$Schema>
    implements Adaptation<$Schema, FooEvent, FooActivityInstance<$Schema>>
{
  private final FooResources<$Schema> container;
  private final Resources<$Schema, FooEvent> resources;

  private FooAdaptation(final FooResources<$Schema> container, final Resources<$Schema, FooEvent> resources) {
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

  @Override
  public Resources<$Schema, FooEvent> getResources() {
    return this.resources;
  }

  @Override
  public Map<String, ActivityType<FooActivityInstance<$Schema>>> getActivityTypes() {
    return FooActivityInstance.getActivityTypes();
  }

  public @Override
  <$Timeline extends $Schema>
  Task<$Timeline, FooEvent, FooActivityInstance<$Schema>>
  createActivityTask(final FooActivityInstance<$Schema> activity)
  {
    return new FooTask<>(this.container, activity);
  }
}
