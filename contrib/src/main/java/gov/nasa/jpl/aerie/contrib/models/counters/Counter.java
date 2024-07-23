package gov.nasa.jpl.aerie.contrib.models.counters;

import gov.nasa.jpl.aerie.contrib.cells.counters.CounterCell;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;

import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class Counter<T> implements DiscreteResource<T> {
  private final CellRef<T, CounterCell<T>> ref;
  private final UnaryOperator<T> duplicator;

  public Counter(final T initialValue, final T zero, final BinaryOperator<T> adder, final UnaryOperator<T> duplicator, final ValueMapper<T> mapper) {
    this.ref = CounterCell.allocate(initialValue, zero, adder, Function.identity(), mapper);
    this.duplicator = duplicator;
  }

  public static Counter<Integer> ofInteger(final Integer initialValue) {
    return new Counter<>(initialValue, 0, Integer::sum, $ -> $, new IntegerValueMapper());
  }

  public static Counter<Integer> ofInteger() {
    return ofInteger(0);
  }

  public static Counter<Double> ofDouble(final Double initialValue) {
    return new Counter<>(initialValue, 0.0, Double::sum, $ -> $, new DoubleValueMapper());
  }

  public static Counter<Double> ofDouble() {
    return ofDouble(0.0);
  }


  @Override
  public T getDynamics() {
    return this.duplicator.apply(this.ref.get().value);
  }

  public void add(final T change) {
    this.ref.emit(change);
  }


  @Deprecated
  @Override
  public boolean equals(final Object obj) {
    return super.equals(obj);
  }
}
