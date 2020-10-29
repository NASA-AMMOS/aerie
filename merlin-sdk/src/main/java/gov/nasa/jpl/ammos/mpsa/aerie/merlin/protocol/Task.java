package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

@FunctionalInterface
public interface Task<$Timeline, TaskId, Event, Activity> {
  ActivityStatus<TaskId> step(Scheduler<$Timeline, TaskId, Event, Activity> scheduler);
}
