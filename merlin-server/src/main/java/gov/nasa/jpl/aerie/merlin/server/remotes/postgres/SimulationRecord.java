package gov.nasa.jpl.aerie.merlin.server.remotes.postgres;

public final record SimulationRecord(
    long id,
    long revision,
    long planId) {}
