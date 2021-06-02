package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.Projection;
import gov.nasa.jpl.aerie.merlin.protocol.Query;

public final class CellRef<Event, CellType extends Cell<Event, CellType>> {
  private final Query<?, Event, CellType> query;

  public CellRef(final CellType initialState) {
    this.query = ModelActions.context.get().allocate(
        new CellProjection<>(initialState.effectTrait()),
        new CellApplicator<>(initialState));
  }

  public CellType get() {
    return ModelActions.context.get().ask(this.query);
  }

  public void emit(final Event event) {
    ModelActions.context.get().emit(event, this.query);
  }


  private static final class CellProjection<Effect> implements Projection<Effect, Effect> {
    private final EffectTrait<Effect> trait;

    public CellProjection(final EffectTrait<Effect> trait) {
      this.trait = trait;
    }

    @Override
    public Effect atom(final Effect atom) {
      return atom;
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
}
