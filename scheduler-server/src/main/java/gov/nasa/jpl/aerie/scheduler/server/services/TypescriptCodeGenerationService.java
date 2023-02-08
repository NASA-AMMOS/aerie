package gov.nasa.jpl.aerie.scheduler.server.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

public final class TypescriptCodeGenerationService {
  private TypescriptCodeGenerationService() { }

  public static String generateTypescriptTypesFromMissionModel(final MissionModelService.MissionModelTypes missionModelTypes) {
    final var activityTypeCodes = new ArrayList<ActivityTypeCode>();
    for (final var activityType : missionModelTypes.activityTypes()) {
      activityTypeCodes.add(getActivityTypeInformation(activityType));
    }
    final var result = new ArrayList<String>();
    result.add("/** Start Codegen */");
    result.add("import type { ActivityTemplate } from './scheduler-edsl-fluent-api.js';");
    result.add("import type { Windows } from './constraints-edsl-fluent-api.js';");
    result.add("import type * as ConstraintEDSL from './constraints-edsl-fluent-api.js'");

    for (final var activityTypeCode : activityTypeCodes) {
      result.add("interface %s extends ActivityTemplate<ActivityType.%s> {}".formatted(activityTypeCode.activityTypeName(), activityTypeCode.activityTypeName()));
    }
    result.add(getCastingMethod());
    result.add(generateActivityTemplateConstructors(activityTypeCodes));
    result.add(generateResourceTypes(missionModelTypes.resourceTypes()));
    result.add("declare global {");
    result.add(indent("var ActivityTemplates: typeof ActivityTemplateConstructors;"));
    result.add(indent("var Resources: typeof Resource;"));
    result.add("}");
    result.add("// Make ActivityTemplates and ActivityTypes available on the global object");
    result.add("Object.assign(globalThis, {");
    result.add(indent("ActivityTemplates: ActivityTemplateConstructors,"));
    result.add(indent("ActivityTypes: ActivityType,"));
    result.add(indent("Resources: Resource,"));
    result.add("});");
    result.add("/** End Codegen */");
    return joinLines(result);
  }

  private static String generateResourceTypes(final Collection<MissionModelService.ResourceType> resourceTypes) {
    final var result = new ArrayList<String>();
    result.add("export enum Resource {");
    for (final var resourceType : resourceTypes) {
      result.add(indent("\"%s\" = \"%s\",".formatted(resourceType.name(), resourceType.name())));
    }
    result.add("};");
    return joinLines(result);
  }

  private static String getCastingMethod(){
    return """
export function makeAllDiscreteProfile (argument: any) : any{
 if ((argument instanceof Discrete) || (argument instanceof Real)) {
   return argument.__astNode
 } else if ((argument instanceof Temporal.Duration) || (argument.kind === 'IntervalDuration')) {
   return argument;
 } else if(typeof(argument) === "number"){
     if(Number.isInteger(argument)){
       return Discrete.Value(argument).__astNode
     } else{
       return Real.Value(argument).__astNode
     }
   } else if(typeof(argument) === "string" || argument instanceof Temporal.Duration || typeof(argument) === "boolean"){
     return Discrete.Value(argument).__astNode
     }
  else if(Array.isArray(argument)){
   const arr: any[] = [];
   argument.every((element) => { arr.push(makeAllDiscreteProfile(element))});
   return Discrete.List(arr).__astNode;
 } else if (typeof argument === "object" ){
   const obj: { [k: string]: any } = {};
   for(var key in (argument)){
     // @ts-ignore
     obj[key] = makeAllDiscreteProfile((<object>argument)[key])
   }
   return Discrete.Map(obj).__astNode;
 } else{
  throw new Error('[makeAllDiscreteProfile] Type not covered: ' + argument);
 }
}

export function makeAll<T>(args : T):T{
// @ts-ignore
return (<T>makeAllDiscreteProfile(args))
}
""";
  }

  private static String generateActivityTemplateConstructors(final Iterable<ActivityTypeCode> activityTypeCodes) {
    final var result = new ArrayList<String>();
    result.add("const ActivityTemplateConstructors = {");
    for (final var activityTypeCode : activityTypeCodes) {
      if(activityTypeCode.parameterTypes().isEmpty()) {
        result.add(indent("%s: function %sConstructor(): %s {".formatted(
            activityTypeCode.activityTypeName(),
            activityTypeCode.activityTypeName(),
            activityTypeCode.activityTypeName())));
        result.add(indent(indent("return { activityType: ActivityType.%s, args: {} };".formatted(activityTypeCode.activityTypeName()))));
        result.add(indent("},"));
      }
      else {
        result.add(indent("%s: function %sConstructor(args:  ConstraintEDSL.Gen.ActivityTypeParameterMap[ActivityType.%s]".formatted(activityTypeCode.activityTypeName,activityTypeCode.activityTypeName,activityTypeCode.activityTypeName)));
        result.add(indent("): %s {".formatted(activityTypeCode.activityTypeName())));
        result.add(indent("// @ts-ignore"));
        result.add(indent(indent("return { activityType: ActivityType.%s, args: makeAll(args) };".formatted(activityTypeCode.activityTypeName()))));
        result.add(indent("},"));
      }
    }
    result.add("};");
    return joinLines(result);
  }

