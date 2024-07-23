package gov.nasa.jpl.aerie.contrib.cells.counters;

import gov.nasa.jpl.aerie.contrib.traits.CommutativeMonoid;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public final class CounterCell<T> {
  public T value;

  public CounterCell(final T initialValue) {
    this.value = Objects.requireNonNull(initialValue);
  }

  public static <Event, T> CellRef<Event, CounterCell<T>>
  allocate(
      final T initialValue,
      final T zero,
      final BinaryOperator<T> adder,
      final Function<Event, T> interpreter,
      final ValueMapper<T> mapper
  ) {
    return CellRef.allocate(
        new CounterCell<>(initialValue),
        new CounterCellType<>(zero, adder, mapper),
        interpreter);
  }

  public static final class CounterCellType<T> implements CellType<T, CounterCell<T>> {
    private final EffectTrait<T> monoid;
    private final ValueMapper<T> mapper;

    public CounterCellType(final T zero, final BinaryOperator<T> adder, final ValueMapper<T> mapper) {
      this.monoid = new CommutativeMonoid<>(zero, adder);
      this.mapper = mapper;
    }

    @Override
    public EffectTrait<T> getEffectType() {
      return this.monoid;
    }

    @Override
    public CounterCell<T> duplicate(final CounterCell<T> cell) {
      return new CounterCell<>(cell.value);
    }

    @Override
    public void apply(final CounterCell<T> cell, final T effect) {
      cell.value = this.monoid.sequentially(cell.value, effect);
    }

    @Override
    public SerializedValue serialize(final CounterCell<T> cell) {
      return this.mapper.serializeValue(cell.value);
    }

    @Override
    public CounterCell<T> deserialize(final SerializedValue serializedValue) {
      return new CounterCell<>(
          this.mapper.deserializeValue(serializedValue).getSuccessOrThrow()
      );
    }
  }
}
