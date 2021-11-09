package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.timeline.CausalEventSource;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Event;
import gov.nasa.jpl.aerie.merlin.driver.timeline.EventGraph;
import gov.nasa.jpl.aerie.merlin.driver.timeline.LiveCells;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Query;
import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A TaskFrame is a record of a completed task and its remaining branches.
 *
 * <p>
 * <pre>
 *   base |-> branches[0].left |-> branches[1].left   ... |-> branches[n].left     |-> tip
 *        +-> parent.tip       +-> branches[0].right      +-> branches[n-1].right  +-> branches[n].right
 * </pre>
 * </p>
 *
 * <p>
 * When there are no branches, this simplifies to:
 * <pre>
 *   base |-> tip
 *        +-> parent.tip
 * </pre>
 * In which case, the frame can evolve no further; we can collapse this down to a single graph
 * and make it the new parent tip.
 * </p>
*/
public final class TaskFrame<Signal> {
  private final Optional<TaskFrame<Signal>> parent;
  private final CausalEventSource base;

  private EventGraph<Event> tip;
  private final Deque<Triple<CausalEventSource, LiveCells, Signal>> branches;

  private TaskFrame(
      final Optional<TaskFrame<Signal>> parent,
      final CausalEventSource base,
      final FrameBuilder<Signal> builder
  ) {
    this.parent = parent;
    this.base = base;

    this.tip = builder.events.commit(EventGraph.empty());
    this.branches = builder.branches;
  }

  public static <Signal>
  TaskFrame<Signal> of(final LiveCells context, final Consumer<FrameBuilder<Signal>> body) {
    final var builder = new FrameBuilder<Signal>(context);
    body.accept(builder);
    return new TaskFrame<>(Optional.empty(), new CausalEventSource(), builder);
  }

  public static <Signal>
  EventGraph<Event> runToCompletion(
      final Iterable<Signal> jobs,
      final LiveCells context,
      final BiConsumer<Signal, FrameBuilder<Signal>> executor
  ) {
    var frame = TaskFrame.<Signal>of(context, builder -> jobs.forEach(builder::signal));

    while (!frame.isDone()) frame = frame.step(executor);

    return frame.base.commit(frame.tip);
  }

  public boolean isDone() {
    return this.branches.isEmpty() && this.parent.isEmpty();
  }

  public TaskFrame<Signal> step(final BiConsumer<Signal, FrameBuilder<Signal>> executor) {
    if (!this.branches.isEmpty()) {
      // We have uncommitted child branches.
      // Start wandering down the next one.
      final var branch = this.branches.pop();
      final var branchBase = branch.getLeft();
      final var branchContext = branch.getMiddle();
      final var signal = branch.getRight();

      final var builder = new FrameBuilder<Signal>(branchContext);
      executor.accept(signal, builder);

      return new TaskFrame<>(Optional.of(this), branchBase, builder);
    } else if (this.parent.isPresent()) {
      // We're done here, but a parent frame is waiting for us to finish.
      // Commit our tip back up to the parent.
      final var parent = this.parent.orElseThrow();
      parent.tip = this.base.commit(EventGraph.concurrently(parent.tip, this.tip));
      return parent;
    } else {
      // There's nothing at all left to do.
      assert this.isDone();
      return this;
    }
  }

  public static final class FrameBuilder<Signal> {
    private CausalEventSource events;
    private LiveCells cells;
    private final Deque<Triple<CausalEventSource, LiveCells, Signal>> branches = new ArrayDeque<>();

    private FrameBuilder(final LiveCells context) {
      this.events = new CausalEventSource();
      this.cells = new LiveCells(this.events, context);
    }

    public <State> Optional<State> getState(final Query<State> query) {
      return this.cells.getState(query);
    }

    public Optional<Duration> getExpiry(final Query<?> query) {
      return this.cells.getExpiry(query);
    }

    public void emit(final Event event) {
      this.events.add(event);
    }

    public void signal(final Signal target) {
      // If we haven't emitted any events, subscribe the target to the previous branch point instead.
      // This avoids making long chains of LiveCells over segments where no events have actually been accumulated.
      if (this.events.isEmpty() && !this.branches.isEmpty()) {
        this.branches.push(Triple.of(new CausalEventSource(), this.branches.peek().getMiddle(), target));
      } else {
        this.branches.push(Triple.of(this.events, this.cells, target));

        this.events = new CausalEventSource();
        this.cells = new LiveCells(this.events, this.cells);
      }
    }
  }
}
