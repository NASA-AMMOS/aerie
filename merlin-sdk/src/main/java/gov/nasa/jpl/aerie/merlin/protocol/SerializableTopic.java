package gov.nasa.jpl.aerie.merlin.protocol;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

public interface SerializableTopic<Event> {
  String getName();
  Query<?, Event, ?> getQuery();
  SerializedValue serializeEvent(Event event);
  ValueSchema getValueSchema();
}
