package gov.nasa.jpl.aerie.merlin.framework;

import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;

import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

public sealed interface TaskTrace {
  List<Writer> writers = new ArrayList<>();

  record Empty() implements TaskTrace {
    @Override
    public String toString() {
      return render(this);
    }
  }
  record Read(CellId<?> query, List<ReadRecord> readRecords) implements TaskTrace {
    @Override
    public String toString() {
      return render(this);
    }
  }
  record ActionContainer(Action action, ModifiableOnce<TaskTrace> rest) implements TaskTrace {
    @Override
    public String toString() {
      return render(this);
    }
  }
  record Exit<T>(T returnValue) implements TaskTrace {
    @Override
    public String toString() {
      return render(this);
    }
  }
  record ReadRecord(Object value, ModifiableOnce<TaskTrace> rest) {}

  static String render(TaskTrace trace) {
    if (trace instanceof Empty t) {
      return "unfinished...";
    } else if (trace instanceof Read t) {
      final var result = new StringBuilder();
      result.append("read(");
      result.append(t.query.toString());
      result.append("){\n");
      for (final var readRecord : t.readRecords()) {
        result.append(indent(readRecord.value().toString() + "->[") + "\n");
        result.append(indent(indent(render(readRecord.rest().get()))));
        result.append("\n" + indent("]") + "\n");
      }
      result.append("}");
      return result.toString();
    } else if (trace instanceof ActionContainer t) {
      return t.action.toString() + ";\n" + render(t.rest().get());
    } else if (trace instanceof Exit<?> t) {
      return "exit(" + t.returnValue() + ");\n";
    } else {
      throw new Error("Unhandled variant of TaskTrace: " + trace);
    }
  }

  private static String indent(final String s) {
    return joinLines(s.lines().map(line -> "  " + line).toList());
  }

  private static String joinLines(final Iterable<String> result) {
    return String.join("\n", result);
  }

  static TaskTrace empty() {
    return new Empty();
  }

  class Writer {
    private boolean closed = false;
    public final ModifiableOnce<TaskTrace> trace;
    private ModifiableOnce<TaskTrace> tip;

    Writer() {
      this(new ModifiableOnce<>(TaskTrace.empty()));
    }

    Writer(ModifiableOnce<TaskTrace> trace) {
      this.trace = trace;
      this.tip = this.trace;
      writers.add(this);
    }

    public <ReturnedType> void read(final CellId<?> query, final ReturnedType read) {
      if (closed) throw new IllegalStateException("Cannot call methods on closed writer");
      final var newTip = new ModifiableOnce<>(TaskTrace.empty());
      final var readRecords = new ArrayList<ReadRecord>();
      readRecords.add(new ReadRecord(read, newTip));
      this.tip.replace(new Read(query, readRecords));
      this.tip = newTip;
    }

    public <Event> void emit(Event event, Topic<Event> topic) {
      if (closed) throw new IllegalStateException("Cannot call methods on closed writer");
      final var newTip = new ModifiableOnce<>(TaskTrace.empty());
      this.tip.replace(new ActionContainer(new Action.Emit<>(event, topic), newTip));
      this.tip = newTip;
    }

    public void spawn() {
      if (closed) throw new IllegalStateException("Cannot call methods on closed writer");
      final var newTip = new ModifiableOnce<>(TaskTrace.empty());
      this.tip.replace(new ActionContainer(new Action.Spawn(), newTip));
      this.tip = newTip;
    }

    public TaskTrace get() {
      this.closed = true;
      return this.trace.get();
    }

    public <T> void yield(TaskStatus<T> taskStatus) {
      if (closed) throw new IllegalStateException("Cannot call methods on closed writer");

      if (taskStatus instanceof TaskStatus.Completed<T> t) {
        this.tip.replace(new Exit<>(t.returnValue()));
        this.closed = true;
      } else {
        final var newTip = new ModifiableOnce<>(TaskTrace.empty());
        this.tip.replace(new ActionContainer(new Action.Yield(taskStatus), newTip));
        this.tip = newTip;
      }
    }
  }

  class Cursor {
    private boolean closed = false;
    private ModifiableOnce<TaskTrace> trace;
    private boolean awaitingRead = false;
    private final List<Object> reads = new ArrayList<>();
    private int numSteps = 0;

    public Cursor(TaskTrace trace) {
      this.trace = new ModifiableOnce<>(trace);
    }

    Action nextAction() {
      if (closed) throw new IllegalStateException("Cannot call methods on closed Cursor");
      final var rbt = this.trace.get();
      if (rbt instanceof TaskTrace.Empty) {
        throw new NoSuchElementException();
      } else if (rbt instanceof Read read) {
        awaitingRead = true;
        return new Action.Read(read.query());
      } else if (rbt instanceof ActionContainer actionContainer) {
        this.trace = actionContainer.rest;
        if (actionContainer.action() instanceof Action.Yield) {
          numSteps++;
        }
        return actionContainer.action();
      } else if (rbt instanceof Exit<?> exit) {
        return new Action.Yield(TaskStatus.completed(exit.returnValue()));
      } else {
        throw new Error("Unhandled variant of RBT " + rbt);
      }
    }

    boolean hasNext() {
      if (closed) throw new IllegalStateException("Cannot call methods on closed Cursor");
      if (awaitingRead) throw new IllegalStateException("Should call read next, not hasNext");
      return !(this.trace.get() instanceof TaskTrace.Empty);
    }

    <ReadValue> Optional<TaskResumptionInfo> read(ReadValue readValue) {
      if (closed) throw new IllegalStateException("Cannot call methods on closed Cursor");
      if (!awaitingRead) {
        throw new IllegalStateException("Called read, but the last action was not a read");
      }
      awaitingRead = false;
      final var readRecords = ((Read) this.trace.get()).readRecords();
      for (final var readRecord : readRecords) {
        if (Objects.equals(readRecord.value(), readValue)) {
          this.trace = readRecord.rest;
          return Optional.empty();
        }
      }
      closed = true;
      final ModifiableOnce<TaskTrace> rest = new ModifiableOnce<>(TaskTrace.empty());
      readRecords.add(new ReadRecord(readValue, rest));
      return Optional.of(new TaskResumptionInfo(this.reads, this.numSteps, new Writer(rest)));
    }
  }

  record TaskResumptionInfo(List<Object> reads, int numSteps, Writer writer) {}

  static Writer writer() {
    return new Writer();
  }

  static Cursor cursor(TaskTrace rbt) {
    return new Cursor(rbt);
  }

  class ModifiableOnce<T> {
    private T contents;
    private boolean modified = false;
    private ModifiableOnce(T empty) {
      this.contents = empty;
    }

    static <T> ModifiableOnce<T> of(T empty) {
      return new ModifiableOnce<>(empty);
    }

    void replace(T replacement) {
      if (this.modified) throw new IllegalArgumentException("Modified twice!");
      this.contents = replacement;
    }

    T get() {
      return contents;
    }
  }
}
