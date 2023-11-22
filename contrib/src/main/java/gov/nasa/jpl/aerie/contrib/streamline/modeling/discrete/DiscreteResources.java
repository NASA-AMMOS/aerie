package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.CommutativityTestInput;
import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringToResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteDynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.utils.DoubleUtils;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.unit_aware.Unit;
import gov.nasa.jpl.aerie.contrib.streamline.unit_aware.UnitAware;
import gov.nasa.jpl.aerie.contrib.streamline.unit_aware.UnitAwareResources;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.autoEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.testing;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiring.expiring;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Expiry.expiry;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.every;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.currentValue;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Resources.equivalentExceptions;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.clocks.ClockResources.clock;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.DiscreteEffects.set;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.*;
import static java.util.Arrays.stream;

public final class DiscreteResources {
  private DiscreteResources() {}

  public static <T> Resource<Discrete<T>> constant(T value) {
    return DiscreteResourceMonad.unit(value);
  }

  // General discrete cell resource constructor
  public static <T> CellResource<Discrete<T>> discreteCellResource(T initialValue) {
    return CellResource.cellResource(discrete(initialValue));
  }

  // Annoyingly, we need to repeat the specialization for integer resources, so that
  // discreteCellResource(42) doesn't become a double resource, due to the next overload
  public static CellResource<Discrete<Integer>> discreteCellResource(int initialValue) {
    return CellResource.cellResource(discrete(initialValue));
  }

  // specialized constructor for doubles, because they require a toleranced equality comparison
  public static CellResource<Discrete<Double>> discreteCellResource(double initialValue) {
    return CellResource.cellResource(discrete(initialValue), autoEffects(testing(
        (CommutativityTestInput<Discrete<Double>> input) -> DoubleUtils.areEqualResults(
            input.original().extract(),
            input.leftResult().extract(),
            input.rightResult().extract()))));
  }

  /**
   * Returns a condition that's satisfied whenever this resource is true.
   */
  public static Condition when(Resource<Discrete<Boolean>> resource) {
    return (positive, atEarliest, atLatest) ->
        resource.getDynamics().match(
            dynamics -> Optional.of(atEarliest).filter($ -> dynamics.data().extract() == positive),
            error -> Optional.empty());
  }

  /**
   * Cache resource, updating the cache when updatePredicate(cached value, resource value) is true.
   */
  public static <V> Resource<Discrete<V>> cache(Resource<Discrete<V>> resource, BiPredicate<V, V> updatePredicate) {
    final var cell = CellResource.cellResource(resource.getDynamics());
    // TODO: Does the update predicate need to propagate expiry information?
    BiPredicate<ErrorCatching<Expiring<Discrete<V>>>, ErrorCatching<Expiring<Discrete<V>>>> liftedUpdatePredicate = (eCurrent, eNew) ->
        eCurrent.match(
            current -> eNew.match(
                value -> updatePredicate.test(current.data().extract(), value.data().extract()),
                newException -> true),
            currentException -> eNew.match(
                value -> true,
                newException -> !equivalentExceptions(currentException, newException)));
    whenever(() -> {
      var currentDynamics = resource.getDynamics();
      return when(() -> DiscreteDynamicsMonad.unit(liftedUpdatePredicate.test(
          currentDynamics,
          resource.getDynamics())));
    }, () -> {
      final var newDynamics = resource.getDynamics();
      cell.emit($ -> newDynamics);
    });
    return cell;
  }

  /**
   * Sample valueSupplier once every samplePeriod.
   */
  public static <V, T extends Dynamics<Duration, T>> Resource<Discrete<V>> sampled(Supplier<V> valueSupplier, Resource<T> samplePeriod) {
    var result = discreteCellResource(valueSupplier.get());
    every(() -> currentValue(samplePeriod, Duration.MAX_VALUE),
          () -> set(result, valueSupplier.get()));
    return result;
  }

