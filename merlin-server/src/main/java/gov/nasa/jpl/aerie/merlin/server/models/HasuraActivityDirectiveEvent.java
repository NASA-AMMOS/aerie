package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import java.util.Map;

public record HasuraActivityDirectiveEvent(
    PlanId planId,
    ActivityDirectiveId activityDirectiveId,
    String activityTypeName,
    Map<String, SerializedValue> arguments,
    Timestamp argumentsModifiedTime) {}
