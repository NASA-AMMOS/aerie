package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

@FunctionalInterface
public interface Task<$Timeline, Event, TaskSpec> {
  TaskStatus<$Timeline> step(Scheduler<$Timeline, Event, TaskSpec> scheduler);
}
