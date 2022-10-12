package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;

public record ActivityDirective
(
    ActivityDirectiveId id,
    PlanId planId,
    Timestamp argumentsModifiedTime,
    SerializedActivity activity
) { }
