package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

public record ConstraintRecord(
    long id,
    long revision,
    String name,
    String description,
    String definition) {}
