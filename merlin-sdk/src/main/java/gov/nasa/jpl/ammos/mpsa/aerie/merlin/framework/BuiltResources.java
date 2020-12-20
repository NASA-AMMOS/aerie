package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.ResourceFamily;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BuiltResources<$Schema> {
  public final Schema<$Schema> schema;
  public final List<ResourceFamily<$Schema, ?, ?>> resourceFamilies;
  public final Map<String, Condition<$Schema>> constraints;
  public final Map<String, Runnable> daemons;

  public BuiltResources(
      final Schema<$Schema> schema,
      final List<ResourceFamily<$Schema, ?, ?>> resourceFamilies,
      final Map<String, Condition<$Schema>> constraints,
      final Map<String, Runnable> daemons)
  {
    this.schema = Objects.requireNonNull(schema);
    this.resourceFamilies = Collections.unmodifiableList(resourceFamilies);
    this.constraints = Collections.unmodifiableMap(constraints);
    this.daemons = Collections.unmodifiableMap(daemons);
  }
}
