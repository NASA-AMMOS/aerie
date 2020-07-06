package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class SettableState<T> {
  private final String name;
  private final Supplier<T> getter;
  private final Function<Predicate<T>, List<Window>> checker;
  private final Consumer<T> emitter;

  public SettableState(
      final String name,
      final Supplier<T> getter,
      final Function<Predicate<T>, List<Window>> checker,
      final Consumer<T> emitter
  ) {
    this.name = name;
    this.getter = getter;
    this.checker = checker;
    this.emitter = emitter;
  }

  public T get() {
    return this.getter.get();
  }

  public void set(T value) {
    this.emitter.accept(value);
  }

  public Constraint when(final Predicate<T> condition) {
    return Constraint.createStateConstraint(this.name, () -> this.checker.apply(condition));
  }

  public Constraint whenEqualTo(final T probe) {
    return this.when((x) -> Objects.equals(x, probe));
  }
}
