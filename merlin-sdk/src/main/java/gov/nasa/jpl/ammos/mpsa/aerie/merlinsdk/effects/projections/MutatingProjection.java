package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.projections;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.Projection;

import java.util.function.Function;

public abstract class MutatingProjection<Var, Accumulator, Effect>
    implements Projection<Var, Function<Accumulator, Effect>>
{
  protected abstract Accumulator fork(final Accumulator accumulator);
  protected abstract void apply(final Accumulator accumulator, final Effect effect);


  protected final Projection<Var, Effect> projection;

  public MutatingProjection(final Projection<Var, Effect> projection) {
    this.projection = projection;
  }

  @Override
  public final Function<Accumulator, Effect> atom(final Var atom) {
    return model -> {
      final var effects = this.projection.atom(atom);
      this.apply(model, effects);
      return effects;
    };
  }

  @Override
  public final Function<Accumulator, Effect> empty() {
    return model -> this.projection.empty();
  }

  @Override
  public final Function<Accumulator, Effect> sequentially(final Function<Accumulator, Effect> prefix, final Function<Accumulator, Effect> suffix) {
    return model -> {
      final var effects1 = prefix.apply(model);
      final var effects2 = suffix.apply(model);
      return this.projection.sequentially(effects1, effects2);
    };
  }

  @Override
  public final Function<Accumulator, Effect> concurrently(final Function<Accumulator, Effect> left, final Function<Accumulator, Effect> right) {
    return model -> {
      final var effects = this.projection.concurrently(
          left.apply(this.fork(model)),
          right.apply(this.fork(model)));
      this.apply(model, effects);
      return effects;
    };
  }
}
