
package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.Map;
import java.util.stream.Collectors;

public class TypescriptCodeGenerationServiceAdapter implements ConstraintsCodeGenService {
  private final MissionModelService missionModelService;

  public TypescriptCodeGenerationServiceAdapter(final MissionModelService missionModelService) {
    this.missionModelService = missionModelService;
  }

  @Override
  public String generateTypescriptTypesFromMissionModel(final String missionModelId)
  throws MissionModelService.NoSuchMissionModelException
  {
    return TypescriptCodeGenerationService
        .generateTypescriptTypes(
            activityTypes(missionModelService, missionModelId),
            resourceTypes(missionModelService, missionModelId));
  }

  static Map<String, TypescriptCodeGenerationService.ActivityType> activityTypes(final MissionModelService missionModelService, final String modelId)
  throws MissionModelService.NoSuchMissionModelException
  {
    return missionModelService
        .getActivityTypes(modelId)
        .entrySet()
        .stream()
        .map(entry -> Map.entry(
            entry.getKey(),
            new TypescriptCodeGenerationService.ActivityType(
                entry
                    .getValue()
                    .parameters()
                    .stream()
                    .map(p -> new TypescriptCodeGenerationService.Parameter(p.name(), p.schema()))
                    .toList())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
  static Map<String, ValueSchema> resourceTypes(final MissionModelService missionModelService, final String modelId)
  throws MissionModelService.NoSuchMissionModelException
  {
    return missionModelService.getStatesSchemas(modelId);
  }
}

