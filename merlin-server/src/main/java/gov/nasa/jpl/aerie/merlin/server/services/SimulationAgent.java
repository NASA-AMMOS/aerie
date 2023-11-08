package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.ResultsProtocol;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

import java.util.function.Supplier;

public interface SimulationAgent {
  void simulate(PlanId planId, RevisionData revisionData, ResultsProtocol.WriterRole writer, Supplier<Boolean> canceledListener) throws InterruptedException;
}
