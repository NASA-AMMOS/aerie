package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.List;

public final record ActivityType(
    String name,
    List<Parameter> parameters,
    List<String> requiredParameters,
    ValueSchema computedAttributesValueSchema
) {}
