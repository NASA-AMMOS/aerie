package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints.Constraint;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ConsumableState {
  private final String name;
  private final Supplier<Double> getter;
  private final Function<Predicate<Double>, List<Window>> checker;
  private final Consumer<Double> emitter;

  public ConsumableState(
      final String name,
      final Supplier<Double> getter,
      final Function<Predicate<Double>, List<Window>> checker,
      final Consumer<Double> emitter
  ) {
    this.name = name;
    this.getter = getter;
    this.checker = checker;
    this.emitter = emitter;
  }

  public void add(final double delta) {
    this.emitter.accept(delta);
  }

  public double get() {
    return this.getter.get();
  }

  public Constraint when(final Predicate<Double> condition) {
    return Constraint.createStateConstraint(this.name, () -> this.checker.apply(condition));
  }

  public Constraint whenGreaterThan(final double y) {
    return this.when((x) -> (x > y));
  }

  public Constraint whenLessThan(final double y) {
    return this.when((x) -> (x < y));
  }

  public Constraint whenLessThanOrEqualTo(final double y) {
    return this.when((x) -> (x <= y));
  }

  public Constraint whenGreaterThanOrEqualTo(final double y) {
    return this.when((x) -> (x >= y));
  }

  public Constraint whenEqualWithin(final double y, final double tolerance) {
    return this.when((x) -> (Math.abs(x - y) < tolerance));
  }
}
