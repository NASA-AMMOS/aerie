package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Condition;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class BuiltResources<$Schema> {
  public final Schema<$Schema> schema;
  public final Map<String, RealResource<$Schema>> realResources;
  public final Map<String, Pair<ValueSchema, DiscreteResource<$Schema, SerializedValue>>> discreteResources;
  public final Map<String, Condition<$Schema>> constraints;
  public final Map<String, Runnable> daemons;

  public BuiltResources(
      final Schema<$Schema> schema,
      final Map<String, RealResource<$Schema>> realResources,
      final Map<String, Pair<ValueSchema, DiscreteResource<$Schema, SerializedValue>>> discreteResources,
      final Map<String, Condition<$Schema>> constraints,
      final Map<String, Runnable> daemons)
  {
    this.schema = Objects.requireNonNull(schema);
    this.realResources = Collections.unmodifiableMap(realResources);
    this.discreteResources = Collections.unmodifiableMap(discreteResources);
    this.constraints = Collections.unmodifiableMap(constraints);
    this.daemons = Collections.unmodifiableMap(daemons);
  }
}
