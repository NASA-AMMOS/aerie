package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.unit_aware.UnitAware;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad.effect;

public final class DiscreteEffects {
  private DiscreteEffects() {}

  // More convenient overload of "set" when using discrete dynamics

  /**
   * Set the resource to the given value.
   */
  public static <A> void set(CellResource<Discrete<A>> resource, A newValue) {
    resource.emit("Set " + newValue, effect(x -> newValue));
  }

  // Flag/Switch style operations

  /**
   * Set the resource to true.
   */
  public static void turnOn(CellResource<Discrete<Boolean>> resource) {
    set(resource, true);
  }

  /**
   * Set the resource to false.
   */
  public static void turnOff(CellResource<Discrete<Boolean>> resource) {
    set(resource, false);
  }

  /**
   * Toggle the resource value.
   */
  public static void toggle(CellResource<Discrete<Boolean>> resource) {
    resource.emit("Toggle", effect(x -> !x));
  }

  // Counter style operations

  /**
   * Add one to the resource's value.
   */
  public static void increment(CellResource<Discrete<Integer>> resource) {
    increment(resource, 1);
  }

  /**
   * Add the given amount to the resource's value.
   */
  public static void increment(CellResource<Discrete<Integer>> resource, int amount) {
    resource.emit("Increment by " + amount, effect(x -> x + amount));
  }

  /**
   * Subtract one from the resource's value.
   */
  public static void decrement(CellResource<Discrete<Integer>> resource) {
    decrement(resource, 1);
  }

  /**
   * Subtract the given amount from the resource's value.
   */
  public static void decrement(CellResource<Discrete<Integer>> resource, int amount) {
    resource.emit("Decrement by " + amount, effect(x -> x - amount));
  }

  // General numeric resources

  /**
   * Add amount to resource's value
   */
  public static void increase(CellResource<Discrete<Double>> resource, double amount) {
    resource.emit("Increase by " + amount, effect(x -> x + amount));
  }

  /**
   * Subtract amount from resource's value
   */
  public static void decrease(CellResource<Discrete<Double>> resource, double amount) {
    resource.emit("Decrease by " + amount, effect(x -> x - amount));
  }

  // Queue style operations, mirroring the Queue interface

  /**
   * Add element to the end of the queue resource
   */
  public static <T> void add(CellResource<Discrete<List<T>>> resource, T element) {
    resource.emit("Add %s to queue".formatted(element), effect(q -> {
      var q$ = new LinkedList<>(q);
      q$.add(element);
      return q$;
    }));
  }

  /**
   * Remove an element from the front of the queue resource.
   * <p>
   *   Returns that element, or empty if the queue was already empty.
   * </p>
   */
  public static <T> Optional<T> remove(CellResource<Discrete<List<T>>> resource) {
    final var currentQueue = currentValue(resource);
    if (currentQueue.isEmpty()) return Optional.empty();

    final T result = currentQueue.get(currentQueue.size() - 1);
    resource.emit("Remove %s from queue".formatted(result), effect(q -> {
      var q$ = new LinkedList<>(q);
      T purportedResult = q$.removeLast();
      if (!result.equals(purportedResult)) {
        throw new IllegalStateException("Detected effect conflicting with queue remove operation");
      }
      return q$;
    }));
    return Optional.of(result);
  }

  // Consumable style operations

  /**
   * Subtract the given amount from resource.
   */
  public static void consume(CellResource<Discrete<Double>> resource, double amount) {
    resource.emit("Consume " + amount, effect(x -> x - amount));
  }

  /**
   * Add the given amount to resource.
   */
  public static void restore(CellResource<Discrete<Double>> resource, double amount) {
    resource.emit("Restore " + amount, effect(x -> x + amount));
  }

  // Non-consumable style operations

  /**
   * Decrease resource by amount while action is running.
   */
  public static void using(CellResource<Discrete<Double>> resource, double amount, Runnable action) {
    consume(resource, amount);
    action.run();
    restore(resource, amount);
  }

  // Atomic style operations

  /**
   * Decrease resource by one while action is running.
   */
  public static void using(CellResource<Discrete<Integer>> resource, Runnable action) {
    decrement(resource);
    action.run();
    increment(resource);
  }

  // Unit-aware effects:

  // More convenient overload of "set" when using discrete dynamics

  /**
   * Set the resource to the given value.
   */
  public static <A> void set(UnitAware<CellResource<Discrete<A>>> resource, UnitAware<A> newValue) {
    set(resource.value(), newValue.value(resource.unit()));
  }

  // Consumable style operations

  /**
   * Subtract the given amount from resource.
   */
  public static void consume(UnitAware<CellResource<Discrete<Double>>> resource, UnitAware<Double> amount) {
    consume(resource.value(), amount.value(resource.unit()));
  }

  /**
   * Add the given amount to resource.
   */
  public static void restore(UnitAware<CellResource<Discrete<Double>>> resource, UnitAware<Double> amount) {
    restore(resource.value(), amount.value(resource.unit()));
  }

  // Non-consumable style operations

  /**
   * Decrease resource by amount while action is running.
   */
  public static void using(UnitAware<CellResource<Discrete<Double>>> resource, UnitAware<Double> amount, Runnable action) {
    consume(resource, amount);
    action.run();
    restore(resource, amount);
  }
}
