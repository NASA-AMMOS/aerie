package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete;

import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ExpiringToResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteMonad;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.Unit;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareResources;
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

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
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

  public static Condition when(Resource<Discrete<Boolean>> resource) {
    return (positive, atEarliest, atLatest) ->
        resource.getDynamics().match(
            dynamics -> Optional.of(atEarliest).filter($ -> dynamics.data().extract() == positive),
            error -> Optional.empty());
  }

  public static <V> Resource<Discrete<V>> cache(Resource<Discrete<V>> resource, BiPredicate<V, V> updatePredicate) {
    final var cell = cellResource(resource.getDynamics());
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
      return when(() -> DynamicsMonad.unit(discrete(liftedUpdatePredicate.test(
          currentDynamics,
          resource.getDynamics()))));
    }, () -> {
      final var newDynamics = resource.getDynamics();
      cell.emit($ -> newDynamics);
    });
    return cell;
  }

  public static <V, T extends Dynamics<Duration, T>> Resource<Discrete<V>> sampled(Supplier<V> valueSupplier, Resource<T> samplePeriod) {
    var result = cellResource(discrete(valueSupplier.get()));
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

  public static UnitAware<Resource<Discrete<Double>>> unitAware(Resource<Discrete<Double>> resource, Unit unit) {
    return UnitAwareResources.unitAware(resource, unit, DiscreteResources::discreteScaling);
  }

  public static UnitAware<CellResource<Discrete<Double>>> unitAware(CellResource<Discrete<Double>> resource, Unit unit) {
    return UnitAwareResources.unitAware(resource, unit, DiscreteResources::discreteScaling);
  }

  private static Discrete<Double> discreteScaling(Discrete<Double> d, Double scale) {
    return DiscreteMonad.map(d, $ -> $ * scale);
  }


  // Boolean logic

  public static Resource<Discrete<Boolean>> and(Resource<Discrete<Boolean>> left, Resource<Discrete<Boolean>> right) {
    // Short-circuiting and: Only gets right if left is true
    return bind(left, l -> l ? right : unit(false));
  }

  @SafeVarargs
  public static Resource<Discrete<Boolean>> and(Resource<Discrete<Boolean>>... operands) {
    return and(stream(operands));
  }

  public static Resource<Discrete<Boolean>> and(Stream<Resource<Discrete<Boolean>>> operands) {
    // Reduce using the short-circuiting and to improve efficiency
    return operands.reduce(unit(true), DiscreteResources::and);
  }

  public static Resource<Discrete<Boolean>> or(Resource<Discrete<Boolean>> left, Resource<Discrete<Boolean>> right) {
    // Short-circuiting or: Only gets right if left is false
    return bind(left, l -> l ? unit(true) : right);
  }

  @SafeVarargs
  public static Resource<Discrete<Boolean>> or(Resource<Discrete<Boolean>>... operands) {
    return or(stream(operands));
  }

  public static Resource<Discrete<Boolean>> or(Stream<Resource<Discrete<Boolean>>> operands) {
    // Reduce using the short-circuiting or to improve efficiency
    return operands.reduce(unit(false), DiscreteResources::or);
  }

  public static Resource<Discrete<Boolean>> not(Resource<Discrete<Boolean>> operand) {
    return map(operand, $ -> !$);
  }

  public static Resource<Discrete<Boolean>> assertThat(String description, Resource<Discrete<Boolean>> assertion) {
    return map(assertion, a -> {
      if (a) return true;
      throw new AssertionError(description);
    });
  }

  // Integer arithmetic

  @SafeVarargs
  public static Resource<Discrete<Integer>> add(Resource<Discrete<Integer>>... operands) {
    return sum(Arrays.stream(operands));
  }

  public static Resource<Discrete<Integer>> sum(Stream<Resource<Discrete<Integer>>> operands) {
    return operands.reduce(unit(0), lift(Integer::sum)::apply);
  }

  public static Resource<Discrete<Integer>> subtract(Resource<Discrete<Integer>> left, Resource<Discrete<Integer>> right) {
    return map(left, right, (l, r) -> l - r);
  }

  @SafeVarargs
  public static Resource<Discrete<Integer>> multiply(Resource<Discrete<Integer>>... operands) {
    return product(Arrays.stream(operands));
  }

  public static Resource<Discrete<Integer>> product(Stream<Resource<Discrete<Integer>>> operands) {
    return operands.reduce(unit(1), lift((Integer x, Integer y) -> x * y)::apply);
  }

  public static Resource<Discrete<Integer>> divide(Resource<Discrete<Integer>> left, Resource<Discrete<Integer>> right) {
    return map(left, right, (l, r) -> l / r);
  }

  // Collections

  public static <C extends Collection<?>> Resource<Discrete<Boolean>> isEmpty(Resource<Discrete<C>> resource) {
    return map(resource, Collection::isEmpty);
  }

  public static <C extends Collection<?>> Resource<Discrete<Boolean>> isNonEmpty(Resource<Discrete<C>> resource) {
    return not(isEmpty(resource));
  }
}
