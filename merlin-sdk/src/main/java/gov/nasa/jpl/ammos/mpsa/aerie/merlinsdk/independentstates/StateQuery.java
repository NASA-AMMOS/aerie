package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public interface StateQuery<ResourceType> {
  ResourceType get();
  List<Window> when(Predicate<ResourceType> condition);

  static <S, T> StateQuery<T> from(final StateQuery<S> query, final Function<S, T> map) {
    return new StateQuery<>() {
      @Override
      public T get() {
        return map.apply(query.get());
      }

      @Override
      public List<Window> when(final Predicate<T> condition) {
        return query.when(v -> condition.test(map.apply(v)));
      }
    };
  }
}
