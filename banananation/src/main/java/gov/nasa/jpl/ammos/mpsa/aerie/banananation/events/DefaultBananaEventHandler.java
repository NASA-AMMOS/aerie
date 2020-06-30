package gov.nasa.jpl.ammos.mpsa.aerie.banananation.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.independentstates.events.IndependentStateEvent;

// This can be mechanically derived from `EventHandler`.
public interface DefaultBananaEventHandler<Result> extends BananaEventHandler<Result> {
  Result unhandled();

  @Override
  default Result independent(IndependentStateEvent event) {
    return unhandled();
  }
}
