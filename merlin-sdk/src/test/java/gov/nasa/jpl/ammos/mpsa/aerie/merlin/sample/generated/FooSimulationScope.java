package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.BuiltResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ProxyContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ReplayingTask;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SimulationScope;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities.TaskSpec;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public final class FooSimulationScope<$Schema> implements SimulationScope<$Schema, TaskSpec> {
  private final ProxyContext<$Schema, TaskSpec> rootContext = new ProxyContext<>();

  private final FooResources<$Schema> container;
  private final BuiltResources<$Schema> resources;

  private FooSimulationScope(final FooResources<$Schema> container, final BuiltResources<$Schema> resources) {
    this.container = container;
    this.resources = resources;

    container.setContext(this.rootContext);
  }

  private static <$Schema> FooSimulationScope<$Schema> create(final ResourcesBuilder<$Schema> builder) {
    final var container = new FooResources<>(builder);
    return new FooSimulationScope<>(container, builder.build());
  }

  public static FooSimulationScope<?> create() {
    return create(new ResourcesBuilder<>(Schema.builder()));
  }

  public Module<$Schema, TaskSpec> getRootModule() {
    return this.container;
  }

  @Override
  public Schema<$Schema> getSchema() {
    return this.resources.getSchema();
  }

  @Override
  public Map<String, ? extends Pair<ValueSchema, ? extends Resource<History<? extends $Schema>, SerializedValue>>>
  getDiscreteResources()
  {
    return this.resources.getDiscreteResources();
  }

  @Override
  public Map<String, ? extends Resource<History<? extends $Schema>, RealDynamics>>
  getRealResources()
  {
    return this.resources.getRealResources();
  }

  public @Override
  <$Timeline extends $Schema>
  Task<$Timeline, TaskSpec>
  createTask(final TaskSpec taskSpec)
  {
    final var task = taskSpec.<$Schema>createTask();
    task.setContext(this.rootContext);
    return new ReplayingTask<>(this.rootContext, () -> task.run(this.container));
  }
}
