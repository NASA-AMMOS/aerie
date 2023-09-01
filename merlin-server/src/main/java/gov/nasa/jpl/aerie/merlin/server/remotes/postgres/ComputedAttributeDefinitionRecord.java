package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.Map;

public record ComputedAttributeDefinitionRecord(ValueSchema valueSchema, Map<String, String> units) {}
