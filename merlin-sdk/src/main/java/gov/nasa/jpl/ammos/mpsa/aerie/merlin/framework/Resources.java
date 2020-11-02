package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.Schema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class Resources<$Schema, Event>
    implements gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Resources<$Schema, Event>
{
  private final Schema.Builder<$Schema, Event> builder;
  private final Map<String, RealResource<History<? extends $Schema, ?>>> realResources;
  private final Map<String, Pair<ValueSchema, Resource<History<? extends $Schema, ?>, SerializedValue>>> discreteResources;

  public Resources(final Schema.Builder<$Schema, Event> builder) {
    this.builder = builder;
    this.realResources = new HashMap<>();
    this.discreteResources = new HashMap<>();
  }

  protected <Effect, ModelType extends Model<Effect, ModelType>>
  Query<$Schema, ModelType>
  model(final ModelType initialState, final Function<Event, Effect> interpreter)
  {
    return this.builder.register(initialState, interpreter);
  }

  protected <ModelType, ResourceType>
  DiscreteResource<History<? extends $Schema, ?>, ResourceType>
  resource(final String name,
           final Query<$Schema, ? extends ModelType> model,
           final DiscreteResource<ModelType, ResourceType> resource,
           final ValueMapper<ResourceType> mapper)
  {
    return resource(name, resource.connect(model), mapper);
  }

  protected <ResourceType>
  DiscreteResource<History<? extends $Schema, ?>, ResourceType>
  resource(final String name,
           final DiscreteResource<History<? extends $Schema, ?>, ResourceType> resource,
           final ValueMapper<ResourceType> mapper)
  {
    this.discreteResources.put(name, Pair.of(mapper.getValueSchema(), resource.map(mapper::serializeValue)));
    return resource;
  }

  protected <ModelType>
  RealResource<History<? extends $Schema, ?>>
  resource(final String name,
           final Query<$Schema, ModelType> model,
           final RealResource<ModelType> resource)
  {
    return resource(name, resource.connect(model));
  }

  protected
  RealResource<History<? extends $Schema, ?>>
  resource(final String name,
           final RealResource<History<? extends $Schema, ?>> resource)
  {
    this.realResources.put(name, resource);
    return resource;
  }

  public Schema<$Schema, Event> getSchema() {
    return this.builder.build();
  }

  @Override
  public Map<String, ? extends Resource<History<? extends $Schema, ?>, RealDynamics>> getRealResources() {
    return Collections.unmodifiableMap(this.realResources);
  }

  @Override
  public Map<String, ? extends Pair<ValueSchema, ? extends Resource<History<? extends $Schema, ?>, SerializedValue>>> getDiscreteResources() {
    return Collections.unmodifiableMap(this.discreteResources);
  }
}
