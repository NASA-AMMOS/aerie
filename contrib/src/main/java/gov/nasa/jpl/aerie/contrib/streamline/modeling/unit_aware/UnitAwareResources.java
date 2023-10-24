package gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware;

import gov.nasa.jpl.aerie.contrib.streamline.core.CellResource;
import gov.nasa.jpl.aerie.contrib.streamline.core.ErrorCatching;
import gov.nasa.jpl.aerie.contrib.streamline.core.Labelled;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resource;
import gov.nasa.jpl.aerie.contrib.streamline.core.Dynamics;
import gov.nasa.jpl.aerie.contrib.streamline.core.DynamicsEffect;
import gov.nasa.jpl.aerie.contrib.streamline.core.Expiring;
import gov.nasa.jpl.aerie.contrib.streamline.core.Resources;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ResourceMonad;

import java.util.function.BiFunction;
import java.util.function.Function;

import static gov.nasa.jpl.aerie.contrib.streamline.modeling.unit_aware.Quantities.quantity;

public final class UnitAwareResources {
  private UnitAwareResources() {}

  public static <D> UnitAware<Resource<D>> unitAware(Resource<D> resource, Unit unit, BiFunction<D, Double, D> scaling) {
    return UnitAware.unitAware(resource, unit, extend(scaling, ResourceMonad::map));
  }

  public static <D extends Dynamics<?, D>> UnitAware<CellResource<D>> unitAware(CellResource<D> resource, Unit unit, BiFunction<D, Double, D> scaling) {
    final BiFunction<ErrorCatching<Expiring<D>>, Double, ErrorCatching<Expiring<D>>> extendedScaling = extend(scaling, DynamicsMonad::map);
    return UnitAware.unitAware(resource, unit, (cellResource, scale) -> new CellResource<D>() {
      @Override
      public void emit(final Labelled<DynamicsEffect<D>> effect) {
        // Use an effect in the scaled domain by first scaling the dynamics,
        // then applying the effect, then de-scaling the result back
        cellResource.emit(new Labelled<>(
            unscaledDynamics -> extendedScaling.apply(effect.data().apply(
                extendedScaling.apply(unscaledDynamics, scale)), 1 / scale),
            effect.name()));
      }

      @Override
      public ErrorCatching<Expiring<D>> getDynamics() {
        return extendedScaling.apply(cellResource.getDynamics(), scale);
      }

      @Override
      public void registerName(final String name) {
        resource.registerName(name);
      }
    });
  }

  public static <A, MA> BiFunction<MA, Double, MA> extend(BiFunction<A, Double, A> scaling, BiFunction<MA, Function<A, A>, MA> map) {
    return (ma, s) -> map.apply(ma, a -> scaling.apply(a, s));
  }

  public static <A> BiFunction<Resource<A>, Double, Resource<A>> extend(BiFunction<A, Double, A> scaling) {
    return extend(scaling, ResourceMonad::map);
  }

  public static <D extends Dynamics<Double, D>> UnitAware<Double> currentValue(UnitAware<? extends Resource<D>> resource) {
    return quantity(Resources.currentValue(resource.value()), resource.unit());
  }

  public static <D extends Dynamics<?, D>> UnitAware<Resource<D>> cache(UnitAware<Resource<D>> resource) {
    return resource.map(Resources::cache);
  }
}