  /**
   * Returns a discrete resource that follows a precomputed sequence of values.
   * Resource value is the value associated with the greatest key in segments not exceeding
   * the current simulation time, or valueBeforeFirstEntry if every key exceeds current simulation time.
   */
  public static <V> Resource<Discrete<V>> precomputed(
      final V valueBeforeFirstEntry, final NavigableMap<Duration, V> segments) {
    var clock = clock();
    return ResourceMonad.bind(clock, clock$ -> {
      var t = clock$.extract();
      var entry = segments.floorEntry(t);
      var value = entry == null ? valueBeforeFirstEntry : entry.getValue();
      var nextTime = expiry(Optional.ofNullable(segments.higherKey(t)));
      return ExpiringToResourceMonad.unit(expiring(discrete(value), nextTime.minus(t)));
    });
  }

  /**
   * Returns a discrete resource that follows a precomputed sequence of values.
   * Resource value is the value associated with the greatest key in segments not exceeding
   * the current simulation time, or valueBeforeFirstEntry if every key exceeds current simulation time.
   */
  public static <V> Resource<Discrete<V>> precomputed(
      final V valueBeforeFirstEntry, final NavigableMap<Instant, V> segments, final Instant simulationStartTime) {
    var segmentsUsingDurationKeys = new TreeMap<Duration, V>();
    for (var entry : segments.entrySet()) {
      segmentsUsingDurationKeys.put(
          Duration.of(ChronoUnit.MICROS.between(simulationStartTime, entry.getKey()), Duration.MICROSECONDS),
          entry.getValue());
    }
    return precomputed(valueBeforeFirstEntry, segmentsUsingDurationKeys);
  }

  /**
   * Add units to a discrete double resource.
   */
  public static UnitAware<Resource<Discrete<Double>>> unitAware(Resource<Discrete<Double>> resource, Unit unit) {
    return UnitAwareResources.unitAware(resource, unit, DiscreteResources::discreteScaling);
  }

  /**
   * Add units to a discrete double resource.
   */
  public static UnitAware<CellResource<Discrete<Double>>> unitAware(CellResource<Discrete<Double>> resource, Unit unit) {
    return UnitAwareResources.unitAware(resource, unit, DiscreteResources::discreteScaling);
  }

  private static Discrete<Double> discreteScaling(Discrete<Double> d, Double scale) {
    return DiscreteMonad.map(d, $ -> $ * scale);
  }


  // Boolean logic

  /**
   * Short-circuiting logical "and"
   */
  public static Resource<Discrete<Boolean>> and(Resource<Discrete<Boolean>> left, Resource<Discrete<Boolean>> right) {
    // Short-circuiting and: Only gets right if left is true
    return bind(left, l -> l ? right : unit(false));
  }

  /**
   * Reduce operands using short-circuiting logical "and"
   */
  @SafeVarargs
  public static Resource<Discrete<Boolean>> and(Resource<Discrete<Boolean>>... operands) {
    return and(stream(operands));
  }

  /**
   * Reduce operands using short-circuiting logical "and"
   */
  public static Resource<Discrete<Boolean>> and(Stream<? extends Resource<Discrete<Boolean>>> operands) {
    // Reduce using the short-circuiting and to improve efficiency
    return operands.reduce(unit(true), DiscreteResources::and, DiscreteResources::and);
  }

  /**
   * Short-circuiting logical "or"
   */
  public static Resource<Discrete<Boolean>> or(Resource<Discrete<Boolean>> left, Resource<Discrete<Boolean>> right) {
    // Short-circuiting or: Only gets right if left is false
    return bind(left, l -> l ? unit(true) : right);
  }

  /**
   * Reduce operands using short-circuiting logical "or"
   */
  @SafeVarargs
  public static Resource<Discrete<Boolean>> or(Resource<Discrete<Boolean>>... operands) {
    return or(stream(operands));
  }

  /**
   * Reduce operands using short-circuiting logical "or"
   */
  public static Resource<Discrete<Boolean>> or(Stream<? extends Resource<Discrete<Boolean>>> operands) {
    // Reduce using the short-circuiting or to improve efficiency
    return operands.reduce(unit(false), DiscreteResources::or, DiscreteResources::or);
  }

  /**
   * Logical "not"
   */
  public static Resource<Discrete<Boolean>> not(Resource<Discrete<Boolean>> operand) {
    return map(operand, $ -> !$);
  }

