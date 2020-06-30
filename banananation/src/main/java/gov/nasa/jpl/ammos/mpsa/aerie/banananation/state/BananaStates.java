package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.events.BananaEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.IndependentStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.states.ConsumableState;

import static gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaQuerier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaQuerier.query;

public final class BananaStates {
  public static final IndependentStateFactory factory = new IndependentStateFactory(query, (ev) -> ctx.emit(BananaEvent.independent(ev)));

  public static final ConsumableState fruit = factory.createConsumableState("fruit", 4.0);
  public static final ConsumableState peel = factory.createConsumableState("peel", 4.0);
}
