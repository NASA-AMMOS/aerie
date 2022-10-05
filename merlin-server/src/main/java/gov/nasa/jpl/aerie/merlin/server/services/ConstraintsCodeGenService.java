package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

import java.util.Optional;

public interface ConstraintsCodeGenService {
  String generateTypescriptTypes(String missionModelId, Optional<PlanId> planId)
  throws MissionModelService.NoSuchMissionModelException, NoSuchPlanException;
}
