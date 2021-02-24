package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.Checkpoint;
import gov.nasa.jpl.aerie.merlin.timeline.History;
import gov.nasa.jpl.aerie.merlin.timeline.Query;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class TaskFrame<$Timeline, Signal> {
  private History<$Timeline> tip;
  private final Deque<Pair<History<$Timeline>, Signal>> branches;
  private final Optional<TaskFrame<$Timeline, Signal>> continuation;

  private TaskFrame(FrameBuilder<$Timeline, Signal> builder) {
    this.tip = builder.tip;
    this.branches = builder.branches;
    this.continuation = builder.continuation;
  }

  public static <$Timeline, Signal>
  TaskFrame<$Timeline, Signal>
  of(final History<$Timeline> tip, final Consumer<FrameBuilder<$Timeline, Signal>> body) {
    final var builder = new FrameBuilder<$Timeline, Signal>(tip, Optional.empty());
    body.accept(builder);
    return builder.yield();
  }

  public static <$Timeline, Signal>
  History<$Timeline> runToCompletion(
      TaskFrame<$Timeline, Signal> frame,
      final BiConsumer<Signal, FrameBuilder<$Timeline, Signal>> executor)
  {
    while (!frame.isDone()) frame = frame.step(executor);

    return frame.getTip();
  }


  public History<$Timeline> getTip() {
    return this.tip;
  }

  public boolean isDone() {
    return this.branches.isEmpty() && this.continuation.isEmpty();
  }

  public TaskFrame<$Timeline, Signal> step(final BiConsumer<Signal, FrameBuilder<$Timeline, Signal>> executor) {
    if (!this.branches.isEmpty()) {
      // We have uncommitted child branches.
      // Start wandering down the next one.
      final var branch = this.branches.pop();
      final var branchTip = branch.getLeft();
      final var signal = branch.getRight();

      final var builder = new FrameBuilder<>(branchTip, Optional.of(this));
      executor.accept(signal, builder);

      return builder.yield();
    } else if (this.continuation.isPresent()) {
      // We're done here, but a parent frame is waiting for us to finish.
      // Commit our tip back up to the parent.
      final var parent = this.continuation.orElseThrow();
      parent.tip = parent.tip.join(this.tip);
      return parent;
    } else {
      // There's nothing at all left to do.
      return this;
    }
  }


  public static final class FrameBuilder<$Timeline, Signal> {
    private boolean yielded = false;
    private History<$Timeline> tip;
    private final Deque<Pair<History<$Timeline>, Signal>> branches = new ArrayDeque<>();
    private final Optional<TaskFrame<$Timeline, Signal>> continuation;

    private FrameBuilder(final History<$Timeline> tip, final Optional<TaskFrame<$Timeline, Signal>> continuation) {
      this.tip = tip;
      this.continuation = continuation;
    }

    private TaskFrame<$Timeline, Signal> yield() {
      this.yielded = true;
      return new TaskFrame<>(this);
    }


    public Checkpoint<$Timeline> now() {
      if (this.yielded) throw new RuntimeException("Unexpected action from task after yield");

      return this.tip::ask;
    }

    public <Event> void emit(final Event event, final Query<? super $Timeline, Event, ?> query) {
      if (this.yielded) throw new RuntimeException("Unexpected action from task after yield");

      this.tip = this.tip.emit(event, query);
    }

    public void signal(final Signal target) {
      if (this.yielded) throw new RuntimeException("Unexpected action from task after yield");

      this.tip = this.tip.fork();
      this.branches.push(Pair.of(this.tip, target));
    }
  }
}
