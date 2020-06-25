package gov.nasa.jpl.ammos.mpsa.aerie.banananation.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.apgenstates.events.ApgenEvent;

public interface BananaEventHandler<Result> {
  Result apgen(ApgenEvent event);
}
