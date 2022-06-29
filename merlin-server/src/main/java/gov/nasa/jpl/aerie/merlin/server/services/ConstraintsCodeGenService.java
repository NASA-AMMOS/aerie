package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.server.exceptions.NoSuchPlanException;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;
import gov.nasa.jpl.aerie.merlin.server.remotes.PlanRepository;

public interface ConstraintsCodeGenService {
  String generateTypescriptTypesFromPlan(PlanId planId, String missionModelId, PlanRepository planRepository)
  throws MissionModelService.NoSuchMissionModelException, NoSuchPlanException;

  String generateTypescriptTypesFromMissionModel(String missionModelId)
  throws MissionModelService.NoSuchMissionModelException;
}
