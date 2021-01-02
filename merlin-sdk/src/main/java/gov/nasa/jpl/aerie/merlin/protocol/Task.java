package gov.nasa.jpl.aerie.merlin.protocol;

@FunctionalInterface
public interface Task<$Timeline> {
  TaskStatus<$Timeline> step(Scheduler<$Timeline> scheduler);
}
