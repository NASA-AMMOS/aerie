package gov.nasa.jpl.aerie.merlin.driver.retracing.tracing;

import gov.nasa.jpl.aerie.merlin.driver.retracing.engine.TaskId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.CellId;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionLog {
  private final Map<TaskId, Writer> writers;

  public ActionLog() {
    this.writers = new HashMap<>();
  }

  public Writer writer(TaskId taskId) {
    if (!this.writers.containsKey(taskId)) {
      this.writers.put(taskId, new Writer());
    }
    return this.writers.get(taskId);
  }

  public static class Writer {
    private final List<Action> log;

    public Writer() {
      this.log = new ArrayList<>();
    }

    public <State> void get(CellId<State> token, State state$) {
      log.add(new Action.Read<>(token, state$));
    }

    public <EventType> void emit(EventType event, Topic<EventType> topic) {
      log.add(new Action.Emit<>(event, topic));
    }

    public void spawn(TaskFactory<?> state) {
      log.add(new Action.Spawn(state));
    }

    public void yield(TaskStatus<?> status) {
      log.add(new Action.Yield(status));
    }
  }

  public sealed interface Action {
    record Read<State>(CellId<State> token, State result) implements Action {}
    record Emit<EventType>(EventType event, Topic<EventType> topic) implements Action {}
    record Spawn(TaskFactory<?> state) implements Action {}
    record Yield(TaskStatus<?> status) implements Action {}
  }
}
