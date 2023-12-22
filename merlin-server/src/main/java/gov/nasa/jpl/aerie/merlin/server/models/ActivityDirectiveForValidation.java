package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;

import java.sql.Timestamp;

public record ActivityDirectiveForValidation
(
    ActivityDirectiveId id,
    PlanId planId,
    Timestamp argumentsModifiedTime,
    SerializedActivity activity
) { }
