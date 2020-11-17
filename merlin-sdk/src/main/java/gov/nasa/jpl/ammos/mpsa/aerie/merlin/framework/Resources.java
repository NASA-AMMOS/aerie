package gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.framework.models.Model;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.Query;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.discrete.DiscreteResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.real.RealResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;

import java.util.function.Function;

public abstract class Resources<$Schema, TaskSpec> extends Module<$Schema, TaskSpec> {
  private final ResourcesBuilder<$Schema> builder;

  public Resources(final ResourcesBuilder<$Schema> builder) {
    this.builder = builder;
  }

  protected <Event, Effect, ModelType extends Model<Effect, ModelType>>
  Query<$Schema, Event, ModelType>
  model(final ModelType initialState,
        final Function<Event, Effect> interpreter)
  {
    return this.builder.model(initialState, interpreter);
  }


  protected <ModelType, E extends Enum<E>>
  DiscreteResource<History<? extends $Schema>, E>
  resource(final String name,
           final Query<$Schema, ?, ? extends ModelType> model,
           final DiscreteResource<ModelType, E> resource)
  {
    return this.builder.enumerated(name, (history) -> resource.getDynamics(history.ask(model)))::getDynamics;
  }

  protected <E extends Enum<E>>
  DiscreteResource<History<? extends $Schema>, E>
  resource(final String name,
           final DiscreteResource<History<? extends $Schema>, E> resource)
  {
    return this.builder.enumerated(name, resource)::getDynamics;
  }


  protected <ModelType, ResourceType>
  DiscreteResource<History<? extends $Schema>, ResourceType>
  resource(final String name,
           final Query<$Schema, ?, ? extends ModelType> model,
           final DiscreteResource<ModelType, ResourceType> resource,
           final ValueMapper<ResourceType> mapper)
  {
    return this.builder.discrete(name, (history) -> resource.getDynamics(history.ask(model)), mapper)::getDynamics;
  }

  protected <ResourceType>
  DiscreteResource<History<? extends $Schema>, ResourceType>
  resource(final String name,
           final DiscreteResource<History<? extends $Schema>, ResourceType> resource,
           final ValueMapper<ResourceType> mapper)
  {
    return this.builder.discrete(name, resource, mapper)::getDynamics;
  }


  protected <ModelType>
  RealResource<History<? extends $Schema>>
  resource(final String name,
           final Query<$Schema, ?, ModelType> model,
           final RealResource<ModelType> resource)
  {
    return this.builder.real(name, (history) -> resource.getDynamics(history.ask(model)))::getDynamics;
  }

  protected
  RealResource<History<? extends $Schema>>
  resource(final String name,
           final RealResource<History<? extends $Schema>> resource)
  {
    return this.builder.real(name, resource)::getDynamics;
  }
}
