package gov.nasa.jpl.aerie.merlin.protocol;

/**
 * A solver over a continuous dynamics for a resource type.
 *
 * <p>
 *   A dynamics is a continuous function from time (the real line) into a space of resource values (the resource type).
 *   A condition is a continuous function from the resource space into the space of boolean valuations,
 *   also called <a href="https://en.wikipedia.org/wiki/Sierpi%C5%84ski_space">Sierpiński space</a>.
 * </p>
 *
 * @param <Resource> The topological space of resource values under discussion.
 * @param <Dynamics> The type of continuous time-dependent resource behaviors.
 */
public interface ResourceSolver<$Schema, Resource,  /*->*/ Dynamics> {
  Dynamics getDynamics(Resource resource, Querier<? extends $Schema> now);

  <Result> Result approximate(ApproximatorVisitor<Dynamics, Result> visitor);

  interface ApproximatorVisitor<Dynamics, Result> {
    Result real(RealApproximator<Dynamics> approximator);
    Result discrete(DiscreteApproximator<Dynamics> approximator);
  }
}
