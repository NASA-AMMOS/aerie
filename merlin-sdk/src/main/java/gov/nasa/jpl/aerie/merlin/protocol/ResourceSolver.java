package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.time.Duration;
import gov.nasa.jpl.aerie.time.Window;

import java.util.Optional;

/**
 * A solver over a continuous dynamics for a resource type and conditions against that resource.
 *
 * <p>
 *   A dynamics is a continuous function from time (the real line) into a space of resource values (the resource type).
 *   A condition is a continuous function from the resource space into the space of boolean valuations,
 *   also called <a href="https://en.wikipedia.org/wiki/Sierpi%C5%84ski_space">Sierpiński space</a>.
 * </p>
 *
 * <p>
 *   A solver is able to perform two kinds of computations: solve a dynamics for its value at a time,
 *   and solve the composition of a dynamics and a condition -- in sum, a continuous function from time to truth values
 *   -- for the windows of time over which the condition is true.
 * </p>
 *
 * @param <Resource> The topological space of resource values under discussion.
 * @param <Dynamics> The type of continuous time-dependent resource behaviors.
 * @param <Condition> The type of Boolean conditions on resource values.
 */
public interface ResourceSolver<$Schema, Resource,  /*->*/ Dynamics, Condition> {
  DelimitedDynamics<Dynamics> getDynamics(Resource resource, History<? extends $Schema> now);

  <Result> Result approximate(ApproximatorVisitor<Dynamics, Result> visitor);

  Optional<Duration> firstSatisfied(Dynamics dynamics, Condition condition, Window selection);
  Optional<Duration> firstDissatisfied(Dynamics dynamics, Condition condition, Window selection);

  interface ApproximatorVisitor<Dynamics, Result> {
    Result real(RealApproximator<Dynamics> approximator);
    Result discrete(DiscreteApproximator<Dynamics> approximator);
  }
}
