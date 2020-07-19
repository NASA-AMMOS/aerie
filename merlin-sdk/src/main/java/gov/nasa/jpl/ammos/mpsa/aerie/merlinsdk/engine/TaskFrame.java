package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.timeline.History;
import org.apache.commons.lang3.tuple.Pair;
import org.pcollections.PStack;

public final class TaskFrame<T, Event> {
  public History<T, Event> tip;
  public PStack<Pair<History<T, Event>, SimulationTask<T, Event>>> branches;

  public TaskFrame(
      final History<T, Event> tip,
      final PStack<Pair<History<T, Event>, SimulationTask<T, Event>>> branches)
  {
    this.tip = tip;
    this.branches = branches;
  }
}
