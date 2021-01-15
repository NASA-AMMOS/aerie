package gov.nasa.jpl.aerie.contrib.models.counters;

import gov.nasa.jpl.aerie.contrib.cells.counters.CounterCell;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Model;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;

import java.util.function.BinaryOperator;


public final class Counter<$Schema, T> extends Model<$Schema> {
  public final DiscreteResource<$Schema, T> value;

  private final CellRef<$Schema, T, CounterCell<T>> ref;

  public Counter(
      final Registrar<$Schema> registrar,
      final T initialValue,
      final T zero,
      final BinaryOperator<T> adder,
      final ValueMapper<T> mapper)
  {
    super(registrar);
    this.ref = registrar.cell(new CounterCell<>(initialValue, zero, adder));

    this.value = registrar.resource(
        "value",
        DiscreteResource.atom(this.ref, CounterCell::getValue),
        mapper);
  }

  public void add(final T change) {
    this.ref.emit(change);
  }

  public static <$Schema> Counter<$Schema, Integer> ofInteger(final Registrar<$Schema> registrar, final Integer initialValue) {
    return new Counter<>(registrar, initialValue, 0, Integer::sum, new IntegerValueMapper());
  }

  public static <$Schema> Counter<$Schema, Integer> ofInteger(final Registrar<$Schema> registrar) {
    return ofInteger(registrar, 0);
  }

  public static <$Schema> Counter<$Schema, Double> ofDouble(final Registrar<$Schema> registrar, final Double initialValue) {
    return new Counter<>(registrar, initialValue, 0.0, Double::sum, new DoubleValueMapper());
  }

  public static <$Schema> Counter<$Schema, Double> ofDouble(final Registrar<$Schema> registrar) {
    return ofDouble(registrar, 0.0);
  }
}
