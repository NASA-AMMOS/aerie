package gov.nasa.jpl.aerie.merlin.worker.postgres;

import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import java.util.Map;

public record PostgresValidationNotificationPayload(
    int activityDirectiveId,
    int revision,
    int planId,
    int modelId,
    String typeName,
    Map<String, SerializedValue> arguments
) {}
