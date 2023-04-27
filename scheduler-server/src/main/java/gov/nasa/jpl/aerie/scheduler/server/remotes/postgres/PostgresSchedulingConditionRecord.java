package gov.nasa.jpl.aerie.scheduler.server.remotes.postgres;

public record PostgresSchedulingConditionRecord(
    long id, long revision, String name, String definition, boolean enabled) {}
