package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

import java.util.Map;

public interface Adaptation<$Schema, Event, Activity extends ActivityInstance> {
  /* Produce */ Resources<$Schema, Event>
  /* Given   */ getResources();

  // List<ViolableConstraint> getViolableConstraints();

  /* Produce */ Map<String, ActivityType<Activity>>
  /* Given   */ getActivityTypes();

  /* For all */ <TaskId>
  /* Produce */ Task<$Schema, TaskId, Event, Activity>
  /* Given   */ createActivityTask(Activity activity);
}
