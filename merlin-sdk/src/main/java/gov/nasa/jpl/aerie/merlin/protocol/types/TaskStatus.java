package gov.nasa.jpl.aerie.merlin.protocol.types;

import gov.nasa.jpl.aerie.merlin.protocol.model.Condition;

public sealed interface TaskStatus<$Timeline> {
  record Completed<$Timeline>() implements TaskStatus<$Timeline> {}

  record Delayed<$Timeline>(Duration delay) implements TaskStatus<$Timeline> {}

  record AwaitingTask<$Timeline>(String target) implements TaskStatus<$Timeline> {}

  record AwaitingCondition<$Timeline>(Condition<? super $Timeline> condition) implements TaskStatus<$Timeline> {}


  static <$Timeline> Completed<$Timeline> completed() {
    return new Completed<>();
  }

  static <$Timeline> Delayed<$Timeline> delayed(final Duration delay) {
    return new Delayed<>(delay);
  }

  static <$Timeline> AwaitingTask<$Timeline> awaiting(final String id) {
    return new AwaitingTask<>(id);
  }

  static <$Timeline> AwaitingCondition<$Timeline> awaiting(final Condition<? super $Timeline> condition) {
    return new AwaitingCondition<>(condition);
  }
}
