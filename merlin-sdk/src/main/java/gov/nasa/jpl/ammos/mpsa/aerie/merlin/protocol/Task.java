package gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol;

@FunctionalInterface
public interface Task<$Timeline> {
  TaskStatus<$Timeline> step(Scheduler<$Timeline> scheduler);
}
