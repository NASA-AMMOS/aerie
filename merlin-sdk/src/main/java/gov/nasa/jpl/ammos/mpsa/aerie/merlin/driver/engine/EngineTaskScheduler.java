package gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.SimulationTask;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine.TaskScheduler;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.Deque;

/*package-local*/
final class EngineTaskScheduler<T, Event> implements TaskScheduler<T, Event> {
  private final String taskId;
  private final SimulationEngine<T, Event> engine;
  private final Deque<Pair<History<T, Event>, SimulationTask<T, Event>>> branches = new ArrayDeque<>();

  public EngineTaskScheduler(final SimulationEngine<T, Event> engine, final String taskId) {
    this.taskId = taskId;
    this.engine = engine;
  }

  /*package-local*/
  Deque<Pair<History<T, Event>, SimulationTask<T, Event>>> getBranches() {
    return this.branches;
  }

  @Override
  public void spawn(final History<T, Event> forkPoint, final SimulationTask<T, Event> task) {
    this.branches.push(Pair.of(forkPoint, task));
  }

  @Override
  public void defer(final Duration delay, final SimulationTask<T, Event> task) {
    this.engine.defer(delay, task);
  }

  @Override
  public void await(final String taskToAwait, final SimulationTask<T, Event> task) {
    this.engine.await(taskToAwait, task);
  }

  @Override
  public void complete() {
    this.engine.markCompleted(this.taskId);
  }
}
