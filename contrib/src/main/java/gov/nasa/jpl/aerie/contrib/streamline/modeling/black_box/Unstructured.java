package gov.nasa.jpl.aerie.contrib.streamline.modeling.black_box;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.merlin.protocol.types.Duration.ZERO;

/**
 * Dynamics with no observable structure.
 * While very general, these need to be approximated by more structured
 * dynamics to report out to Aerie.
 */
public interface Unstructured<T> extends Dynamics<T, Unstructured<T>> {
  static <T> Unstructured<T> timeBased(Function<Duration, T> valueOverTime) {
    return new Unstructured<T>() {
      @Override
      public T extract() {
        return valueOverTime.apply(ZERO);
      }

      @Override
      public Unstructured<T> step(final Duration t) {
        return timeBased(valueOverTime.compose(t::plus));
      }
    };
  }

  static <A, B> Unstructured<B> map(Dynamics<A, ?> a, Function<A, B> f) {
    return new Unstructured<B>() {
      @Override
      public B extract() {
        return f.apply(a.extract());
      }

      @Override
      public Unstructured<B> step(final Duration t) {
        return map(a.step(t), f);
      }
    };
  }

  // TODO: Look into the theory of applicatives, see if that could simplify this code any
  static <A, B, C> Unstructured<C> map(Dynamics<A, ?> a, Dynamics<B, ?> b, BiFunction<A, B, C> f) {
    return new Unstructured<C>() {
      @Override
      public C extract() {
        return f.apply(a.extract(), b.extract());
      }

      @Override
      public Unstructured<C> step(final Duration t) {
        return map(a.step(t), b.step(t), f);
      }
    };
  }

  static <A, B, C, D> Unstructured<D> map(Dynamics<A, ?> a, Dynamics<B, ?> b, Dynamics<C, ?> c, TriFunction<A, B, C, D> f) {
    Unstructured<Function<C, D>> g = map(a, b, (a$, b$) -> c$ -> f.apply(a$, b$, c$));
    return map(g, c, Function::apply);
  }
}
