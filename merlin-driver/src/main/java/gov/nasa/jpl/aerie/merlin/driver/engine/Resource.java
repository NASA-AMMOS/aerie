package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Querier;
import gov.nasa.jpl.aerie.merlin.protocol.model.ResourceSolver;

/*package-local*/
record Resource<$Schema, ResourceType, DynamicsType>(
    ResourceSolver<$Schema, ResourceType, DynamicsType> solver,
    ResourceType resource
) {
  public DynamicsType get(final Querier<? extends $Schema> querier) {
    return this.solver.getDynamics(this.resource, querier);
  }
}
