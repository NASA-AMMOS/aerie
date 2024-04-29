package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.TaskFactory;

public interface Scheduler {
  <State> State get(CellId<State> cellId);

  <Event> void emit(Event event, Topic<Event> topic);

  void spawn(TaskFactory<?> task);
  <T> void startActivity(T activity, Topic<T> inputTopic);
  <T> void endActivity(T result, Topic<T> outputTopic);
  <ActivityDirectiveId> void startDirective(ActivityDirectiveId directiveId, Topic<ActivityDirectiveId> activityTopic);

  void pushSpan();

  void popSpan();
}
