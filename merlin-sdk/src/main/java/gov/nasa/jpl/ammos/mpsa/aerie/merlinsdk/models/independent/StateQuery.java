package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.independent;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;

import java.util.function.Function;
import java.util.function.Predicate;

public interface StateQuery<ResourceType> {
  ResourceType get();
  Windows when(Predicate<ResourceType> condition);

  static <S, T> StateQuery<T> from(final StateQuery<S> query, final Function<S, T> map) {
    return new StateQuery<>() {
      @Override
      public T get() {
        return map.apply(query.get());
      }

      @Override
      public Windows when(final Predicate<T> condition) {
        return query.when(v -> condition.test(map.apply(v)));
      }
    };
  }
}
