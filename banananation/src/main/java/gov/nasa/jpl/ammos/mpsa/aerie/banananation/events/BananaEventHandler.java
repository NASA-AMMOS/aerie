package gov.nasa.jpl.ammos.mpsa.aerie.banananation.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.models.activities.ActivityEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.contrib.models.independent.events.IndependentStateEvent;

public interface BananaEventHandler<Result> {
  Result independent(IndependentStateEvent event);
  Result activity(ActivityEvent event);
}
