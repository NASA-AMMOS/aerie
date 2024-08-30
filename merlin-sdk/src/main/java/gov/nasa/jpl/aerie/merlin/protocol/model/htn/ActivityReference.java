package gov.nasa.jpl.aerie.merlin.protocol.model.htn;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.HashMap;
import java.util.Map;

public record ActivityReference(
    String activityType,
    Map<String, SerializedValue> parameters
) {}
