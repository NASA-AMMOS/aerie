package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

@FunctionalInterface
public interface Task<$Schema, TaskId, Event, Activity> {
  ActivityStatus<TaskId> step(Scheduler<? extends $Schema, TaskId, Event, Activity> scheduler);
}
