package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

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
public interface Solver<Resource, Dynamics, Condition> {
  /**
   * Determine the value of a resource under a dynamics at a given time.
   *
   * @param dynamics The dynamics governing the resource over time.
   * @param elapsedTime The time point at which to get a value.
   *
   * @return The resource value determined by the given dynamics at the given time.
   */
  Resource valueAt(Dynamics dynamics, Duration elapsedTime);

  /**
   * Determine the windows of time over which a dynamics holds a condition true.
   *
   * @param dynamics The dynamics governing the resource over time.
   * @param condition The condition assigning each resource value a truth value.
   *
   * @return The windows of time over which the condition is held true under the dynamics.
   */
  Windows whenSatisfied(Dynamics dynamics, Condition condition);

  /**
   * Determine the windows of time contained by a temporal scope over which a dynamics holds a condition true.
   *
   * <p>
   *   This should be overridden if there is a more efficient way to compute windows within a scope
   *   than computing all windows and then intersecting with the scope.
   * </p>
   *
   * @param dynamics The dynamics governing the resource over time.
   * @param condition The condition assigning each resource value a truth value.
   * @param scope The temporal scope within which to look for windows.
   *
   * @return The windows of time over which the condition is held true under the dynamics within the temporal scope.
   */
  default Windows whenSatisfied(final Dynamics dynamics, final Condition condition, final Window scope) {
    final var windows = this.whenSatisfied(dynamics, condition);
    windows.intersectWith(scope);
    return windows;
  }

  /**
   * Determine the first moment within a temporal scope in which a dynamics holds a condition true.
   *
   * <p>
   *   This should be overridden if there is a more efficient way to determine the first satisfying instant
   *   than producing a complete list of windows.
   * </p>
   *
   * @param dynamics The dynamics governing the resource over time.
   * @param condition The condition assigning each resource value a truth value.
   * @param scope The temporal scope within which to look for a satisfying instant.
   *
   * @return The first instant on which the condition is true,
   *         or an empty {@code Optional} if the condition is never true.
   */
  default Optional<Duration> firstSatisfied(final Dynamics dynamics, final Condition condition, final Window scope) {
    for (final var window : this.whenSatisfied(dynamics, condition, scope)) {
      return Optional.of(window.start);
    }
    return Optional.empty();
  }
}
