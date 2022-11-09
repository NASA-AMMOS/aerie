package gov.nasa.jpl.aerie.scheduler.server.models;

public record GlobalSchedulingConditionRecord(
    GlobalSchedulingConditionSource source,
    boolean enabled
) {}
