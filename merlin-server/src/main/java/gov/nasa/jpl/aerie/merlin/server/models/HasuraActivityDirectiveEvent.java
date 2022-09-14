package gov.nasa.jpl.aerie.merlin.server.models;

import java.util.Map;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

public record HasuraActivityDirectiveEvent
(
    PlanId planId,
    ActivityDirectiveId activityDirectiveId,
    String activityTypeName,
    Map<String, SerializedValue> arguments
) { }
