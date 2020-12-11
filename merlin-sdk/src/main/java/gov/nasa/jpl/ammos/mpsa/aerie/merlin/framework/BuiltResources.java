package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class BuiltResources<$Schema> {
  public final Schema<$Schema> schema;
  public final Map<String, Resource<$Schema, RealDynamics>> realResources;
  public final Map<String, Pair<ValueSchema, Resource<$Schema, SerializedValue>>> discreteResources;
  public final Map<String, Runnable> daemons;

  public BuiltResources(
      final Schema<$Schema> schema,
      final Map<String, Resource<$Schema, RealDynamics>> realResources,
      final Map<String, Pair<ValueSchema, Resource<$Schema, SerializedValue>>> discreteResources,
      final Map<String, Runnable> daemons)
  {
    this.schema = Objects.requireNonNull(schema);
    this.realResources = Collections.unmodifiableMap(realResources);
    this.discreteResources = Collections.unmodifiableMap(discreteResources);
    this.daemons = Collections.unmodifiableMap(daemons);
  }
}
