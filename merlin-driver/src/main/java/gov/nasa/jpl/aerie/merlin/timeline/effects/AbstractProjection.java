package gov.nasa.jpl.aerie.merlin.timeline.effects;

/**
 * An abstract class implementing {@link EffectTrait} by delegation.
 *
 * <p>
 * Only the abstract method {@link Projection#atom} needs to be implemented by a subclass. The remaining abstract methods
 * of the {@link Projection} class will be delegated to an <code>EffectTrait</code> provided at time of construction.
 * </p>
 *
 * @param <Event> The type of event to be mapped into effects.
 * @param <Effect> The type of effect that will be combined according to the event graph structure.
 */
public abstract class AbstractProjection<Event, Effect> implements Projection<Event, Effect> {
  protected final EffectTrait<Effect> trait;

  public AbstractProjection(final EffectTrait<Effect> trait) {
    this.trait = trait;
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
}