  private static String generateActivityArgumentTypes(final Iterable<TypescriptCodeGenerationService.ActivityParameter> parameterTypes) {
    final var result = new ArrayList<String>();
    for (final var parameterType : parameterTypes) {
      result.add("%s: %s,".formatted(parameterType.name(), TypescriptType.toString(parameterType.type())));
    }
    return joinLines(result);
  }

  private static String generateActivityTemplateTypeDeclarations(final Iterable<ActivityTypeCode> activityTypeCodes) {
    final var result = new ArrayList<String>();
    for (final var activityTypeCode : activityTypeCodes) {
      result.add(String.format("%s: (", activityTypeCode.activityTypeName()));
      result.add(indent("args: {"));
      for (final var parameterType : activityTypeCode.parameterTypes()) {
        result.add(indent(indent("%s: %s,".formatted(parameterType.name(), TypescriptType.toString(parameterType.type())))));
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

  private record ActivityParameter(String name, TypescriptType type) {}

  private sealed interface TypescriptType {
    record TSString() implements TypescriptType {}
    record TSDouble() implements TypescriptType {}
    record TSBoolean() implements TypescriptType {}
    record TSInt() implements TypescriptType {}
    record TSDuration() implements TypescriptType {}
    record TSArray(TypescriptType elementType) implements TypescriptType {}
    record TSStruct(List<Pair<String, TypescriptType>> keysAndTypes) implements TypescriptType {}
    record TSEnum(List<String> values) implements TypescriptType {}

    /**
     * Print this type out in one line.
     */
    static String toString(final TypescriptType type) {
      if (type instanceof TSString) {
        return "(string | Discrete<string>)";
      } else if (type instanceof TSDouble) {
        //return "(Double | Real)";
        return "(number | Real)";
      } else if (type instanceof TSBoolean) {
        return "(boolean | Discrete<boolean>)";
      } else if (type instanceof TSInt) {
        return "(number | Real)";
      } else if (type instanceof TSDuration) {
        return "(Temporal.Duration | Discrete<Temporal.Duration>)";
      } else if (type instanceof TSArray t) {
        final var typeStr = toString(t.elementType());
        return "((" +typeStr+ ")[] | Discrete<("+typeStr+")[]>)";
      } else if (type instanceof TSStruct t) {

        var typeStr = "{ ";
        for(final var keyAndType :  t.keysAndTypes()){
          typeStr += keyAndType.getLeft() + ":"+ toString(keyAndType.getRight())+",";
        }
        typeStr+="}";
        return "(%s | Discrete<%s>)".formatted(typeStr, typeStr);
      } else if (type instanceof TSEnum t) {
        final var typeStr = "(" + String.join(" | ", t.values().stream().map(x -> "\"" + x + "\"").toList()) + ")";
        return "(%s | Discrete<%s>)".formatted(typeStr, typeStr);
      } else {
        throw new Error("Unhandled variant of TypescriptType: " + type);
      }
    }
  }

  private static ActivityTypeCode getActivityTypeInformation(final MissionModelService.ActivityType activityType) {
    return new ActivityTypeCode(activityType.name(), generateActivityParameterTypes(activityType));
  }

  private static List<ActivityParameter> generateActivityParameterTypes(final MissionModelService.ActivityType activityType) {
    return activityType
        .parameters()
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .map($ -> new ActivityParameter($.getKey(), valueSchemaToTypescriptType($.getValue())))
        .toList();
  }

  private static TypescriptType valueSchemaToTypescriptType(final ValueSchema valueSchema) {
    return valueSchema.match(new ValueSchema.Visitor<>() {
      @Override
      public TypescriptType onReal() {
        return new TypescriptType.TSDouble();
      }

      @Override
      public TypescriptType onInt() {
        return new TypescriptType.TSInt();
      }

      @Override
      public TypescriptType onBoolean() {
        return new TypescriptType.TSBoolean();
      }

      @Override
      public TypescriptType onString() {
        return new TypescriptType.TSString();
      }

      @Override
      public TypescriptType onDuration() {
        return new TypescriptType.TSDuration();
      }

      @Override
      public TypescriptType onPath() {
        return new TypescriptType.TSString();
      }

      @Override
      public TypescriptType onSeries(final ValueSchema value) {
        return new TypescriptType.TSArray(valueSchemaToTypescriptType(value));
      }

      @Override
      public TypescriptType onStruct(final Map<String, ValueSchema> value) {
        return new TypescriptType.TSStruct(
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
      public TypescriptType onVariant(final List<ValueSchema.Variant> variants) {
        return new TypescriptType.TSEnum(
            variants
                .stream()
                .map(ValueSchema.Variant::label)
                .toList());
      }
    });
  }
}
