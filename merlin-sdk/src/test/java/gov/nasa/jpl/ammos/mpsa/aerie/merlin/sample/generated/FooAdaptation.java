package gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.BuiltResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Module;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ProxyContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.sample.generated.activities.TaskSpec;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

// TODO: Automatically generate at compile time.
public final class FooAdaptation<$Schema> implements Adaptation<$Schema, TaskSpec> {
  private final ProxyContext<$Schema> rootContext = new ProxyContext<>();

  private final FooResources<$Schema> container;
  private final BuiltResources<$Schema> resources;
  private final Map<String, TaskSpecType<$Schema, TaskSpec>> daemonTypes;
  private final Map<String, TaskSpecType<$Schema, TaskSpec>> allTaskSpecTypes;

  public FooAdaptation(final Schema.Builder<$Schema> schemaBuilder) {
    final var builder = new ResourcesBuilder<>(schemaBuilder);
    final var container = new FooResources<>(builder);
    container.setContext(this.rootContext);

    final var allTaskSpecTypes = new HashMap<>(TaskSpec.getTaskSpecTypes(this.rootContext, container));
    final var daemonTypes = new HashMap<String, TaskSpecType<$Schema, TaskSpec>>();

    getDaemons("/daemons", container, (name, daemon) -> {
      final var daemonType = TaskSpec.createDaemonType(name, daemon, this.rootContext);

      daemonTypes.put(daemonType.getName(), daemonType);
      allTaskSpecTypes.put(daemonType.getName(), daemonType);
    });

    this.container = container;
    this.resources = builder.build();
    this.daemonTypes = Collections.unmodifiableMap(daemonTypes);
    this.allTaskSpecTypes = Collections.unmodifiableMap(allTaskSpecTypes);
  }

  private static void getDaemons(
      final String namespace,
      final Module<?> module,
      final BiConsumer<String, Runnable> receiver)
  {
    for (final var daemon : module.getDaemons().entrySet()) {
      receiver.accept(namespace + "/" + daemon.getKey(), daemon.getValue());
    }

    for (final var submodule : module.getSubmodules().entrySet()) {
      getDaemons(namespace + "/" + submodule.getKey(), submodule.getValue(), receiver);
    }
  }

  @Override
  public Map<String, TaskSpecType<$Schema, TaskSpec>> getTaskSpecificationTypes() {
    return this.allTaskSpecTypes;
  }

  @Override
  public Iterable<Pair<String, Map<String, SerializedValue>>> getDaemons() {
    return this.daemonTypes
        .values()
        .stream()
        .map(x -> Pair.of(x.getName(), Map.<String, SerializedValue>of()))
        .collect(Collectors.toList());
  }

  @Override
  public Map<String, ? extends Pair<ValueSchema, ? extends Resource<History<? extends $Schema>, SerializedValue>>> getDiscreteResources() {
    return this.resources.getDiscreteResources();
  }

  @Override
  public Map<String, ? extends Resource<History<? extends $Schema>, RealDynamics>> getRealResources() {
    return this.resources.getRealResources();
  }

  @Override
  public Schema<$Schema> getSchema() {
    return this.resources.getSchema();
  }
}
