package gov.nasa.jpl.aerie.merlin.server.models;

import gov.nasa.jpl.aerie.merlin.driver.SerializedActivity;

public record ActivityDirectiveForValidation
(
    ActivityDirectiveId id,
    PlanId planId,
    SerializedActivity activity
) { }
