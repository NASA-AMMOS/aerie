package gov.nasa.jpl.aerie.scheduler.server.models;

public record GoalRecord(GoalId id, GoalSource definition, boolean enabled) {}
