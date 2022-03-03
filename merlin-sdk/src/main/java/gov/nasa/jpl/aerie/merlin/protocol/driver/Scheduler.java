package gov.nasa.jpl.aerie.merlin.protocol.driver;

import gov.nasa.jpl.aerie.merlin.protocol.model.Task;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.Map;

public interface Scheduler {
  <State> State get(Query<?, State> query);

  <Event> void emit(Event event, Query<? super Event, ?> query);

  <Return> String spawn(Task<Return> task);

  <Input, Output> String spawn(DirectiveTypeId<Input, Output> directiveType, Input input, Task<Output> task);

  String spawn(String type, Map<String, SerializedValue> arguments);
}
