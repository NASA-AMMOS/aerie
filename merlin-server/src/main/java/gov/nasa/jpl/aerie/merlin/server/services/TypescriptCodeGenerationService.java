package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.ArrayList;

public final class TypescriptCodeGenerationService {
  private final MissionModelService missionModelService;

  public TypescriptCodeGenerationService(final MissionModelService missionModelService) {
    this.missionModelService = missionModelService;
  }

  public String generateTypescriptTypesFromMissionModel(final String missionModelId)
  throws MissionModelService.NoSuchMissionModelException
  {
    final var activityTypes = this.missionModelService.getActivityTypes(missionModelId);



    final var result = new ArrayList<String>();
    result.add("/** Start Codegen */");
    result.add("/** End Codegen */");
    return joinLines(result);
  }

  private static String joinLines(final Iterable<String> result) {
    return String.join("\n", result);
  }

  private static String indent(final String s) {
    return joinLines(s.lines().map(line -> "  " + line).toList());
  }
}
