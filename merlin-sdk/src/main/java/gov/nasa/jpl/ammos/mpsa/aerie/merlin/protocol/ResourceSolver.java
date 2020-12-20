package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.DelimitedDynamics;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.Optional;

public interface ResourceSolver<$Schema, Resource,  /*->*/ Dynamics, Condition> {
  DelimitedDynamics<Dynamics> getDynamics(Resource resource, History<? extends $Schema> now);
  Approximator<Dynamics> getApproximator();

  Optional<Duration> firstSatisfied(Dynamics dynamics, Condition condition, Window selection);
}
