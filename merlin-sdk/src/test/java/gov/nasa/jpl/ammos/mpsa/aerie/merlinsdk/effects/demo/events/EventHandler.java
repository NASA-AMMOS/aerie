package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.demo.events;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.eventgraph.ActivityEvent;

public interface EventHandler<Result> {
  Result addDataRate(String binName, double amount);
  Result clearDataRate(String binName);
  Result log(String message);

  Result activity(ActivityEvent event);
}
