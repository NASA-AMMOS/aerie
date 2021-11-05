package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.model.Resource;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

/*package-local*/ record ProfilingState<$Schema, Dynamics> (
    Resource<$Schema, Dynamics> resource,
    Profile<Dynamics> profile
) {
  public static <$Schema, DynamicsType>
  ProfilingState<$Schema, DynamicsType> create(final Resource<$Schema, DynamicsType> resource) {
    return new ProfilingState<>(resource, new Profile<>());
  }

  public void append(final Duration currentTime, final Querier<? extends $Schema> querier) {
    this.profile.append(currentTime, this.resource.getDynamics(querier));
  }
}