  /**
   * Resource-level if-then-else logic.
   */
  public static <D> Resource<D> choose(Resource<Discrete<Boolean>> condition, Resource<D> thenCase, Resource<D> elseCase) {
    return ResourceMonad.bind(condition, c -> c.extract() ? thenCase : elseCase);
  }

  /**
   * Assert that this resource is always true.
   * Otherwise, this resource fails.
   * Register this resource to detect that failure.
   */
  public static Resource<Discrete<Boolean>> assertThat(String description, Resource<Discrete<Boolean>> assertion) {
    return map(assertion, a -> {
      if (a) return true;
      throw new AssertionError(description);
    });
  }

  // Integer arithmetic

  /**
   * Add integer resources
   */
  @SafeVarargs
  public static Resource<Discrete<Integer>> addInt(Resource<Discrete<Integer>>... operands) {
    return sumInt(Arrays.stream(operands));
  }

  /**
   * Add integer resources
   */
  public static Resource<Discrete<Integer>> sumInt(Stream<? extends Resource<Discrete<Integer>>> operands) {
    return operands.reduce(unit(0), lift(Integer::sum), lift(Integer::sum)::apply);
  }

  /**
   * Subtract integer resources
   */
  public static Resource<Discrete<Integer>> subtractInt(Resource<Discrete<Integer>> left, Resource<Discrete<Integer>> right) {
    return map(left, right, (l, r) -> l - r);
  }

  /**
   * Multiply integer resources
   */
  @SafeVarargs
  public static Resource<Discrete<Integer>> multiplyInt(Resource<Discrete<Integer>>... operands) {
    return productInt(Arrays.stream(operands));
  }

  /**
   * Multiply integer resources
   */
  public static Resource<Discrete<Integer>> productInt(Stream<? extends Resource<Discrete<Integer>>> operands) {
    return operands.reduce(unit(1), lift((x, y) -> x * y), lift((Integer x, Integer y) -> x * y)::apply);
  }

  /**
   * Divide integer resources
   */
  public static Resource<Discrete<Integer>> divideInt(Resource<Discrete<Integer>> left, Resource<Discrete<Integer>> right) {
    return map(left, right, (l, r) -> l / r);
  }

  // Double arithmetic

  /**
   * Add double resources
   */
  @SafeVarargs
  public static Resource<Discrete<Double>> add(Resource<Discrete<Double>>... operands) {
    return sum(Arrays.stream(operands));
  }

  /**
   * Add double resources
   */
  public static Resource<Discrete<Double>> sum(Stream<? extends Resource<Discrete<Double>>> operands) {
    return operands.reduce(unit(0.0), lift(Double::sum), lift(Double::sum)::apply);
  }

  /**
   * Subtract double resources
   */
  public static Resource<Discrete<Double>> subtract(Resource<Discrete<Double>> left, Resource<Discrete<Double>> right) {
    return map(left, right, (l, r) -> l - r);
  }

  /**
   * Multiply double resources
   */
  @SafeVarargs
  public static Resource<Discrete<Double>> multiply(Resource<Discrete<Double>>... operands) {
    return product(Arrays.stream(operands));
  }

  /**
   * Multiply double resources
   */
  public static Resource<Discrete<Double>> product(Stream<? extends Resource<Discrete<Double>>> operands) {
    return operands.reduce(unit(1.0), lift((x, y) -> x * y), lift((Double x, Double y) -> x * y)::apply);
  }

  /**
   * Divide double resources
   */
  public static Resource<Discrete<Double>> divide(Resource<Discrete<Double>> left, Resource<Discrete<Double>> right) {
    return map(left, right, (l, r) -> l / r);
  }

  // Collections

  /**
   * Returns a resource that's true when the argument is empty
   */
  public static <C extends Collection<?>> Resource<Discrete<Boolean>> isEmpty(Resource<Discrete<C>> resource) {
    return map(resource, Collection::isEmpty);
  }

  /**
   * Returns a resource that's true when the argument is non-empty
   */
  public static <C extends Collection<?>> Resource<Discrete<Boolean>> isNonEmpty(Resource<Discrete<C>> resource) {
    return not(isEmpty(resource));
  }
}
