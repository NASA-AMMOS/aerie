package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TypescriptCodeGenerationService {
  public record ActivityType(String name, Map<String, ValueSchema> parameters) {}
  public record ResourceType(String name, String type, ValueSchema schema) {}
  public record MissionModelTypes(Collection<ActivityType> activityTypes, Collection<ResourceType> resourceTypes) {}

  private final MerlinService merlinService;

  public TypescriptCodeGenerationService(MerlinService merlinService) {
    this.merlinService = merlinService;
  }

  public static String generateTypescriptTypesFromMissionModel(final MissionModelTypes missionModelTypes) {
    final var activityTypeCodes = new ArrayList<ActivityTypeCode>();
    for (final var activityType : missionModelTypes.activityTypes()) {
      activityTypeCodes.add(getActivityTypeInformation(activityType));
    }
    var result = "/** Start Codegen */\n";
    for (final var activityTypeCode : activityTypeCodes) {
      result += activityTypeCode.declaration() + "\n";
    }
    result += "export const ActivityTemplates = {\n";
    for (final var activityTypeCode : activityTypeCodes) {
      result += activityTypeCode.implementation();
    }
    result += "}\n/** End Codegen */";
    return result;
  }

  private record ActivityTypeCode(String declaration, String implementation) {}

  private static ActivityTypeCode getActivityTypeInformation(final ActivityType activityType) {
    return new ActivityTypeCode(
        String.format("interface %s extends ActivityTemplate {}\n", activityType.name()),
        String.format("""
          %s: function %s(
            name: string,
            args: {
            %s
            }): %s {
            return {
              name,
              activityType: '%s',
              args: args,
            };
          },
        """, activityType.name(), activityType.name(), generateActivityParameterTypes(activityType).indent(2), activityType.name(), activityType.name())
    );
  }

  @NotNull
  private static String generateActivityParameterTypes(final ActivityType activityType) {
    final var result = new ArrayList<>();
    for (final var param : activityType.parameters().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
      final var name = param.getKey();
      final var valueSchema = param.getValue();
      result.add(String.format("%s: %s,", name, valueSchemaToTypescriptType(valueSchema)));
    }
    return String.join("\n", result.toArray(new String[0]));
  }

  private static String valueSchemaToTypescriptType(final ValueSchema valueSchema) {
    return valueSchema.match(new ValueSchema.Visitor<String>() {
      @Override
      public String onReal() {
        return "AST.Double";
      }

      @Override
      public String onInt() {
        return "AST.Integer";
      }

      @Override
      public String onBoolean() {
        return "boolean";
      }

      @Override
      public String onString() {
        return "string";
      }

      @Override
      public String onDuration() {
        return "AST.Duration";
      }

      @Override
      public String onPath() {
        return "string";
      }

      @Override
      public String onSeries(final ValueSchema value) {
        return String.format("%s[]", valueSchemaToTypescriptType(value));
      }

      @Override
      public String onStruct(final Map<String, ValueSchema> value) {
        var result = "{\n";
        final var entries = new ArrayList<>(value.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        for (final var entry : entries) {
          result += String.format("%s: %s,\n", entry.getKey(), valueSchemaToTypescriptType(entry.getValue())).indent(2);
        }
        result += "}";
        return result;
      }

      @Override
      public String onVariant(final List<ValueSchema.Variant> variants) {
        return "(" + String.join(
            " | ",
            variants
                .stream()
                .map(variant -> "\"" + variant.label() + "\"")
                .toList()) + ")";
      }
    });
  }
}
