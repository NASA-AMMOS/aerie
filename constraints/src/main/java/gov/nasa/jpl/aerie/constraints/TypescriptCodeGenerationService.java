package gov.nasa.jpl.aerie.constraints;

import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TypescriptCodeGenerationService {

  private static final ValueSchema LINEAR_RESOURCE_SCHEMA = ValueSchema.ofStruct(Map.of(
      "initial", ValueSchema.REAL,
      "rate", ValueSchema.REAL));

  public record Parameter(String name, ValueSchema schema) {}
  public record ActivityType(List<Parameter> parameters) {}

  public static String generateTypescriptTypes(
      final Map<String, ActivityType> activityTypes,
      final Map<String, ValueSchema> resources
  ) {
    final var result = new ArrayList<String>();
    result.add("/** Start Codegen */");
    result.add("import * as AST from './constraints-ast.js';");
    result.add("import { Discrete, Real, Windows } from './constraints-edsl-fluent-api.js';");

    result.add("export enum ActivityType {");
    for (String activityType: activityTypes.keySet()) {
      result.add(indent(activityType + " = \"" + activityType + "\","));
    }
    result.add("}");

    result.add("export type Resource = {");
    for (var resource: resources.keySet()) {
      result.add(indent("\"" + resource + "\": " + valueSchemaToTypescript(resources.get(resource)) + ","));
    }
    result.add("};");

    result.add("export type ResourceName = " + String.join(
        " | ",
        resources.keySet().stream().map(key -> ("\"" + key + "\"")).toList()
    ) + ";");

    result.add("export type RealResourceName = " + String.join(
        " | ",
        resources.entrySet().stream().filter($ -> valueSchemaIsNumeric($.getValue())).map($ -> ("\"" + $.getKey() + "\"")).toList()
    ) + ";");

    // ActivityParameters

    result.add("export const ActivityTypeParameterMap = {");
    for (String activityType: activityTypes.keySet()) {
      result.add(indent("[ActivityType." + activityType + "]: (alias: string) => ({"));
      for (final var parameter: activityTypes.get(activityType).parameters()) {
        var parameterProfile = valueSchemaToTypescriptProfile(parameter.schema());
        final String nodeKind;
        if (parameterProfile.equals("Real")) {
          nodeKind = "RealProfileParameter";
        } else {
          nodeKind = "DiscreteProfileParameter";
        }
        result.add(indent(indent("\"" + parameter.name() + "\": new " + parameterProfile + "({")));
        result.add(indent(indent(indent("""
                                            kind: AST.NodeKind.%s,
                                            alias,
                                            name: "%s"
                                            """.formatted(nodeKind, parameter.name())))));
        result.add(indent(indent("}),")));
      }
      result.add(indent("}),"));
    }
    result.add("};");
    result.add("""
                   declare global {""");
    result.add(indent("enum ActivityType {"));
    for (String activityType: activityTypes.keySet()) {
      result.add(indent(indent(activityType + " = \"" + activityType + "\",")));
    }
    result.add(indent("}"));
    result.add("""
                   }
                   Object.assign(globalThis, {
                     ActivityType
                   });""");

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
        return "Temporal.Duration";
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

        for (final var member: values.keySet().stream().sorted().toList()) {
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

  private static boolean valueSchemaIsNumeric(final ValueSchema valueSchema) {
    return valueSchema.equals(ValueSchema.INT)
        || valueSchema.equals(ValueSchema.REAL)
        || valueSchema.equals(LINEAR_RESOURCE_SCHEMA);
  }
}
