package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

public record ConstraintRecord(
    long id,
    String name,
    String description,
    String definition) {}
