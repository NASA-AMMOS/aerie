package gov.nasa.jpl.aerie.contrib.cells.counters;

import gov.nasa.jpl.aerie.contrib.traits.CommutativeMonoid;
import gov.nasa.jpl.aerie.merlin.framework.Cell;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import java.util.function.BinaryOperator;

public final class CounterCell<T> implements Cell<T, CounterCell<T>> {
  private T value;
  private final CommutativeMonoid<T> trait;

  public CounterCell(final T initialValue, final CommutativeMonoid<T> trait) {
    this.value = initialValue;
    this.trait = trait;
  }

  public CounterCell(final T initialValue, final T zero, final BinaryOperator<T> adder) {
    this(initialValue, new CommutativeMonoid<>(zero, adder));
  }

  @Override
  public CounterCell<T> duplicate() {
    return new CounterCell<>(this.value, this.trait);
  }

  @Override
  public EffectTrait<T> effectTrait() {
    return this.trait;
  }

  @Override
  public void react(final T t) {
    this.value = this.trait.sequentially(this.value, t);
  }

  public T getValue() {
    return this.value;
  }
}
