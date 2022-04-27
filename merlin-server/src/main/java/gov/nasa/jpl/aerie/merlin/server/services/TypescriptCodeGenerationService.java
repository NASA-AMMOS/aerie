package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.merlin.server.models.PlanId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class TypescriptCodeGenerationService {
  public record ActivityType(String name, Map<String, ValueSchema> parameters) {}
  public record ResourceType(String name, String type, ValueSchema schema) {}
  public record MissionModelTypes(Collection<ActivityType> activityTypes, Collection<ResourceType> resourceTypes) {}

  public TypescriptCodeGenerationService() {}

  public String generateTypescriptTypesForPlan(final PlanId planId) {
    return generateTypescriptTypesFromMissionModel();
  }

  public static String generateTypescriptTypesFromMissionModel() {
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
