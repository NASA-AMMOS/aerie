package gov.nasa.jpl.aerie.scheduler.server.models;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;

import java.util.List;
import java.util.Map;

public record Specification(
    PlanId planId,
    long planRevision,
    List<GoalRecord> goalsByPriority,
    Timestamp horizonStartTimestamp,
    Timestamp horizonEndTimestamp,
    Map<String, SerializedValue> simulationArguments,
    boolean analysisOnly,
    List<GlobalSchedulingConditionRecord> globalSchedulingConditions
) {}
