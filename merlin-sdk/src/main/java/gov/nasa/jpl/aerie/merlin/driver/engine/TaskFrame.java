package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.timeline.History;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Deque;

/*package-local*/
final class TaskFrame<$Timeline> {
  public History<$Timeline> tip;
  public Deque<Pair<History<$Timeline>, String>> branches;

  public TaskFrame(
      final History<$Timeline> tip,
      final Deque<Pair<History<$Timeline>, String>> branches)
  {
    this.tip = tip;
    this.branches = branches;
  }

  public Pair<History<$Timeline>, String> popBranch() {
    return this.branches.pop();
  }
}
