package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad.effect;

public final class DiscreteEffects {
  private DiscreteEffects() {}

  // More convenient overload of "set" when using discrete dynamics

  public static <A> void set(CellResource<Discrete<A>> resource, A newValue) {
    resource.emit("Set " + newValue, effect(x -> newValue));
  }

  // Flag/Switch style operations

  public static void set(CellResource<Discrete<Boolean>> resource) {
    set(resource, true);
  }

  public static void unset(CellResource<Discrete<Boolean>> resource) {
    set(resource, false);
  }

  public static void toggle(CellResource<Discrete<Boolean>> resource) {
    resource.emit(effect(x -> !x));
  }

  // Counter style operations

  public static void increment(CellResource<Discrete<Integer>> resource) {
    increment(resource, 1);
  }

  public static void increment(CellResource<Discrete<Integer>> resource, int amount) {
    resource.emit(effect(x -> x + amount));
  }

  public static void decrement(CellResource<Discrete<Integer>> resource) {
    decrement(resource, 1);
  }

  public static void decrement(CellResource<Discrete<Integer>> resource, int amount) {
    resource.emit(effect(x -> x - amount));
  }

  // Consumable style operations

  public static void consume(CellResource<Discrete<Double>> resource, double amount) {
    resource.emit(effect(x -> x - amount));
  }

  public static void restore(CellResource<Discrete<Double>> resource, double amount) {
    resource.emit(effect(x -> x + amount));
  }

  // Non-consumable style operations

  public static void using(CellResource<Discrete<Double>> resource, double amount, Runnable action) {
    consume(resource, amount);
    action.run();
    restore(resource, amount);
  }

  // Atomic style operations

  public static void using(CellResource<Discrete<Integer>> resource, Runnable action) {
    decrement(resource);
    action.run();
    increment(resource);
  }

  // Unit-aware effects:

  // More convenient overload of "set" when using discrete dynamics

  public static <A> void set(UnitAware<CellResource<Discrete<A>>> resource, UnitAware<A> newValue) {
    set(resource.value(), newValue.value(resource.unit()));
  }

  // Consumable style operations

  public static void consume(UnitAware<CellResource<Discrete<Double>>> resource, UnitAware<Double> amount) {
    consume(resource.value(), amount.value(resource.unit()));
  }

  public static void restore(UnitAware<CellResource<Discrete<Double>>> resource, UnitAware<Double> amount) {
    restore(resource.value(), amount.value(resource.unit()));
  }

  // Non-consumable style operations

  public static void using(UnitAware<CellResource<Discrete<Double>>> resource, UnitAware<Double> amount, Runnable action) {
    consume(resource, amount);
    action.run();
    restore(resource, amount);
  }
}
