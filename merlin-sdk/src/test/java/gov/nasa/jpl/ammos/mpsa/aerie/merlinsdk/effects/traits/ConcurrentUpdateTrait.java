package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.traits;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ConcurrentUpdateTrait<T> implements EffectTrait<Pair<Optional<T>, Set<T>>> {
  @Override
  public Pair<Optional<T>, Set<T>> empty() {
    return Pair.of(Optional.empty(), Set.of());
  }

  @Override
  public Pair<Optional<T>, Set<T>> sequentially(final Pair<Optional<T>, Set<T>> prefix, final Pair<Optional<T>, Set<T>> suffix) {
    return Pair.of(
        suffix.getLeft().or(prefix::getLeft),
        (suffix.getRight().isEmpty()) ? prefix.getRight() : suffix.getRight());
  }

  @Override
  public Pair<Optional<T>, Set<T>> concurrently(final Pair<Optional<T>, Set<T>> left, final Pair<Optional<T>, Set<T>> right) {
    final var nextValue =
        (left.getLeft().isEmpty() || right.getLeft().isEmpty())
            ? left.getLeft().or(right::getLeft)
            : Optional.<T>empty();

    final var set = new HashSet<>(left.getRight());
    set.addAll(right.getRight());

    return Pair.of(nextValue, set);
  }
}
