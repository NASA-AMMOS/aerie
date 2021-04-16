package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BuiltAdaptation<$Schema> implements Adaptation<$Schema> {
  private final Schema<$Schema> schema;
  private final List<ResourceFamily<$Schema, ?>> resourceFamilies;
  private final Map<String, TaskSpecType<$Schema, ?>> taskSpecTypes;
  private final List<Context.TaskFactory> daemons;

  public BuiltAdaptation(
      final Schema<$Schema> schema,
      final List<ResourceFamily<$Schema, ?>> resourceFamilies,
      final List<Context.TaskFactory> daemons,
      final Map<String, TaskSpecType<$Schema, ?>> taskSpecTypes)
  {
    this.schema = Objects.requireNonNull(schema);
    this.resourceFamilies = Collections.unmodifiableList(resourceFamilies);
    this.taskSpecTypes = Collections.unmodifiableMap(taskSpecTypes);
    this.daemons = Collections.unmodifiableList(daemons);
  }

  @Override
  public Map<String, TaskSpecType<$Schema, ?>> getTaskSpecificationTypes() {
    return this.taskSpecTypes;
  }

  @Override
  public <$Timeline extends $Schema> Task<$Timeline> getDaemon() {
    return scheduler -> {
      this.daemons.forEach(daemon -> scheduler.spawn(daemon.create()));
      return TaskStatus.completed();
    };
  }

  @Override
  public Iterable<ResourceFamily<$Schema, ?>> getResourceFamilies() {
    return this.resourceFamilies;
  }

  @Override
  public Schema<$Schema> getSchema() {
    return this.schema;
  }
}
