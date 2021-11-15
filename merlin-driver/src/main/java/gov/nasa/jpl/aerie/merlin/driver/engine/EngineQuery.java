package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.driver.timeline.Query;
import gov.nasa.jpl.aerie.merlin.driver.timeline.Topic;

public record EngineQuery<Event, State> (Topic<Event> topic, Query<State> query)
    implements gov.nasa.jpl.aerie.merlin.protocol.driver.Query<Event, State>
{}
