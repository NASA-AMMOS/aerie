package gov.nasa.jpl.aerie.merlin.driver;

import gov.nasa.jpl.aerie.merlin.protocol.AdaptationFactory;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.Task;
import gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.protocol.TaskStatus;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Adaptation<$Schema> {
  private final Schema<$Schema> schema;
  private final List<ResourceFamily<$Schema, ?>> resourceFamilies;
  private final Map<String, TaskSpecType<$Schema, ?>> taskSpecTypes;
  private final List<AdaptationFactory.TaskFactory<$Schema>> daemons;

  public Adaptation(
      final Schema<$Schema> schema,
      final List<ResourceFamily<$Schema, ?>> resourceFamilies,
      final List<AdaptationFactory.TaskFactory<$Schema>> daemons,
      final Map<String, TaskSpecType<$Schema, ?>> taskSpecTypes)
  {
    this.schema = Objects.requireNonNull(schema);
    this.resourceFamilies = Collections.unmodifiableList(resourceFamilies);
    this.taskSpecTypes = Collections.unmodifiableMap(taskSpecTypes);
    this.daemons = Collections.unmodifiableList(daemons);
  }

  public Map<String, TaskSpecType<$Schema, ?>> getTaskSpecificationTypes() {
    return this.taskSpecTypes;
  }

  public <$Timeline extends $Schema> Task<$Timeline> getDaemon() {
    return scheduler -> {
      this.daemons.forEach(daemon -> scheduler.spawn(daemon.create()));
      return TaskStatus.completed();
    };
  }

  public Iterable<ResourceFamily<$Schema, ?>> getResourceFamilies() {
    return this.resourceFamilies;
  }

  public Schema<$Schema> getSchema() {
    return this.schema;
  }
}
