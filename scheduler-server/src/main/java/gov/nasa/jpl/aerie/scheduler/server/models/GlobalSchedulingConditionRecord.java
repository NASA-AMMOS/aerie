package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.scheduler.model.ActivityTypeList;

public record GlobalSchedulingConditionRecord(
    GlobalSchedulingConditionSource source,
    ActivityTypeList activityTypes,
    boolean enabled
) {}
