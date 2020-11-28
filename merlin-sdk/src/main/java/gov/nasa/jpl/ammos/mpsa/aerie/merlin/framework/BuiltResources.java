package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class BuiltResources<$Schema> {
  private final Schema<$Schema> schema;
  private final Map<String, Resource<History<? extends $Schema>, RealDynamics>> realResources;
  private final Map<String, Pair<ValueSchema, Resource<History<? extends $Schema>, SerializedValue>>> discreteResources;
  private final Map<String, Runnable> daemons;

  public BuiltResources(
      final Schema<$Schema> schema,
      final Map<String, Resource<History<? extends $Schema>, RealDynamics>> realResources,
      final Map<String, Pair<ValueSchema, Resource<History<? extends $Schema>, SerializedValue>>> discreteResources,
      final Map<String, Runnable> daemons)
  {
    this.schema = Objects.requireNonNull(schema);
    this.realResources = Objects.requireNonNull(realResources);
    this.discreteResources = Objects.requireNonNull(discreteResources);
    this.daemons = Objects.requireNonNull(daemons);
  }

  public Schema<$Schema> getSchema() {
    return this.schema;
  }

  public Map<String, ? extends Pair<ValueSchema, ? extends Resource<History<? extends $Schema>, SerializedValue>>> getDiscreteResources() {
    return Collections.unmodifiableMap(this.discreteResources);
  }

  public Map<String, ? extends Resource<History<? extends $Schema>, RealDynamics>> getRealResources() {
    return Collections.unmodifiableMap(this.realResources);
  }

  public Map<String, Runnable> getDaemons() {
    return Collections.unmodifiableMap(this.daemons);
  }
}
