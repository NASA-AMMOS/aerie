package gov.nasa.jpl.aerie.json;

import java.util.Objects;
import java.util.function.Function;

/** An infallible isomorphism between types {@code S} and {@code T}. */
public interface Iso<S, T> {
  T from(S source);
  S to(T target);

  static <S, T> Iso<S, T> of(final Function<S, ? extends T> from, final Function<? super T, S> to) {
    Objects.requireNonNull(from);
    Objects.requireNonNull(to);

    return new Iso<>() {
      @Override
      public T from(final S source) {
        return from.apply(source);
      }

      @Override
      public S to(final T target) {
        return to.apply(target);
      }
    };
  }

  static <T> Iso<T, T> identity() {
    return Iso.of(Function.identity(), Function.identity());
  }

  default <X> Iso<S, X> compose(final Iso<T, X> other) {
    Objects.requireNonNull(other);

    return Iso.of(
        $ -> other.from(this.from($)),
        $ -> this.to(other.to($)));
  }

  default Iso<T, S> invert() {
    return Iso.of(this::to, this::from);
  }
}
