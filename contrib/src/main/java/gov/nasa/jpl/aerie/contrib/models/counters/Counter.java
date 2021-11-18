package gov.nasa.jpl.aerie.contrib.models.counters;

import gov.nasa.jpl.aerie.contrib.cells.counters.CounterCell;
import gov.nasa.jpl.aerie.merlin.framework.Cell;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;

import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class Counter<T> implements DiscreteResource<T> {
  private final Cell<T, CounterCell<T>> cell;

  public Counter(final T initialValue, final T zero, final BinaryOperator<T> adder, final UnaryOperator<T> duplicator) {
    this.cell = CounterCell.allocate(initialValue, zero, adder, duplicator, Function.identity());
  }

  public static Counter<Integer> ofInteger(final Integer initialValue) {
    return new Counter<>(initialValue, 0, Integer::sum, $ -> $);
  }

  public static Counter<Integer> ofInteger() {
    return ofInteger(0);
  }

  public static Counter<Double> ofDouble(final Double initialValue) {
    return new Counter<>(initialValue, 0.0, Double::sum, $ -> $);
  }

  public static Counter<Double> ofDouble() {
    return ofDouble(0.0);
  }


  @Override
  public T getDynamics() {
    return this.cell.get().getValue();
  }

  public void add(final T change) {
    this.cell.emit(change);
  }


  @Deprecated
  @Override
  public boolean equals(final Object obj) {
    return super.equals(obj);
  }
}
