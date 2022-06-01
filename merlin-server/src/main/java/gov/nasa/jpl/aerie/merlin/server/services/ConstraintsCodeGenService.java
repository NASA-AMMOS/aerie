package gov.nasa.jpl.aerie.merlin.server.services;

public interface ConstraintsCodeGenService {
  String generateTypescriptTypesFromMissionModel(String missionModelId)
  throws MissionModelService.NoSuchMissionModelException;
}
