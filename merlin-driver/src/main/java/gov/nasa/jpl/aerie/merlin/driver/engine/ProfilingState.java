package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceSolver;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

/*package-local*/ record ProfilingState<$Schema, Dynamics>(
    Resource<$Schema, ?, Dynamics> getter,
    Profile<Dynamics> profile
) {
  public static <$Schema, ResourceType, DynamicsType>
  ProfilingState<$Schema, DynamicsType> create(
      final ResourceType getter,
      final ResourceSolver<$Schema, ResourceType, DynamicsType> solver
  ) {
    return new ProfilingState<>(new Resource<>(solver, getter), new Profile<>());
  }

  public void append(final Duration currentTime, final Querier<? extends $Schema> querier) {
    this.profile.append(currentTime, this.getter.get(querier));
  }
}
