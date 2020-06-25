package gov.nasa.jpl.ammos.mpsa.aerie.banananation.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events.ApgenEvent;

// This can be mechanically derived from `EventHandler`.
public interface DefaultBananaEventHandler<Result> extends BananaEventHandler<Result> {
  Result unhandled();

  @Override
  default Result apgen(ApgenEvent event) {
    return unhandled();
  }
}
