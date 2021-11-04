package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.model.Applicator;
import gov.nasa.jpl.aerie.merlin.protocol.model.EffectTrait;
import gov.nasa.jpl.aerie.merlin.protocol.model.Projection;

import java.util.function.Function;

public final class CellRef<Event, CellType> {
  private final Query<?, Event, CellType> query;

  private CellRef(Query<?, Event, CellType> query) {
    this.query = query;
  }

  public static <Event, Effect, CellType>
  CellRef<Event, CellType> allocate(
      final CellType initialState,
      final Applicator<Effect, CellType> applicator,
      final EffectTrait<Effect> trait,
      final Function<Event, Effect> eventToEffect
  ) {
    final var query = ModelActions.context.get().allocate(
        initialState,
        applicator,
        Projection.from(trait, eventToEffect));
    return new CellRef<>(query);
  }

  public static <Effect, CellType extends Cell<Effect, CellType>>
  CellRef<Effect, CellType> allocate(final CellType initialState, final EffectTrait<Effect> trait) {
    return allocate(initialState, new CellApplicator<>(), trait, $ -> $);
  }

  public static <Event, Effect, CellType extends Cell<Effect, CellType>>
  CellRef<Event, CellType> allocate(final CellType initialState, final EffectTrait<Effect> trait, final Function<Event, Effect> eventToEffect) {
    return allocate(initialState, new CellApplicator<>(), trait, eventToEffect);
  }

  public static <Effect, CellType>
  CellRef<Effect, CellType> allocate(final CellType initialState, final Applicator<Effect, CellType> applicator, final EffectTrait<Effect> trait) {
    return allocate(initialState, applicator, trait, $ -> $);
  }

  public CellType get() {
    return ModelActions.context.get().ask(this.query);
  }

  public void emit(final Event event) {
    ModelActions.context.get().emit(event, this.query);
  }
}
