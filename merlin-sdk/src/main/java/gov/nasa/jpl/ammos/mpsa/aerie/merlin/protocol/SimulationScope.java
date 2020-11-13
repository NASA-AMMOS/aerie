package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

public interface SimulationScope<$Schema, Event, Activity extends ActivityInstance> {
  /* Produce */ Resources<$Schema, Event>
  /* Given   */ getResources();

  /* For all */ <$Timeline extends $Schema>
  /* Produce */ Task<$Timeline, Event, Activity>
  /* Given   */ createActivityTask(Activity activity);
}
