package gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.FooResources;
import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated.activities.DaemonTaskType;
import gov.nasa.jpl.ammos.mpsa.aerie.fooadaptation.generated.activities.FooActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.BuiltResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ProxyContext;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: Automatically generate at compile time.
public final class FooAdaptation<$Schema> implements Adaptation<$Schema> {
  private final ProxyContext<$Schema> rootContext = new ProxyContext<>();

  private final BuiltResources<$Schema> resources;
  private final Map<String, TaskSpecType<$Schema, ?>> daemonTypes;
  private final Map<String, TaskSpecType<$Schema, ?>> allTaskSpecTypes;

  public FooAdaptation(final Schema.Builder<$Schema> schemaBuilder) {
    final var builder = new ResourcesBuilder<>(this.rootContext, schemaBuilder);
    final var container = new FooResources<>(builder.getCursor());
    final var resources = builder.build();

    final var allTaskSpecTypes = new HashMap<String, TaskSpecType<$Schema, ?>>();
    {
      final var activityType = new FooActivityType<>(this.rootContext, container);
      allTaskSpecTypes.put(activityType.getName(), activityType);
    }

    final var daemonTypes = new HashMap<String, TaskSpecType<$Schema, ?>>();

    resources.daemons.forEach((name, daemon) -> {
      final var daemonType = new DaemonTaskType<>("/daemons/" + name, daemon, this.rootContext);

      daemonTypes.put(daemonType.getName(), daemonType);
      allTaskSpecTypes.put(daemonType.getName(), daemonType);
    });

    this.resources = resources;
    this.daemonTypes = Collections.unmodifiableMap(daemonTypes);
    this.allTaskSpecTypes = Collections.unmodifiableMap(allTaskSpecTypes);
  }

  @Override
  public Map<String, TaskSpecType<$Schema, ?>> getTaskSpecificationTypes() {
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
  public Iterable<ResourceFamily<$Schema, ?, ?>> getResourceFamilies() {
    return this.resources.resourceFamilies;
  }

  @Override
  public Map<String, Condition<$Schema>> getConstraints() {
    return this.resources.constraints;
  }

  @Override
  public Schema<$Schema> getSchema() {
    return this.resources.schema;
  }
}
