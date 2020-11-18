package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

@FunctionalInterface
public interface Task<$Timeline, TaskSpec> {
  TaskStatus<$Timeline> step(Scheduler<$Timeline, TaskSpec> scheduler);
}
