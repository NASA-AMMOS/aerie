package gov.nasa.jpl.aerie.merlin.server.services;

public class TypescriptCodeGenerationServiceAdapter implements ConstraintsCodeGenService {
  private final MissionModelService missionModelService;

  public TypescriptCodeGenerationServiceAdapter(final MissionModelService missionModelService) {
    this.missionModelService = missionModelService;
  }

  @Override
  public String generateTypescriptTypesFromMissionModel(final String missionModelId)
  throws MissionModelService.NoSuchMissionModelException
  {
    return new TypescriptCodeGenerationService(missionModelService).generateTypescriptTypesFromMissionModel(missionModelId);
  }
}

