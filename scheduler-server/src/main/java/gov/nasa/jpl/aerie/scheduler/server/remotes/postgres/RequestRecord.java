package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

public record RequestRecord(
    long specificationId,
    long analysisId,
    long specificationRevision,
    String status,
    String failureReason,
    boolean canceled
) {}
