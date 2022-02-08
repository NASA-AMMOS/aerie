package gov.nasa.jpl.aerie.contrib.cells.register;

import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

public sealed interface RegisterEffect<T> {
  record NoOp<T>() implements RegisterEffect<T> {}
  record PureConflict<T>() implements RegisterEffect<T> {}
  record ConflictedWrite<T>(T value) implements RegisterEffect<T> {}
  record UnconflictedWrite<T>(T value) implements RegisterEffect<T> {}

  static <T> RegisterEffect<T> set(T value) {
    return new UnconflictedWrite<>(value);
  }

  final class Trait<T> implements EffectTrait<RegisterEffect<T>> {

    private static final NoOp<?> EMPTY = new NoOp<>();
    private static final PureConflict<?> PURE_CONFLICT = new PureConflict<>();

    @SuppressWarnings("unchecked")
    @Override
    public RegisterEffect<T> empty() {
      // SAFETY: EMPTY is an empty record, so it can conform to any desired generic type.
      return (RegisterEffect<T>) EMPTY;
    }

    @Override
    public RegisterEffect<T> sequentially(final RegisterEffect<T> prefix, final RegisterEffect<T> suffix) {
      if (suffix instanceof ConflictedWrite || suffix instanceof UnconflictedWrite) {
        // The suffix is a set (with or without a subsequent conflict); it strictly dominates the prefix.
        return suffix;
      } else if (suffix instanceof NoOp) {
        // The suffix is a no-op; take the prefix.
        return prefix;
      } else if (prefix instanceof ConflictedWrite || prefix instanceof UnconflictedWrite) {
        // The suffix is a pure conflict, and the prefix performed a valid write.
        return new ConflictedWrite<>(prefix instanceof ConflictedWrite<T> w
            ? w.value()
            : ((UnconflictedWrite<T>) prefix).value());
      } else {
        // Both suffix and prefix are pure conflicts, so the suffix dominates the prefix.
        return suffix;
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public RegisterEffect<T> concurrently(final RegisterEffect<T> left, final RegisterEffect<T> right) {
      if (left instanceof NoOp) {
        // Left is a no-op; take the right.
        return right;
      } else if (right instanceof NoOp) {
        // Right is a no-op; take the left.
        return left;
      } else {
        // Left and right are both doing *something*, causing a pure conflict.
        // SAFETY: PURE_CONFLICT is an empty record, so it can conform to any desired generic type.
        return (RegisterEffect<T>) PURE_CONFLICT;
      }
    }
  }
}
