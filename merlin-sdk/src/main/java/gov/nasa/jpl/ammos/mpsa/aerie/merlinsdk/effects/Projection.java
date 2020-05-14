package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects;

import java.util.function.Function;

/**
 * A projection from an {@link EventGraph} to some accumulated effect type.
 *
 * <p>
 * <code>Projection</code> extends {@link EffectTrait} by requiring a mapping from events to effects. Just as
 * <code>EffectTrait</code> can be thought of as allowing the construction of algebraic expressions over effects,
 * <code>Projection</code> further allows atomic events to be used as variables in these expressions.
 * </p>
 *
 * @param <Event> The type of event to be mapped into effects.
 * @param <Effect> The type of effect that will be combined according to the event graph structure.
 * @see EffectTrait
 */
public interface Projection<Event, Effect> extends EffectTrait<Effect> {
  /**
   * Interpret an atomic event as an effect.
   *
   * @param atom An atomic event to be interpreted as an effect.
   * @return The interpretation of the given atomic event as an effect.
   */
  Effect atom(Event atom);

  /**
   * A factory method for constructing projections from an {@link EffectTrait} and an interpretation of events in that
   * trait.
   *
   * A <code>Projection</code> is an <code>EffectTrait</code> with an additionam function from events to effects. This
   * factory method allows a <code>Projection</code>> to be constructed from these components without requiring a new
   * class declaration.
   *
   * @param trait The trait against which the {@link #empty}, {@link #sequentially}, and {@link #concurrently} methods
   *              shall be implemented.
   * @param interpretation The interpretation of events as effects against which the {@link #atom} method shall be
   *                       implemented.
   * @param <Event> The type of event to project from.
   * @param <Effect> The type of effect to project onto.
   * @return A <code>Projection</code> behaving as a composite of the given effect trait and event interpretation.
   */
  static <Event, Effect> Projection<Event, Effect> from(
      final EffectTrait<Effect> trait,
      final Function<Event, Effect> interpretation
  ) {
    return new AbstractProjection<>(trait) {
      @Override
      public Effect atom(final Event atom) {
        return interpretation.apply(atom);
      }
    };
  }
}
