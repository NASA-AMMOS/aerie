package gov.nasa.jpl.aerie.scheduler.worker.postgres;

public record PostgresSchedulingRequestNotificationPayload(
    long specificationRevision,
    long specificationId,
    long analysisId
) { }
