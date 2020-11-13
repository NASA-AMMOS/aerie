package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ReplayingTask;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Resources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SimulationScope;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Schema;

public final class FooSimulationScope<$Schema> implements SimulationScope<$Schema, FooEvent, FooActivityInstance> {
  private final FooResources<$Schema> container;
  private final Resources<$Schema, FooEvent> resources;

  private FooSimulationScope(final FooResources<$Schema> container, final Resources<$Schema, FooEvent> resources) {
    this.container = container;
    this.resources = resources;
  }

  private static <$Schema> FooSimulationScope<$Schema> create(final ResourcesBuilder<$Schema, FooEvent> builder) {
    final var container = new FooResources<>(builder);
    return new FooSimulationScope<>(container, builder.build());
  }

  public static FooSimulationScope<?> create() {
    return create(new ResourcesBuilder<>(Schema.builder()));
  }

  @Override
  public Resources<$Schema, FooEvent> getResources() {
    return this.resources;
  }

  public @Override
  <$Timeline extends $Schema>
  Task<$Timeline, FooEvent, FooActivityInstance>
  createActivityTask(final FooActivityInstance activity)
  {
    return new ReplayingTask<>(this.container, activity::run);
  }
}
