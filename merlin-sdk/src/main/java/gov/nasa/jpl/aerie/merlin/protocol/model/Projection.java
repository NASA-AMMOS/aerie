package gov.nasa.jpl.aerie.merlin.protocol.model;

import java.util.function.Function;

/**
 * A projection from a sequence-parallel expression of {@code Event}s to some accumulated {@code Effect} type.
 *
 * <p>
 * {@code Projection} extends {@link EffectTrait} by requiring an interpretation of events as concretee effects.
 * Just as {@code EffectTrait} allows the construction of algebraic expressions over effects,
 * {@code Projection} further allows atomic events to be used as variables in these expressions.
 * </p>
 *
 * @param <Event> The type of event to be mapped into effects.
 * @param <Effect> The type of effect that will be combined according to the expression structure.
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
   * A <code>Projection</code> is an <code>EffectTrait</code> with an additional function from events to effects. This
   * factory method allows a <code>Projection</code> to be constructed from these components without requiring a new
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
    return new Projection<>() {
      @Override
      public Effect atom(final Event atom) {
        return interpretation.apply(atom);
      }

      @Override
      public final Effect empty() {
        return trait.empty();
      }

      @Override
      public final Effect sequentially(final Effect prefix, final Effect suffix) {
        return trait.sequentially(prefix, suffix);
      }

      @Override
      public final Effect concurrently(final Effect left, final Effect right) {
        return trait.concurrently(left, right);
      }
    };
  }
}
