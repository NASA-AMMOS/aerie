package gov.nasa.jpl.aerie.merlin.driver.engine;

/** A typed wrapper for signal IDs. */
public sealed interface SignalId {
  /** A signal controlled by a task. */
  record TaskSignalId(TaskId id) implements SignalId {}

  /** A signal controlled by a condition. */
  record ConditionSignalId(ConditionId id) implements SignalId {}

  static TaskSignalId forTask(final TaskId task) {
    return new TaskSignalId(task);
  }

  static ConditionSignalId forCondition(final ConditionId condition) {
    return new ConditionSignalId(condition);
  }
}
