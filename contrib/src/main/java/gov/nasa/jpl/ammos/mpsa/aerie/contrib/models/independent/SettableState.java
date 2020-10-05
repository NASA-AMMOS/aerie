package gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConditionTypes;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.ConstraintStructure;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Windows;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class SettableState<T> {
  private final String name;
  private final Supplier<T> getter;
  private final Function<Predicate<T>, Windows> checker;
  private final Consumer<T> emitter;
  private final ValueMapper<T> mapper;

  public SettableState(
      final String name,
      final Supplier<T> getter,
      final Function<Predicate<T>, Windows> checker,
      final Consumer<T> emitter,
      final ValueMapper<T> mapper
  )
  {
    this.name = name;
    this.getter = getter;
    this.checker = checker;
    this.emitter = emitter;
    this.mapper = mapper;
  }

  public T get() {
    return this.getter.get();
  }

  public void set(T value) {
    this.emitter.accept(value);
  }

  public Constraint when(final Predicate<T> condition, ConditionTypes.StateComparator comparator, T value) {
    return Constraint.createStateConstraint(this.name, () -> this.checker.apply(condition),
                                            ConstraintStructure.ofStateConstraint(
                                                this.name, comparator, mapper.serializeValue(value)));
  }

  public Constraint whenEqualTo(final T probe) {
    return this.when((x) -> Objects.equals(x, probe), ConditionTypes.StateComparator.EQUAL_TO, probe);
  }
}
