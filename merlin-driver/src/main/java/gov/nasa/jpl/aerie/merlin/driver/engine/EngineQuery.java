package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.driver.Query;

public record EngineQuery<$Schema, Event, State>(
    gov.nasa.jpl.aerie.merlin.timeline.Query<$Schema, Event, State> query
) implements Query<$Schema, Event, State> {}
