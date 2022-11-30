package gov.nasa.jpl.aerie.constraints.profile;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A base-preserving map out of the product of two pointed sets.
 *
 * The basepoint of a product is the pair of their basepoints. Only this point needs to be sent
 * to the target set's basepoint. If one or both elements of the tuple are defined, the transformation
 * can do whatever it likes.
 */
public interface BinaryOperation<Left, Right, Out> {
  // When only one value is present
  Optional<Out> left(Left left);
  Optional<Out> right(Right right);
  // When both values are present
  Optional<Out> combine(Left left, Right right);
  // When no values are present, the transformation always produces `empty`.

  static <Left, Right, Out> BinaryOperation<Left, Right, Out> from(final BiFunction<Optional<Left>, Optional<Right>, Optional<Out>> f) {
    return new BinaryOperation<>() {
      @Override
      public Optional<Out> combine(final Left left, final Right right) {
        return f.apply(Optional.of(left), Optional.of(right));
      }

      @Override
      public Optional<Out> left(final Left left) {
        return f.apply(Optional.of(left), Optional.empty());
      }

      @Override
      public Optional<Out> right(final Right right) {
        return f.apply(Optional.empty(), Optional.of(right));
      }
    };
  }

  static <Left, Right, Out> BinaryOperation<Left, Right, Out> fromCases(
      final Function<Left, Optional<Out>> left,
      final Function<Right, Optional<Out>> right,
      final BiFunction<Left, Right, Optional<Out>> combine
  ) {
    return new BinaryOperation<>() {
      @Override
      public Optional<Out> combine(final Left a, final Right b) {
        return combine.apply(a, b);
      }

      @Override
      public Optional<Out> left(final Left a) {
        return left.apply(a);
      }

      @Override
      public Optional<Out> right(final Right b) {
        return right.apply(b);
      }
    };
  }

  static <Left, Right, Out> BinaryOperation<Left, Right, Out> combineDefaultEmpty(final BiFunction<Left, Right, Out> f) {
    return new BinaryOperation<>() {
      @Override
      public Optional<Out> left(final Left left) {
        return Optional.empty();
      }

      @Override
      public Optional<Out> right(final Right right) {
        return Optional.empty();
      }

      @Override
      public Optional<Out> combine(final Left left, final Right right) {
        return Optional.of(f.apply(left, right));
      }
    };
  }

  static <V> BinaryOperation<V, V, V> combineDefaultIdentity(final BiFunction<V, V, V> f) {
    return new BinaryOperation<>() {
      @Override
      public Optional<V> left(final V left) {
        return Optional.of(left);
      }

      @Override
      public Optional<V> right(final V right) {
        return Optional.of(right);
      }

      @Override
      public Optional<V> combine(final V left, final V right) {
        return Optional.of(f.apply(left, right));
      }
    };
  }

  enum OpMode {
    Left, Right, Combine
  }
}
