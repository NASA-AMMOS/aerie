package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.SerializedValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BuiltAdaptation<$Schema> implements Adaptation<$Schema> {
  private final BuiltResources<$Schema> resources;
  private final Map<String, TaskSpecType<$Schema, ?>> taskSpecTypes = new HashMap<>();
  private final List<Pair<String, Map<String, SerializedValue>>> daemons = new ArrayList<>();

  public BuiltAdaptation(
      final DynamicCell<Context<$Schema>> rootContext,
      final BuiltResources<$Schema> resources,
      final Map<String, TaskSpecType<$Schema, ?>> activityTypes)
  {
    this.taskSpecTypes.putAll(activityTypes);

    resources.daemons.forEach((name, daemon) -> {
      final var daemonType = new DaemonTaskType<>("/daemons/" + name, daemon, rootContext);

      this.taskSpecTypes.put(daemonType.getName(), daemonType);
      this.daemons.add(Pair.of(daemonType.getName(), Map.of()));
    });

    this.resources = Objects.requireNonNull(resources);
  }

  @Override
  public Map<String, TaskSpecType<$Schema, ?>> getTaskSpecificationTypes() {
    return this.taskSpecTypes;
  }

  @Override
  public Iterable<Pair<String, Map<String, SerializedValue>>> getDaemons() {
    return this.daemons;
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
