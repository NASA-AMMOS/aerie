package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.Adaptation;
import gov.nasa.jpl.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.aerie.merlin.protocol.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType;
import gov.nasa.jpl.aerie.merlin.timeline.Schema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BuiltAdaptation<$Schema> implements Adaptation<$Schema> {
  private final Schema<$Schema> schema;
  private final List<ResourceFamily<$Schema, ?, ?>> resourceFamilies;
  private final Map<String, TaskSpecType<$Schema, ?>> taskSpecTypes;
  private final List<Pair<String, Map<String, SerializedValue>>> daemons;

  public BuiltAdaptation(
      final Schema<$Schema> schema,
      final List<ResourceFamily<$Schema, ?, ?>> resourceFamilies,
      final List<Pair<String, Map<String, SerializedValue>>> daemons,
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
  public Iterable<Pair<String, Map<String, SerializedValue>>> getDaemons() {
    return this.daemons;
  }

  @Override
  public Iterable<ResourceFamily<$Schema, ?, ?>> getResourceFamilies() {
    return this.resourceFamilies;
  }

  @Override
  public Map<String, Condition<$Schema>> getConstraints() {
    return Collections.emptyMap();
  }

  @Override
  public Schema<$Schema> getSchema() {
    return this.schema;
  }
}
