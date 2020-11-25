package gov.nasa.jpl.ammos.mpsa.aerie.merlin.driver.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlin.timeline.History;
import gov.nasa.jpl.ammos.mpsa.aerie.merlin.protocol.Task;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Deque;

/*package-local*/
final class TaskFrame<$Timeline> {
  public History<$Timeline> tip;
  public Deque<Pair<History<$Timeline>, Task<$Timeline>>> branches;

  public TaskFrame(
      final History<$Timeline> tip,
      final Deque<Pair<History<$Timeline>, Task<$Timeline>>> branches)
  {
    this.tip = tip;
    this.branches = branches;
  }

  public boolean hasBranches() {
    return !this.branches.isEmpty();
  }

  public Pair<History<$Timeline>, Task<$Timeline>> popBranch() {
    return this.branches.pop();
  }

  public void pushBranch(final History<$Timeline> startTime, final Task<$Timeline> task) {
    this.branches.push(Pair.of(startTime, task));
  }
}
