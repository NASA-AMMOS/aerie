package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

@FunctionalInterface
public interface Task<$Timeline, Event, Activity> {
  ActivityStatus<$Timeline> step(Scheduler<$Timeline, Event, Activity> scheduler);
}
