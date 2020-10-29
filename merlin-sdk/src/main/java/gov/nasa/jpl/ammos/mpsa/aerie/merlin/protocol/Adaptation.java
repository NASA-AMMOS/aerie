package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.SimulationTimeline;

import java.util.Map;

public interface Adaptation<Event, Activity extends ActivityInstance> {
  /* Produce */ Map<String, ActivityType<Activity>>
  /* Given   */ getActivityTypes();

  /* For all */ <$Timeline, TaskId>
  /* Produce */ SimulationContext<$Timeline, Activity, ? extends Task<$Timeline, TaskId, Event, Activity>>
  /* Given   */ initializeSimulation(SimulationTimeline<$Timeline, Event> timeline);
}
