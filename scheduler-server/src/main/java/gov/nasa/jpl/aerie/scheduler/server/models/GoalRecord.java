package gov.nasa.jpl.aerie.scheduler.server.models;

public record GoalRecord(
    GoalId id,
    String name,
    GoalSource definition,
    boolean simulateAfter) {}
