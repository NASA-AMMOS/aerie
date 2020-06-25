package gov.nasa.jpl.ammos.mpsa.aerie.banananation.state;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.events.BananaEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states.APGenStateFactory;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.states.ConsumableState;

import static gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaQuerier.ctx;
import static gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaQuerier.query;

public final class BananaStates {
  public static final APGenStateFactory factory = new APGenStateFactory(query, (ev) -> ctx.emit(BananaEvent.apgen(ev)));

  public static final ConsumableState fruit = factory.createConsumableState("fruit", 4.0);
  public static final ConsumableState peel = factory.createConsumableState("peel", 4.0);
}
