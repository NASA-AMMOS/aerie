package gov.nasa.jpl.ammos.mpsa.aerie.banananation.generated;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.BanananationResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.BuiltResources;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.Context;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.DaemonTaskType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.ResourcesBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities.DynamicCell;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: Automatically generate at compile time.
public class Banananation<$Schema> implements Adaptation<$Schema> {
  private final DynamicCell<Context<$Schema>> rootContext = DynamicCell.create();

  private final BuiltResources<$Schema> resources;
  private final Map<String, TaskSpecType<$Schema, ?>> daemonTypes;
  private final Map<String, TaskSpecType<$Schema, ?>> allTaskSpecTypes;

  public Banananation(final Schema.Builder<$Schema> schemaBuilder) {
    final var builder = new ResourcesBuilder<>(this.rootContext, schemaBuilder);
    final var container = new BanananationResources<>(builder.getCursor());
    final var resources = builder.build();

    final var activityTypes = ActivityTypes.get(this.rootContext, container);
    final var daemonTypes = new HashMap<String, TaskSpecType<$Schema, ?>>();

    resources.daemons.forEach((name, daemon) -> {
      final var daemonType = new DaemonTaskType<>("/daemons/" + name, daemon, this.rootContext);

      daemonTypes.put(daemonType.getName(), daemonType);
    });

    final var allTaskSpecTypes =
        new HashMap<String, TaskSpecType<$Schema, ?>>(activityTypes.size() + daemonTypes.size());
    allTaskSpecTypes.putAll(activityTypes);
    allTaskSpecTypes.putAll(daemonTypes);

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
