package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Deque;

/*package-local*/
final class TaskFrame<T, Event> {
  public History<T, Event> tip;
  public Deque<Pair<History<T, Event>, SimulationTask<T, Event>>> branches;

  public TaskFrame(
      final History<T, Event> tip,
      final Deque<Pair<History<T, Event>, SimulationTask<T, Event>>> branches)
  {
    this.tip = tip;
    this.branches = branches;
  }

  public boolean hasBranches() {
    return !this.branches.isEmpty();
  }

  public Pair<History<T, Event>, SimulationTask<T, Event>> popBranch() {
    return this.branches.pop();
  }

  public void pushBranch(final History<T, Event> startTime, final SimulationTask<T, Event> task) {
    this.branches.push(Pair.of(startTime, task));
  }
}
