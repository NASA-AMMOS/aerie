package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class UnstructuredResources {
  private UnstructuredResources() {}

  /**
   * Perform an unstructured mapping operation.
   *
   * <p>
   *   This mapping loses all information about dynamics structure, but can represent any function.
   *   The result will likely need to be approximated or sampled to be useful.
   *   For example, suppose <code>performSimulation</code> connects to another simulation tool,
   *   so we have no way to model it analytically. Then, we might write:
   *   <pre>
   * import SecantApproximation.*;
   *
   * Resource&lt;Polynomial&gt; input;
   * Resource&lt;Unstructured&lt;Double&gt;&gt; exactOutput = map(input, this::performSimulation);
   * Resource&lt;Linear&gt; approxOutput = secantApproximation(exactOutput,
   *         IntervalFunctions.byBoundingError(1e-10, Duration.SECOND, Duration.HOUR,
   *                 ErrorEstimates.errorByOptimization()));
   *   </pre>
   * </p>
   *
   * @see SecantApproximation
   */
  public static <A, B> Resource<Unstructured<B>> map(Resource<? extends Dynamics<A, ?>> a, Function<A, B> f) {
    return ResourceMonad.map(a, a$ -> Unstructured.map(a$, f));
  }

  /**
   * @see UnstructuredResources#map(Resource, Function)
   */
  public static <A, B, C> Resource<Unstructured<C>> map(
      Resource<? extends Dynamics<A, ?>> a,
      Resource<? extends Dynamics<B, ?>> b,
      BiFunction<A, B, C> f) {
    return ResourceMonad.map(a, b, (a$, b$) -> Unstructured.map(a$, b$, f));
  }

  /**
   * @see UnstructuredResources#map(Resource, Function)
   */
  public static <A, B, C, D> Resource<Unstructured<D>> map(
      Resource<? extends Dynamics<A, ?>> a,
      Resource<? extends Dynamics<B, ?>> b,
      Resource<? extends Dynamics<C, ?>> c,
      TriFunction<A, B, C, D> f) {
    return ResourceMonad.map(a, b, c, (a$, b$, c$) -> Unstructured.map(a$, b$, c$, f));
  }
}
