package gov.nasa.jpl.aerie.contrib.cells.counters;

import gov.nasa.jpl.aerie.merlin.framework.Cell;

import java.util.function.BinaryOperator;

public final class CounterCell<T> implements Cell<T, CounterCell<T>> {
  private T value;
  private final BinaryOperator<T> adder;

  public CounterCell(final T initialValue, final BinaryOperator<T> adder) {
    this.value = initialValue;
    this.adder = adder;
  }

  @Override
  public CounterCell<T> duplicate() {
    return new CounterCell<>(this.value, this.adder);
  }

  @Override
  public void react(final T t) {
    this.value = this.adder.apply(this.value, t);
  }

  public T getValue() {
    return this.value;
  }
}
