package gov.nasa.jpl.aerie.contrib.cells.register;

import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.protocol.model.CellType;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class RegisterCell<T> {
  public T value;
  private boolean conflicted;

  public RegisterCell(final T initialValue, final boolean conflicted) {
    this.value = Objects.requireNonNull(initialValue);
    this.conflicted = conflicted;
  }

  public static <Event, T> CellRef<Event, RegisterCell<T>>
  allocate(final UnaryOperator<T> duplicator, final T initialValue, final Function<Event, RegisterEffect<T>> interpreter, final ValueMapper<T> valueMapper) {
    return CellRef.allocate(
        new RegisterCell<>(initialValue, false),
        new RegisterCellType<>(duplicator, valueMapper),
        interpreter);
  }

  public boolean isConflicted() {
    return this.conflicted;
  }

  @Override
  public String toString() {
    return "{value=%s, conflicted=%s}".formatted(this.value, this.conflicted);
  }

  public record RegisterCellType<T>(UnaryOperator<T> duplicator, ValueMapper<T> valueMapper) implements CellType<RegisterEffect<T>, RegisterCell<T>> {
    @Override
    public EffectTrait<RegisterEffect<T>> getEffectType() {
      return new RegisterEffect.Trait<>();
    }

    @Override
    public RegisterCell<T> duplicate(final RegisterCell<T> cell) {
      return new RegisterCell<>(duplicator.apply(cell.value), cell.conflicted);
    }

    @Override
    public void apply(final RegisterCell<T> cell, final RegisterEffect<T> effect) {
      if (effect.newValue != null) {
        cell.value = effect.newValue;
        cell.conflicted = effect.conflicted;
      } else if (effect.conflicted) {
        cell.conflicted = true;
      }
    }

    @Override
    public SerializedValue serialize(final RegisterCell<T> cell) {
      return SerializedValue.of(Map.of(
          "value", this.valueMapper.serializeValue(cell.value),
          "conflicted", SerializedValue.of(cell.conflicted)));
    }

    @Override
    public RegisterCell<T> deserialize(final SerializedValue serializedValue) {
      final var map = serializedValue.asMap().get();
      return new RegisterCell<>(
          this.valueMapper.deserializeValue(Objects.requireNonNull(map.get("value"))).getSuccessOrThrow(),
          Objects.requireNonNull(map.get("conflicted")).asBoolean().get());
    }
  }
}
