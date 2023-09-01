package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.Map;

public record ComputedAttributeDefinition(ValueSchema schema, Map<String, String> units) {}
