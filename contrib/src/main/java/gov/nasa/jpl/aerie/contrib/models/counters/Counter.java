package gov.nasa.jpl.aerie.contrib.models.counters;

import gov.nasa.jpl.aerie.contrib.cells.counters.CounterCell;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.DoubleValueMapper;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.IntegerValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.merlin.framework.Registrar;
import gov.nasa.jpl.aerie.merlin.framework.resources.discrete.DiscreteResource;
import gov.nasa.jpl.aerie.merlin.protocol.ValueMapper;

import java.util.function.BinaryOperator;


public final class Counter<T> implements DiscreteResource<T> {
  private final CellRef<T, CounterCell<T>> ref;

  public Counter(
      final Registrar registrar,
      final T initialValue,
      final T zero,
      final BinaryOperator<T> adder,
      final ValueMapper<T> mapper)
  {
    this.ref = registrar.cell(new CounterCell<>(initialValue, zero, adder));

    registrar.resource("value", this, mapper);
  }

  public static Counter<Integer> ofInteger(final Registrar registrar, final Integer initialValue) {
    return new Counter<>(registrar, initialValue, 0, Integer::sum, new IntegerValueMapper());
  }

  public static Counter<Integer> ofInteger(final Registrar registrar) {
    return ofInteger(registrar, 0);
  }

  public static Counter<Double> ofDouble(final Registrar registrar, final Double initialValue) {
    return new Counter<>(registrar, initialValue, 0.0, Double::sum, new DoubleValueMapper());
  }

  public static Counter<Double> ofDouble(final Registrar registrar) {
    return ofDouble(registrar, 0.0);
  }


  @Override
  public T getDynamics() {
    return this.ref.get().getValue();
  }

  public void add(final T change) {
    this.ref.emit(change);
  }
}
