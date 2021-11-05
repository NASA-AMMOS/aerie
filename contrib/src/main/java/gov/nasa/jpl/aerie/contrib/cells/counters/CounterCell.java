package gov.nasa.jpl.aerie.contrib.cells.counters;

import gov.nasa.jpl.aerie.contrib.traits.CommutativeMonoid;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;

import java.util.function.BinaryOperator;
import java.util.function.Function;

public final class CounterCell<T> {
  private T value;
  private final BinaryOperator<T> adder;

  public CounterCell(final T initialValue, final BinaryOperator<T> adder) {
    this.value = initialValue;
    this.adder = adder;
  }

  public static <Event, T> CellRef<Event, CounterCell<T>>
  allocate(final T initialValue, final T zero, final BinaryOperator<T> adder, final Function<Event, T> interpreter) {
    return CellRef.allocate(
        new CounterCell<>(initialValue, adder),
        new CounterApplicator<>(),
        new CommutativeMonoid<>(zero, adder),
        interpreter);
  }

  public T getValue() {
    return this.value;
  }

  public static final class CounterApplicator<T> implements Applicator<T, CounterCell<T>> {
    @Override
    public CounterCell<T> duplicate(final CounterCell<T> cell) {
      return new CounterCell<>(cell.value, cell.adder);
    }

    @Override
    public void apply(final CounterCell<T> cell, final T effect) {
      cell.value = cell.adder.apply(cell.value, effect);
    }
  }
}
