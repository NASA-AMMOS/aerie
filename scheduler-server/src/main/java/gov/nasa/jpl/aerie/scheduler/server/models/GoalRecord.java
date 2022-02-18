package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.scheduler.Goal;

public final record GoalRecord(GoalId id, Goal definition) {}
