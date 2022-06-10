package gov.nasa.jpl.aerie.scheduler.server.models;

public record GoalRecord(GoalId id, SchedulingDSL.GoalSpecifier definition, boolean enabled) {}
