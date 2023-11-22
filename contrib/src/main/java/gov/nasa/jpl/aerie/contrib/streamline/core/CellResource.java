package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ErrorCatchingMonad;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Context;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.Cell;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import java.util.LinkedList;
import java.util.List;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.allocate;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.autoEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.core.Labelled.labelled;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.unit;
import static java.util.stream.Collectors.joining;

/**
 * Resource which is backed directly by a cell.
 * Effects can be applied to this resource.
 * Effect names are augmented with this resource's name(s).
 */
public interface CellResource<D extends Dynamics<?, D>> extends Resource<D> {
  void emit(Labelled<DynamicsEffect<D>> effect);
  default void emit(String effectName, DynamicsEffect<D> effect) {
    emit(labelled(effectName, effect));
  }
  default void emit(DynamicsEffect<D> effect) {
    emit("anonymous effect", effect);
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(D initial) {
    return cellResource(unit(initial));
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(D initial, EffectTrait<Labelled<DynamicsEffect<D>>> effectTrait) {
    return cellResource(unit(initial), effectTrait);
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(ErrorCatching<Expiring<D>> initial) {
    return cellResource(initial, autoEffects());
  }

  static <D extends Dynamics<?, D>> CellResource<D> cellResource(ErrorCatching<Expiring<D>> initial, EffectTrait<Labelled<DynamicsEffect<D>>> effectTrait) {
    return new CellResource<>() {
      // Use autoEffects for a generic CellResource, on the theory that most resources
      // have relatively few effects, and even fewer concurrent effects, so this is performant enough.
      // If that doesn't hold, a more specialized solution can be constructed directly.
      private final CellRef<Labelled<DynamicsEffect<D>>, Cell<D>> cell = allocate(initial, effectTrait);
      private final List<String> names = new LinkedList<>();

      @Override
      public void emit(final Labelled<DynamicsEffect<D>> effect) {
        cell.emit(labelled(augmentEffectName(effect.name()), effect.data()));
      }

      @Override
      public ErrorCatching<Expiring<D>> getDynamics() {
        return cell.get().dynamics;
      }

      @Override
      public void registerName(final String name) {
        names.add(name);
      }

      private String augmentEffectName(String effectName) {
        var resourceName = switch (names.size()) {
          case 0 -> "anonymous resource";
          case 1 -> names.get(0);
          default -> names.get(0) + " (aka. %s)".formatted(String.join(", ", names.subList(1, names.size())));
        };
        return effectName + " on " + resourceName + Context.get().stream().map(c -> " during " + c).collect(joining());
      }
    };
  }

  static <D extends Dynamics<?, D>> void set(CellResource<D> resource, D newDynamics) {
    resource.emit("Set " + newDynamics, DynamicsMonad.effect(x -> newDynamics));
  }

  static <D extends Dynamics<?, D>> void set(CellResource<D> resource, Expiring<D> newDynamics) {
    resource.emit("Set " + newDynamics, ErrorCatchingMonad.<Expiring<D>, Expiring<D>>lift($ -> newDynamics)::apply);
  }

}
