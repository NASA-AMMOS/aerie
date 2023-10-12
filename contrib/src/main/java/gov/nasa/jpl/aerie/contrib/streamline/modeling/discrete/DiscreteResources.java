package gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete;

import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteMonad;
import gov.nasa.jpl.aerie.merlin.framework.Condition;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.Unit;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAware;
import gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.UnitAwareResources;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiPredicate;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellResource.cellResource;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Reactions.whenever;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.Discrete.discrete;
import static gov.nasa.jpl.aerie.contrib.streamline.modeling.discrete.monads.DiscreteResourceMonad.*;

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
                newException -> !currentException.equals(newException)));
    whenever(() -> {
      var currentDynamics = resource.getDynamics();
      return when(() -> DynamicsMonad.unit(discrete(liftedUpdatePredicate.test(currentDynamics, resource.getDynamics()))));
    }, () -> {
      final var newDynamics = resource.getDynamics();
      cell.emit($ -> newDynamics);
    });
    return cell;
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

  @SafeVarargs
  public static Resource<Discrete<Boolean>> and(Resource<Discrete<Boolean>>... operands) {
    return Arrays.stream(operands).reduce(unit(true), lift(Boolean::logicalAnd)::apply);
  }

  @SafeVarargs
  public static Resource<Discrete<Boolean>> or(Resource<Discrete<Boolean>>... operands) {
    return Arrays.stream(operands).reduce(unit(false), lift(Boolean::logicalOr)::apply);
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
}
