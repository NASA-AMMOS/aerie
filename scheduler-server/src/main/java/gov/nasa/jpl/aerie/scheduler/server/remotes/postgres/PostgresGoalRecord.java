package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

public record PostgresGoalRecord(
    long id,
    long revision,
    String definition
) {}
