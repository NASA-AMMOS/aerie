package gov.nasa.jpl.aerie.contrib.streamline.core;

import gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad;
import gov.nasa.jpl.aerie.contrib.streamline.core.monads.ErrorCatchingMonad;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Context;
import gov.nasa.jpl.aerie.contrib.streamline.debugging.Profiling;
import gov.nasa.jpl.aerie.merlin.framework.CellRef;
import gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.Cell;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;

import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.allocate;
import static gov.nasa.jpl.aerie.contrib.streamline.core.CellRefV2.autoEffects;
import static gov.nasa.jpl.aerie.contrib.streamline.core.monads.DynamicsMonad.pure;
import static gov.nasa.jpl.aerie.contrib.streamline.debugging.Naming.*;
import static java.util.stream.Collectors.joining;

/**
 * A resource to which effects can be applied.
 */
public interface MutableResource<D extends Dynamics<?, D>> extends Resource<D> {
  void emit(DynamicsEffect<D> effect);
  default void emit(String effectName, DynamicsEffect<D> effect) {
    name(effect, effectName);
    emit(effect);
  }

  static <D extends Dynamics<?, D>> MutableResource<D> resource(D initial) {
    return resource(pure(initial));
  }

  static <D extends Dynamics<?, D>> MutableResource<D> resource(D initial, EffectTrait<DynamicsEffect<D>> effectTrait) {
    return resource(pure(initial), effectTrait);
  }

  static <D extends Dynamics<?, D>> MutableResource<D> resource(ErrorCatching<Expiring<D>> initial) {
    // Use autoEffects for a generic CellResource, on the theory that most resources
    // have relatively few effects, and even fewer concurrent effects, so this is performant enough.
    // If that doesn't hold, a more specialized solution can be constructed directly.
    return resource(initial, autoEffects());
  }

  static <D extends Dynamics<?, D>> MutableResource<D> resource(ErrorCatching<Expiring<D>> initial, EffectTrait<DynamicsEffect<D>> effectTrait) {
    MutableResource<D> result = new MutableResource<>() {
      private final CellRef<DynamicsEffect<D>, Cell<D>> cell = allocate(initial, effectTrait);

      @Override
      public void emit(final DynamicsEffect<D> effect) {
        augmentEffectName(effect);
        cell.emit(effect);
      }

      @Override
      public ErrorCatching<Expiring<D>> getDynamics() {
        return cell.get().dynamics;
      }

      private void augmentEffectName(DynamicsEffect<D> effect) {
        String effectName = getName(effect).orElse("anonymous effect");
        String resourceName = getName(this).orElse("anonymous resource");
        String augmentedName = effectName + " on " + resourceName + Context.get().stream().map(c -> " during " + c).collect(joining());
        name(effect, augmentedName);
      }
    };
    if (MutableResourceFlags.DETECT_BUSY_CELLS) {
      result = Profiling.profileEffects(result);
    }
    return result;
  }

  static <D extends Dynamics<?, D>> void set(MutableResource<D> resource, D newDynamics) {
    resource.emit("Set " + newDynamics, DynamicsMonad.effect(x -> newDynamics));
  }

  static <D extends Dynamics<?, D>> void set(MutableResource<D> resource, Expiring<D> newDynamics) {
    resource.emit("Set " + newDynamics, ErrorCatchingMonad.<Expiring<D>, Expiring<D>>map($ -> newDynamics)::apply);
  }

  /**
   * Turn on busy cell detection.
   *
   * <p>
   *     Calling this method once before constructing your model will profile effects on every cell.
   *     Profiling effects may be compute and/or memory intensive, and should not be used in production.
   * </p>
   * <p>
   *     If only a few cells are suspect, you can also call {@link Profiling#profileEffects}
   *     directly on just those cells, rather than profiling every cell.
   * </p>
   * <p>
   *     Call {@link Profiling#dump()} to see results.
   * </p>
   */
  static void detectBusyCells() {
    MutableResourceFlags.DETECT_BUSY_CELLS = true;
  }
}

/**
 * Private global flags for configuring cell resources for debugging.
 * Flags here are meant to be set once before constructing the model,
 * and to apply to every cell that gets built.
 */
final class MutableResourceFlags {
  public static boolean DETECT_BUSY_CELLS = false;
}
