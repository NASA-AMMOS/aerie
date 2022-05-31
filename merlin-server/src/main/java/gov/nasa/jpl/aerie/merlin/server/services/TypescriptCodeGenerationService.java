package gov.nasa.jpl.aerie.merlin.server.services;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TypescriptCodeGenerationService {
  private final MissionModelService missionModelService;

  public TypescriptCodeGenerationService(final MissionModelService missionModelService) {
    this.missionModelService = missionModelService;
  }

  public String generateTypescriptTypesFromMissionModel(final String missionModelId)
  throws MissionModelService.NoSuchMissionModelException
  {
    final var activityTypes = this.missionModelService.getActivityTypes(missionModelId);
    final var resources = this.missionModelService.getStatesSchemas(missionModelId);

    final var result = new ArrayList<String>();
    result.add("/** Start Codegen */");

    result.add("export type ActivityTypeName =");
    for (String activityType: activityTypes.keySet()) {
      result.add(indent("| \"" + activityType + "\""));
    }
    result.add(indent(";"));

    result.add("export type ResourceName = " + String.join(" | ", resources.keySet().stream().map($ -> "\"" + $ + "\"").toList()) + ";");
    result.add("export type DiscreteResourceSchema<R extends ResourceName> =");
    for (var resource: resources.keySet()) {
      result.add(indent("R extends \"" + resource + "\" ? " + valueSchemaToTypescript(resources.get(resource)) + " :"));
    }
    result.add(indent("void;"));

    result.add("export function discreteResourceSchemaDummyValue<R extends ResourceName>(resource: R): DiscreteResourceSchema<R> {");
    result.add(indent("return ("));
    for (var resource: resources.keySet()) {
      result.add(indent(indent("resource === \"" + resource + "\" ? " + valueSchemaToTypescriptDefault(resources.get(resource)) + " :")));
    }
    result.add(indent(indent("undefined")));
    result.add(indent(") as DiscreteResourceSchema<R>;"));
    result.add("}");

    result.add("export type RealResourceName = " + String.join(
        " | ",
        resources.entrySet().stream().filter($ -> valueSchemaIsReal($.getValue())).map($ -> ("\"" + $.getKey() + "\"")).toList()
    ) + ";");

    result.add("/** End Codegen */");
    return joinLines(result);
  }

  private static String joinLines(final Iterable<String> result) {
    return String.join("\n", result);
  }

  private static String indent(final String s) {
    return joinLines(s.lines().map(line -> "  " + line).toList());
  }


  private static String valueSchemaToTypescript(final ValueSchema valueSchema) {
    return valueSchema.match(new ValueSchema.Visitor<>() {
      @Override
      public String onReal() {
        return "number";
      }

      @Override
      public String onInt() {
        return "number";
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
        return "Duration";
      }

      @Override
      public String onPath() {
        return "string";
      }

      @Override
      public String onSeries(final ValueSchema value) {
        return valueSchemaToTypescript(value) + "[]";
      }

      @Override
      public String onStruct(final Map<String, ValueSchema> values) {
        final var result = new StringBuilder("{");

        for (final var member: values.keySet()) {
          result
              .append(member)
              .append(": ")
              .append(valueSchemaToTypescript(values.get(member)))
              .append(", ");
        }

        result.append("}");
        return result.toString();
      }

      @Override
      public String onVariant(final List<ValueSchema.Variant> variants) {
        final var result = new StringBuilder("(");

        for (final var variant: variants) {
          result
              .append(" | \"")
              .append(variant.label())
              .append("\"");
        }

        result.append(")");
        return result.toString();
      }
    });
  }

  private static String valueSchemaToTypescriptProfile(final ValueSchema valueSchema) {
    var basicTypescript = valueSchemaToTypescript(valueSchema);
    if (basicTypescript.equals("number")) {
      return "Real";
    } else {
      return "Discrete<" + basicTypescript + ">";
    }
  }

  private static boolean valueSchemaIsReal(final ValueSchema valueSchema) {
    return valueSchema.match(new ValueSchema.DefaultVisitor<>() {
      @Override
      protected Boolean onDefault() {
        return false;
      }

      @Override
      public Boolean onReal() {
        return true;
      }
    });
  }
}
