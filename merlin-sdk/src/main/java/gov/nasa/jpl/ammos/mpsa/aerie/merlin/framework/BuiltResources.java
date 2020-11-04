package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/* package-local */
final class BuiltResources<$Schema, Event>
    implements gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Resources<$Schema, Event>
{
  private final Schema<$Schema, Event> schema;
  private final Map<String, Resource<History<? extends $Schema, ?>, RealDynamics>> realResources;
  private final Map<String, Pair<ValueSchema, Resource<History<? extends $Schema, ?>, SerializedValue>>>
      discreteResources;

  public BuiltResources(
      final Schema<$Schema, Event> schema,
      final Map<String, Resource<History<? extends $Schema, ?>, RealDynamics>> realResources,
      final Map<String, Pair<ValueSchema, Resource<History<? extends $Schema, ?>, SerializedValue>>> discreteResources)
  {
    this.schema = Objects.requireNonNull(schema);
    this.realResources = Objects.requireNonNull(realResources);
    this.discreteResources = Objects.requireNonNull(discreteResources);
  }

  @Override
  public Schema<$Schema, Event> getSchema() {
    return this.schema;
  }

  @Override
  public Map<String, ? extends Pair<ValueSchema, ? extends Resource<History<? extends $Schema, ?>, SerializedValue>>> getDiscreteResources() {
    return Collections.unmodifiableMap(this.discreteResources);
  }

  @Override
  public Map<String, ? extends Resource<History<? extends $Schema, ?>, RealDynamics>> getRealResources() {
    return Collections.unmodifiableMap(this.realResources);
  }
}
