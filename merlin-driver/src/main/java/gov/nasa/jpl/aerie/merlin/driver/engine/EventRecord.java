package gov.nasa.jpl.aerie.merlin.driver.engine;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

public record EventRecord(int topicId, SpanId provenance, SerializedValue value) {}
