package gov.nasa.jpl.aerie.scheduler.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import gov.nasa.jpl.aerie.scheduler.server.exceptions.NoSuchMissionModelException;
import gov.nasa.jpl.aerie.scheduler.server.models.MissionModelId;
import gov.nasa.jpl.aerie.scheduler.server.models.PlanId;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TypescriptCodeGenerationService {
  public record ActivityType(String name, Map<String, ValueSchema> parameters) {}
  public record ResourceType(String name, String type, ValueSchema schema) {}
  public record MissionModelTypes(Collection<ActivityType> activityTypes, Collection<ResourceType> resourceTypes) {}

  private final MerlinService merlinService;

  public TypescriptCodeGenerationService(final MerlinService merlinService) {
    this.merlinService = merlinService;
  }

  public String generateTypescriptTypesForPlan(final PlanId planId) {
    try {
      return generateTypescriptTypesFromMissionModel(this.merlinService.getMissionModelTypes(planId));
    } catch (MerlinService.MerlinServiceException | IOException e) {
      throw new Error("Could not fetch mission model types", e);
    }
  }

  public String generateTypescriptTypesForMissionModel(final MissionModelId missionModelId) throws NoSuchMissionModelException {
    try {
      return generateTypescriptTypesFromMissionModel(this.merlinService.getMissionModelTypes(missionModelId));
    } catch (MerlinService.MerlinServiceException | IOException e) {
      throw new Error("Could not fetch mission model types", e);
    }
  }

  public static String generateTypescriptTypesFromMissionModel(final MissionModelTypes missionModelTypes) {
    final var activityTypeCodes = new ArrayList<ActivityTypeCode>();
    for (final var activityType : missionModelTypes.activityTypes()) {
      activityTypeCodes.add(getActivityTypeInformation(activityType));
    }
    final var result = new ArrayList<String>();
    result.add("/** Start Codegen */");
    result.add("import type { ActivityTemplate } from './scheduler-edsl-fluent-api.js';");
    result.add(generateActivityTypeEnum(activityTypeCodes));
    for (final var activityTypeCode : activityTypeCodes) {
      result.add("interface %s extends ActivityTemplate {}".formatted(activityTypeCode.activityTypeName()));
    }
    result.add(generateActivityTemplateConstructors(activityTypeCodes));
    result.add("declare global {");
    result.add(indent("var ActivityTemplates: typeof ActivityTemplateConstructors;"));
    result.add(indent("var ActivityTypes: typeof ActivityType;"));
    result.add("}");
    result.add("// Make ActivityTemplates and ActivityTypes available on the global object");
    result.add("Object.assign(globalThis, {");
    result.add(indent("ActivityTemplates: ActivityTemplateConstructors,"));
    result.add(indent("ActivityTypes: ActivityType,"));
    result.add("});");
    result.add("/** End Codegen */");
    return joinLines(result);
  }

  private static String generateActivityTypeEnum(ArrayList<ActivityTypeCode> activityTypeCodes) {
    final var result = new ArrayList<String>();
    result.add("export enum ActivityType {");
    for (final var activityTypeCode : activityTypeCodes) {
      result.add(indent("%s = '%s',".formatted(activityTypeCode.activityTypeName(), activityTypeCode.activityTypeName())));
    }
    result.add("}");
    return joinLines(result);
  }

  private static String generateActivityTemplateConstructors(final Iterable<ActivityTypeCode> activityTypeCodes) {
    final var result = new ArrayList<String>();
    result.add("const ActivityTemplateConstructors = {");
    for (final var activityTypeCode : activityTypeCodes) {
      result.add(indent("%s: function %sConstructor(args: {".formatted(activityTypeCode.activityTypeName(), activityTypeCode.activityTypeName())));
      result.add(indent(indent(generateActivityArgumentTypes(activityTypeCode.parameterTypes()))));
      result.add(indent("}): %s {".formatted(activityTypeCode.activityTypeName())));
      result.add(indent(indent("return { activityType: ActivityType.%s, args };".formatted(activityTypeCode.activityTypeName()))));
      result.add(indent("},"));
    }
    result.add("};");
    return joinLines(result);
  }

  private static String generateActivityArgumentTypes(final Iterable<TypescriptCodeGenerationService.ActivityParameter> parameterTypes) {
    final var result = new ArrayList<String>();
    for (final var parameterType : parameterTypes) {
      result.add("%s: %s,".formatted(parameterType.name(), ActivityParameterType.toString(parameterType.type())));
    }
    return joinLines(result);
  }

  private static String generateActivityTemplateTypeDeclarations(final Iterable<ActivityTypeCode> activityTypeCodes) {
    final var result = new ArrayList<String>();
    for (final var activityTypeCode : activityTypeCodes) {
      result.add(String.format("%s: (", activityTypeCode.activityTypeName()));
      result.add(indent("args: {"));
      for (final var parameterType : activityTypeCode.parameterTypes()) {
        result.add(indent(indent("%s: %s,".formatted(parameterType.name(), ActivityParameterType.toString(parameterType.type())))));
      }
      result.add(indent("}) => %s".formatted(activityTypeCode.activityTypeName())));
    }
    return joinLines(result);
  }

  private static String joinLines(final Iterable<String> result) {
    return String.join("\n", result);
  }

  private static String indent(final String s) {
    return joinLines(s.lines().map(line -> "  " + line).toList());
  }

  private record ActivityTypeCode(String activityTypeName, List<ActivityParameter> parameterTypes) {}
  private record ActivityParameter(String name, ActivityParameterType type) {}
  private sealed interface ActivityParameterType {
    record TSString() implements ActivityParameterType {}
    record TSDouble() implements ActivityParameterType {}
    record TSBoolean() implements ActivityParameterType {}
    record TSInt() implements ActivityParameterType {}
    record TSDuration() implements ActivityParameterType {}
    record TSArray(ActivityParameterType elementType) implements ActivityParameterType {}
    record TSStruct(List<Pair<String, ActivityParameterType>> keysAndTypes) implements ActivityParameterType {}
    record TSEnum(List<String> values) implements ActivityParameterType {}

    /**
     * Print this type out in one line.
     */
    static String toString(final ActivityParameterType type) {
      if (type instanceof TSString) {
        return "string";
      } else if (type instanceof TSDouble) {
        return "Double";
      } else if (type instanceof TSBoolean) {
        return "boolean";
      } else if (type instanceof TSInt) {
        return "Integer";
      } else if (type instanceof TSDuration) {
        return "Duration";
      } else if (type instanceof TSArray t) {
        return "%s[]".formatted(toString(t.elementType()));
      } else if (type instanceof TSStruct t) {
        return "{ " + t.keysAndTypes()
                .stream()
                .map($ -> "%s: %s, ".formatted($.getLeft(), toString($.getRight())))
                .reduce("", (a, b) -> a + b) + "}";
      } else if (type instanceof TSEnum t) {
        return "(" + String.join(" | ", t.values().stream().map(x -> "\"" + x + "\"").toList()) + ")";
      } else {
        throw new Error("Unhandled variant of ActivityParameterType: " + type);
      }
    }
  }

  private static ActivityTypeCode getActivityTypeInformation(final ActivityType activityType) {
    return new ActivityTypeCode(activityType.name(), generateActivityParameterTypes(activityType));
  }

  private static List<ActivityParameter> generateActivityParameterTypes(final ActivityType activityType) {
    return activityType
        .parameters()
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .map($ -> new ActivityParameter($.getKey(), valueSchemaToTypescriptType($.getValue())))
        .toList();
  }

  private static ActivityParameterType valueSchemaToTypescriptType(final ValueSchema valueSchema) {
    return valueSchema.match(new ValueSchema.Visitor<>() {
      @Override
      public ActivityParameterType onReal() {
        return new ActivityParameterType.TSDouble();
      }

      @Override
      public ActivityParameterType onInt() {
        return new ActivityParameterType.TSInt();
      }

      @Override
      public ActivityParameterType onBoolean() {
        return new ActivityParameterType.TSBoolean();
      }

      @Override
      public ActivityParameterType onString() {
        return new ActivityParameterType.TSString();
      }

      @Override
      public ActivityParameterType onDuration() {
        return new ActivityParameterType.TSDuration();
      }

      @Override
      public ActivityParameterType onPath() {
        return new ActivityParameterType.TSString();
      }

      @Override
      public ActivityParameterType onSeries(final ValueSchema value) {
        return new ActivityParameterType.TSArray(valueSchemaToTypescriptType(value));
      }

      @Override
      public ActivityParameterType onStruct(final Map<String, ValueSchema> value) {
        return new ActivityParameterType.TSStruct(
            value
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map($ ->
                         Pair.of(
                             $.getKey(),
                             valueSchemaToTypescriptType($.getValue())))
                .toList());
      }

      @Override
      public ActivityParameterType onVariant(final List<ValueSchema.Variant> variants) {
        return new ActivityParameterType.TSEnum(
            variants
                .stream()
                .map(ValueSchema.Variant::label)
                .toList());
      }
    });
  }
}
