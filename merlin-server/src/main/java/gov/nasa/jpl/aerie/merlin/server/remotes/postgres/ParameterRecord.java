package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

public record ParameterRecord(String name, int order, ValueSchema schema, String unit) {}
