package gov.nasa.jpl.aerie.scheduler.server.models;

public final record GoalRecord(GoalId id, SchedulingDSL.GoalSpecifier definition) {}
