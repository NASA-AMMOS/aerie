package gov.nasa.jpl.aerie.merlin.processor.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.RecordValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.ActivityMapper;
import gov.nasa.jpl.aerie.merlin.framework.ModelActions;
import gov.nasa.jpl.aerie.merlin.framework.RootModel;
import gov.nasa.jpl.aerie.merlin.framework.Scoped;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.processor.MissionModelProcessor;
import gov.nasa.jpl.aerie.merlin.processor.Resolver;
import gov.nasa.jpl.aerie.merlin.processor.TypePattern;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ConfigurationTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.EffectModelRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ExportTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.MissionModelRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.TypeRule;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Topic;
import gov.nasa.jpl.aerie.merlin.protocol.model.MerlinPlugin;
import gov.nasa.jpl.aerie.merlin.protocol.model.MissionModelFactory;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerModel;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerPlugin;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.TaskStatus;
import gov.nasa.jpl.aerie.merlin.protocol.types.Unit;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Auto-generates Java source files from mission model metamodels. */
public record MissionModelGenerator(Elements elementUtils, Types typeUtils, Messager messager) {

  private static final String COMPUTED_ATTRIBUTES_VALUE_MAPPER_FIELD_NAME = "computedAttributesValueMapper";

  /** Generate `GeneratedMerlinPlugin` class. */
  public JavaFile generateMerlinPlugin(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getMerlinPluginName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(MerlinPlugin.class)
            .addMethod(
                MethodSpec
                    .methodBuilder("getFactory")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(missionModel.getFactoryName())
                    .addStatement(
                        "return new $T()",
                        missionModel.getFactoryName())
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  /** Generate `GeneratedSchedulerPlugin` class. */
  public JavaFile generateSchedulerPlugin(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getSchedulerPluginName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(SchedulerPlugin.class)
            .addMethod(
                MethodSpec
                    .methodBuilder("getSchedulerModel")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(missionModel.getSchedulerModelName())
                    .addStatement("return new $T()",
                                  missionModel.getSchedulerModelName())
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  /** Generate `GeneratedMissionModelFactory` class. */
  public Optional<JavaFile> generateMissionModelConfigurationMapper(final MissionModelRecord missionModel, final ConfigurationTypeRecord configType) {
    return generateCommonMapperMethods(missionModel, configType).map(typeSpec -> JavaFile
        .builder(configType.mapper().name.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build());
  }

  /** Generate `GeneratedMissionModelFactory` class. */
  public JavaFile generateMissionModelFactory(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getFactoryName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(MissionModelFactory.class),
                    missionModel.modelConfigurationType
                        .map($ -> ClassName.get($.declaration()))
                        .orElse(ClassName.get(Unit.class)),
                    ParameterizedTypeName.get(
                        ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                        ClassName.get(missionModel.topLevelModel))))
            .addField(
                FieldSpec
                    .builder(
                        ParameterizedTypeName.get(
                            ClassName.get(Scoped.class),
                            ParameterizedTypeName.get(
                                ClassName.get(RootModel.class),
                                ClassName.get(missionModel.topLevelModel))),
                        "model",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$T.create()", Scoped.class)
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("getDirectiveTypes")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get(Map.class),
                            ClassName.get(String.class),
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.framework.ActivityMapper.class),
                                ParameterizedTypeName.get(
                                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                                    ClassName.get(missionModel.topLevelModel)),
                                WildcardTypeName.subtypeOf(Object.class),
                                WildcardTypeName.subtypeOf(Object.class))))
                    .addStatement(
                        "return $T.$L",
                        missionModel.getTypesName(),
                        "directiveTypes")
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("getConfigurationType")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(
                        missionModel.modelConfigurationType
                            .map(configType -> configType.mapper().name)
                            .orElse(ClassName.get(gov.nasa.jpl.aerie.merlin.framework.EmptyConfigurationType.class)))
                    .addStatement(
                        "return new $T()",
                        missionModel.modelConfigurationType
                            .map(configType -> configType.mapper().name)
                            .orElse(ClassName.get(gov.nasa.jpl.aerie.merlin.framework.EmptyConfigurationType.class)))
                .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("instantiate")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(
                        ClassName.get(Instant.class),
                       "planStart",
                        Modifier.FINAL)
                    .addParameter(
                        missionModel.modelConfigurationType
                            .map($ -> ClassName.get($.declaration()))
                            .orElse(ClassName.get(Unit.class)),
                        "configuration",
                        Modifier.FINAL)
                    .addParameter(
                        ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer.class),
                        "builder",
                        Modifier.FINAL)
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                            ClassName.get(missionModel.topLevelModel)))
                    .addStatement(
                        "$T.registerTopics($L)",
                        missionModel.getTypesName(),
                        "builder")
                    .addCode("\n")
                    .addStatement(
                        "final var $L = new $T($L)",
                        "registrar",
                        gov.nasa.jpl.aerie.merlin.framework.Registrar.class,
                        "builder")
                    .addStatement(
                        "final var $L = $T.makeExecutorService()",
                        "executor",
                        gov.nasa.jpl.aerie.merlin.framework.RootModel.class)
                    .addCode("\n")
                    .addStatement(
                        "final var $L = $T.initializing($L, $L, () -> $L)",
                        "model",
                        gov.nasa.jpl.aerie.merlin.framework.InitializationContext.class,
                        "executor",
                        "builder",
                        generateMissionModelInstantiation(missionModel))
                    .addStatement(
                        "return new $T<>($L, $L)",
                        gov.nasa.jpl.aerie.merlin.framework.RootModel.class,
                        "model",
                        "executor")
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  private static CodeBlock generateMissionModelInstantiation(final MissionModelRecord missionModel) {
    final var modelClassName = ClassName.get(missionModel.topLevelModel);
    return missionModel.modelConfigurationType
        .map(configType -> missionModel.expectsPlanStart ?
            CodeBlock.of(
                "new $T($L, $L, $L)",
                modelClassName,
                "registrar",
                "planStart",
                "configuration") :
            CodeBlock.of(
                "new $T($L, $L)",
                modelClassName,
                "registrar",
                "configuration"))
        .orElseGet(() -> missionModel.expectsPlanStart ?
            CodeBlock.of(
                "new $T($L, $L)",
                modelClassName,
                "registrar",
                "planStart") :
            CodeBlock.of(
                "new $T($L)",
                modelClassName,
                "registrar"));
  }

  /** Generate `GeneratedSchedulerModel` class. */
  public JavaFile generateSchedulerModel(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getSchedulerModelName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(SchedulerModel.class)
            .addMethod(
                MethodSpec
                    .methodBuilder("getDurationTypes")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(Map.class, String.class, DurationType.class))
                    .addStatement("final var result = new $T()", ParameterizedTypeName.get(HashMap.class, String.class, DurationType.class))
                    .addCode(
                        missionModel
                            .activityTypes
                            .stream()
                            .map(
                                activityTypeRecord ->
                                    CodeBlock
                                        .builder()
                                        .addStatement("result.put(\"$L\", $T.$L)",
                                                      activityTypeRecord.name(),
                                                      DurationType.class,
                                                      activityTypeRecord
                                                          .effectModel()
                                                          .flatMap(EffectModelRecord::durationParameter)
                                                          .map(durationParameter -> CodeBlock.of("controllable(\"$L\")", durationParameter))
                                                          .orElse(CodeBlock.of("uncontrollable()"))))
                            .reduce((x, y) -> x.add("$L", y.build()))
                            .orElse(CodeBlock.builder()).build())
                    .addStatement("return result")
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  /** Generate `ActivityActions` class. */
  public JavaFile generateActivityActions(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getActivityActionsName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            // The location of the mission model package determines where to put this class.
            .addOriginatingElement(missionModel.$package)
            // TODO: List found task spec types as dependencies of this generated file.
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC)
            .superclass(gov.nasa.jpl.aerie.merlin.framework.ModelActions.class)
            .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
            .addMethods(
                missionModel.activityTypes
                    .stream()
                    .flatMap(entry -> Stream
                        .of(
                            MethodSpec
                                .methodBuilder("spawn")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(String.class)
                                .addParameter(
                                    ClassName.get(entry.declaration()),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "final var $L = $T.$L.get()",
                                    "model",
                                    missionModel.getFactoryName(),
                                    "model")
                                .addStatement(
                                    "final var $L = $T.$L",
                                    "mapper",
                                    missionModel.getTypesName(),
                                    entry.mapper().name.canonicalName().replace(".", "_"))
                                .addStatement(
                                    "return $T.spawn($L.createTask($L, $L))",
                                    gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                    "mapper",
                                    "model",
                                    "activity")
                                .build(),
                            MethodSpec
                                .methodBuilder("defer")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(String.class)
                                .addParameter(
                                    ParameterSpec
                                        .builder(
                                            gov.nasa.jpl.aerie.merlin.protocol.types.Duration.class,
                                            "duration")
                                        .addModifiers(Modifier.FINAL)
                                        .build())
                                .addParameter(
                                    ClassName.get(entry.declaration()),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "final var $L = $T.$L.get()",
                                    "model",
                                    missionModel.getFactoryName(),
                                    "model")
                                .addStatement(
                                    "final var $L = $T.$L",
                                    "mapper",
                                    missionModel.getTypesName(),
                                    entry.mapper().name.canonicalName().replace(".", "_"))
                                .addStatement(
                                    "return $T.defer($L, $L.createTask($L, $L))",
                                    gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                    "duration",
                                    "mapper",
                                    "model",
                                    "activity")
                                .build(),
                            MethodSpec
                                .methodBuilder("defer")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(String.class)
                                .addParameter(
                                    ParameterSpec
                                        .builder(
                                            TypeName.LONG,
                                            "quantity")
                                        .addModifiers(Modifier.FINAL)
                                        .build())
                                .addParameter(
                                    ParameterSpec
                                        .builder(
                                            gov.nasa.jpl.aerie.merlin.protocol.types.Duration.class,
                                            "unit")
                                        .addModifiers(Modifier.FINAL)
                                        .build())
                                .addParameter(
                                    ClassName.get(entry.declaration()),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "return defer($L.times($L), $L)",
                                    "unit",
                                    "quantity",
                                    "activity")
                                .build(),
                            MethodSpec
                                .methodBuilder("call")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(TypeName.VOID)
                                .addParameter(
                                    ClassName.get(entry.declaration()),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "$T.waitFor(spawn($L))",
                                    gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                    "activity")
                                .build()))
                    .collect(Collectors.toList()))
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  /** Generate `ActivityTypes` class. */
  public JavaFile generateActivityTypes(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getTypesName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addFields(
                missionModel.activityTypes
                    .stream()
                    .map(activityType -> FieldSpec
                        .builder(
                            activityType.mapper().name,
                            activityType.mapper().name.canonicalName().replace(".", "_"),
                            Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("new $T()", activityType.mapper().name)
                        .build())
                    .toList())
            .addField(
                FieldSpec
                    .builder(
                        ParameterizedTypeName.get(
                            ClassName.get(Map.class),
                            ClassName.get(String.class),
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.framework.ActivityMapper.class),
                                ParameterizedTypeName.get(
                                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                                    ClassName.get(missionModel.topLevelModel)),
                                WildcardTypeName.subtypeOf(Object.class),
                                WildcardTypeName.subtypeOf(Object.class))),
                        "directiveTypes",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer(
                        "$T.ofEntries($>$>$L$<$<)",
                        ClassName.get(Map.class),
                        missionModel.activityTypes
                            .stream()
                            .map(activityType -> CodeBlock
                                .builder()
                                .add(
                                    "\n$T.entry($S, $L)",
                                    ClassName.get(Map.class),
                                    activityType.name(),
                                    activityType.mapper().name.canonicalName().replace(".", "_")))
                            .reduce((x, y) -> x.add(",").add(y.build()))
                            .orElse(CodeBlock.builder())
                            .build())
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("register")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addAnnotation(
                        AnnotationSpec
                            .builder(Deprecated.class)
                            .addMember("forRemoval", CodeBlock.of("true"))
                            .build())
                    .addJavadoc(
                        "@deprecated This method has no effect, and simply returns $T.model.",
                        missionModel.getFactoryName())
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get(Scoped.class),
                            ParameterizedTypeName.get(
                                ClassName.get(RootModel.class),
                                ClassName.get(missionModel.topLevelModel))))
                    .addStatement("return $T.model", missionModel.getFactoryName())
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("registerTopics")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(TypeName.get(Initializer.class), "initializer", Modifier.FINAL)
                    .addStatement(
                        "$L.forEach((name, mapper) -> registerDirectiveType($L, name, mapper))",
                        "directiveTypes",
                        "initializer")
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("registerDirectiveType")
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .addTypeVariable(TypeVariableName.get("Input"))
                    .addTypeVariable(TypeVariableName.get("Output"))
                    .addParameter(ClassName.get(Initializer.class), "initializer", Modifier.FINAL)
                    .addParameter(ClassName.get(String.class), "name", Modifier.FINAL)
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get(ActivityMapper.class),
                            ParameterizedTypeName.get(
                                ClassName.get(RootModel.class),
                                ClassName.get(missionModel.topLevelModel)),
                            TypeVariableName.get("Input"),
                            TypeVariableName.get("Output")),
                        "mapper",
                        Modifier.FINAL)
                    .addStatement(
                        "$L.topic(\n$L,\n$L,\n$L,\n$L)",
                        "initializer",
                        CodeBlock.of("$S + $L", "ActivityType.Input.", "name"),
                        CodeBlock.of("$L.getInputTopic()", "mapper"),
                        CodeBlock.of(
                            "$T.ofStruct($L.getParameters().stream().collect($T.toMap($$ -> $$.name(), $$ -> $$.schema())))",
                            ValueSchema.class,
                            "mapper",
                            Collectors.class),
                        CodeBlock.of("$$ -> $T.of($L.getArguments($$))", SerializedValue.class, "mapper"))
                    .addCode("\n")
                    .addStatement(
                        "$L.topic(\n$L,\n$L,\n$L,\n$L)",
                        "initializer",
                        CodeBlock.of("$S + $L", "ActivityType.Output.", "name"),
                        CodeBlock.of("$L.getOutputTopic()", "mapper"),
                        CodeBlock.of("$L.getReturnValueSchema()", "mapper"),
                        CodeBlock.of("$L::serializeReturnValue", "mapper"))
                    .build()
            )
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  public Pair<JavaFile, List<TypeRule>> generateAutoValueMappers(
      final MissionModelRecord missionModel,
      final Iterable<TypeElement> recordTypes) {
    final var typeRules = new ArrayList<TypeRule>();

    final var typeName = missionModel.getAutoValueMappersName();

    final var builder =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    record ComponentMapperNamePair(String componentName, String mapperName) {}
    for (final var record : recordTypes) {
      final var methodName = ClassName.get(record).canonicalName().replace(".", "_");
      final var componentToMapperName = new ArrayList<ComponentMapperNamePair>();
      final var necessaryMappers = new HashMap<TypeMirror, String>();
      for (final var element : record.getEnclosedElements()) {
        if (!(element instanceof RecordComponentElement el)) continue;
        final var typeMirror = el.getAccessor().getReturnType();
        final var elementName = element.toString();
        final var valueMapperIdentifier = typeMirror.toString().replace(".", "_") + "ValueMapper";
        componentToMapperName.add(new ComponentMapperNamePair(elementName, valueMapperIdentifier));
        necessaryMappers.put(typeMirror, valueMapperIdentifier);
      }

      final var typeRule = new TypeRule(
          new TypePattern.ClassPattern(ClassName.get(ValueMapper.class), List.of(TypePattern.from(record.asType()))),
          Set.of(),
          necessaryMappers
              .keySet()
              .stream()
              .map(component -> (TypePattern) new TypePattern.ClassPattern(
                  ClassName.get(ValueMapper.class),
                  List.of(new TypePattern.ClassPattern((ClassName) ClassName.get(component).box(), List.of()))))
              .toList(),
          typeName,
          methodName);
      typeRules.add(typeRule);

      final var methodBuilder = MethodSpec
          .methodBuilder(methodName)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .returns(ParameterizedTypeName.get(ClassName.get(ValueMapper.class), TypeName.get(record.asType())))
          .addParameters(
              necessaryMappers
                  .entrySet()
                  .stream()
                  .map(mapperRequest -> ParameterSpec
                      .builder(
                          ParameterizedTypeName.get(
                              ClassName.get(ValueMapper.class),
                              ClassName.get(mapperRequest.getKey()).box()),
                          mapperRequest.getValue(),
                          Modifier.FINAL)
                      .build())
                  .toList());

      final var codeBlockBuilder = CodeBlock.builder();
      codeBlockBuilder.add("return new $T<>(\n", RecordValueMapper.class);
      codeBlockBuilder.add("  $T.class,\n", ClassName.get(record));
      codeBlockBuilder.add("  $T.of(\n", List.class);

      codeBlockBuilder.add(
          CodeBlock.join(componentToMapperName
            .stream()
            .map(recordComponent ->
                     CodeBlock
                         .builder()
                         .add("    new $T<>(\n", RecordValueMapper.Component.class)
                         .add("      $S,\n", recordComponent.componentName())
                         .add("      $T::$L,\n", ClassName.get(record), recordComponent.componentName())
                         .add("    $L)", recordComponent.mapperName())
                        .build())
            .toList(), ",\n"));
      codeBlockBuilder.add("));");

      methodBuilder.addCode(codeBlockBuilder.build());
      builder.addMethod(methodBuilder.build());
    }

    return Pair.of(JavaFile
        .builder(typeName.packageName(), builder.build())
        .skipJavaLangImports(true)
        .build(), typeRules);
  }

  private record ComputedAttributesCodeBlocks(TypeName typeName, FieldSpec fieldDef, CodeBlock fieldInit) {}

  /** Generate common `${activity_name}Mapper` methods. */
  public Optional<TypeSpec> generateCommonMapperMethods(final MissionModelRecord missionModel, final ExportTypeRecord exportType) {
    final var maybeMapperBlocks = generateParameterMapperBlocks(missionModel, exportType);
    if (maybeMapperBlocks.isEmpty()) return Optional.empty();

    final var mapperBlocks = maybeMapperBlocks.get();
    final var mapperMethodMaker = MapperMethodMaker.make(exportType);

    // TODO currently only 2 permitted classes (activity and config. type records),
    //  this should be changed to a switch expression once sealed class pattern-matching switch expressions exist
    final TypeName superInterface;
    final Optional<ComputedAttributesCodeBlocks> computedAttributesCodeBlocks;
    if (exportType instanceof ActivityTypeRecord activityType) {
      computedAttributesCodeBlocks = this.getComputedAttributesCodeBlocks(
          missionModel,
          activityType);
      if (computedAttributesCodeBlocks.isEmpty()) {
        return Optional.empty();
      }
      superInterface = ParameterizedTypeName.get(
          ClassName.get(gov.nasa.jpl.aerie.merlin.framework.ActivityMapper.class),
          ParameterizedTypeName.get(
              ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
              ClassName.get(missionModel.topLevelModel)),
          ClassName.get(exportType.declaration()),
          computedAttributesCodeBlocks.get().typeName().box());
    } else { // is instanceof ConfigurationTypeRecord
      computedAttributesCodeBlocks = Optional.empty();
      superInterface = ParameterizedTypeName.get(
          ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.ConfigurationType.class),
          ClassName.get(exportType.declaration()));
    }

    return Optional.of(TypeSpec
        .classBuilder(exportType.mapper().name)
        // The location of the missionModel package determines where to put this class.
        .addOriginatingElement(missionModel.$package)
        // The fields and methods of the activity determines the overall behavior of this class.
        .addOriginatingElement(exportType.declaration())
        // TODO: Add an originating element for each of the mapper rulesets associated with the mission model.
        .addAnnotation(
            AnnotationSpec
                .builder(javax.annotation.processing.Generated.class)
                .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                .build())
        .addSuperinterface(superInterface)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addFields(
            exportType.parameters()
                .stream()
                .map(parameter -> FieldSpec
                    .builder(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.framework.ValueMapper.class),
                            TypeName.get(parameter.type).box()),
                        "mapper_" + parameter.name)
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build())
                .collect(Collectors.toList()))
        .addFields(
            computedAttributesCodeBlocks
                .stream()
                .map(codeBlocks -> codeBlocks.fieldDef())
                .collect(Collectors.toList()))
        .addMethod(
            MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                /* Suppress unchecked warnings because the resolver has to
                    put some big casting in for Class parameters
                 */
                .addAnnotation(
                    AnnotationSpec
                        .builder(SuppressWarnings.class)
                        .addMember("value", "$S", "unchecked")
                        .build())
                .addCode(
                    exportType.parameters()
                        .stream()
                        .map(parameter -> CodeBlock
                            .builder()
                            .addStatement(
                                "this.mapper_$L =\n$L",
                                parameter.name,
                                mapperBlocks.get(parameter.name)))
                        .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                        .build())
                .addCode(computedAttributesCodeBlocks.map(ComputedAttributesCodeBlocks::fieldInit).orElse(CodeBlock.of("")))
                .build())
        .addMethod(mapperMethodMaker.makeGetRequiredParametersMethod())
        .addMethod(mapperMethodMaker.makeGetParametersMethod())
        .addMethod(mapperMethodMaker.makeGetArgumentsMethod())
        .addMethod(mapperMethodMaker.makeInstantiateMethod())
        .addMethod(mapperMethodMaker.makeGetValidationFailuresMethod())
        .build());
  }

  private Optional<ComputedAttributesCodeBlocks> getComputedAttributesCodeBlocks(
      final MissionModelRecord missionModel,
      final ActivityTypeRecord activityType)
  {
    final Optional<CodeBlock> effectModelReturnMapperBlock;
    final TypeName computedAttributesTypeName;
    final Optional<EffectModelRecord> effectModel;
    effectModel = activityType.effectModel();
    final var typeMirror = effectModel.flatMap(EffectModelRecord::returnType);
    if (typeMirror.isPresent()) {
      effectModelReturnMapperBlock = new Resolver(this.typeUtils, this.elementUtils, missionModel.typeRules)
                  .instantiateNullableMapperFor(typeMirror.get());
      if (effectModelReturnMapperBlock.isEmpty()) {
        this.messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Failed to generate value mapper for effect model return type "
            + typeMirror.get()
            + " of activity "
            + activityType.name());
        return Optional.empty();
      }
      computedAttributesTypeName = TypeName.get(typeMirror.get());
    } else {
      effectModelReturnMapperBlock = new Resolver(
          this.typeUtils, this.elementUtils, missionModel.typeRules).applyRules(
          new TypePattern.ClassPattern(ClassName.get(ValueMapper.class),
                                       List.of(new TypePattern.ClassPattern(ClassName.get(Unit.class), List.of()))));
      computedAttributesTypeName = TypeName.get(Unit.class);
    }
    return Optional.of(new ComputedAttributesCodeBlocks(
        computedAttributesTypeName,
        FieldSpec
            .builder(
                ParameterizedTypeName.get(
                    ClassName.get(ValueMapper.class),
                    computedAttributesTypeName.box()),
                COMPUTED_ATTRIBUTES_VALUE_MAPPER_FIELD_NAME)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build(),
        CodeBlock
            .builder()
            .addStatement(
                "this.$L =\n$L",
                COMPUTED_ATTRIBUTES_VALUE_MAPPER_FIELD_NAME,
                effectModelReturnMapperBlock.get()).build()));
  }

  /** Generate `${activity_name}Mapper` class. */
  public Optional<JavaFile> generateActivityMapper(final MissionModelRecord missionModel, final ActivityTypeRecord activityType) {
    return generateCommonMapperMethods(missionModel, activityType)
        .map(typeSpec -> typeSpec
            .toBuilder()
            .addMethod(makeGetReturnValueSchemaMethod())
            .addMethod(makeSerializeReturnValueMethod(activityType))
            .addField(
                FieldSpec
                    .builder(
                        ParameterizedTypeName.get(
                            ClassName.get(Topic.class),
                            TypeName.get(activityType.declaration().asType())),
                        "inputTopic",
                        Modifier.PRIVATE,
                        Modifier.FINAL)
                    .initializer("new $T<>()", ClassName.get(Topic.class))
                    .build())
            .addField(
                FieldSpec
                    .builder(
                        ParameterizedTypeName.get(
                            ClassName.get(Topic.class),
                            activityType.getOutputTypeName()),
                        "outputTopic",
                        Modifier.PRIVATE,
                        Modifier.FINAL)
                    .initializer("new $T<>()", ClassName.get(Topic.class))
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("getInputTopic")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(
                        ClassName.get(Topic.class),
                        TypeName.get(activityType.declaration().asType())))
                    .addStatement("return this.$L", "inputTopic")
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("getOutputTopic")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(
                        ClassName.get(Topic.class),
                        activityType.getOutputTypeName()))
                    .addStatement("return this.$L", "outputTopic")
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("createTask")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(
                        ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.Task.class),
                        activityType
                            .effectModel()
                            .flatMap(EffectModelRecord::returnType)
                            .map(returnType -> TypeName.get(returnType).box())
                            .orElse(TypeName.get(Unit.class))))
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                            ClassName.get(missionModel.topLevelModel)),
                        "model",
                        Modifier.FINAL)
                    .addParameter(
                        TypeName.get(activityType.declaration().asType()),
                        "activity",
                        Modifier.FINAL)
                    .addCode(
                        activityType.effectModel()
                            .map(effectModel -> effectModel
                                .returnType()
                                .map(returnType -> CodeBlock
                                    .builder()
                                    .addStatement(
                                        """
                                          return $T
                                          .$L(() -> {
                                            try (final var restore = $T.model.set($L)) {
                                              $T.emit($L, this.$L);
                                              final var result = $L.$L($L.model());
                                              $T.emit(result, this.$L);
                                              return result;
                                            }
                                          })
                                          .create($L.executor())""",
                                        ModelActions.class,
                                        switch (effectModel.executor()) {
                                          case Threaded -> "threaded";
                                          case Replaying -> "replaying";
                                        },
                                        missionModel.getFactoryName(),
                                        "model",
                                        ModelActions.class,
                                        "activity",
                                        "inputTopic",
                                        "activity",
                                        effectModel.methodName(),
                                        "model",
                                        ModelActions.class,
                                        "outputTopic",
                                        "model")
                                    .build())
                                .orElseGet(() -> CodeBlock
                                    .builder()
                                    .addStatement(
                                        """
                                          return $T
                                          .$L(() -> {
                                            try (final var restore = $T.model.set($L)) {
                                              $T.emit($L, this.$L);
                                              $L.$L($L.model());
                                              $T.emit($T.UNIT, this.$L);
                                              return $T.UNIT;
                                            }
                                          })
                                          .create($L.executor())""",
                                        ModelActions.class,
                                        switch (effectModel.executor()) {
                                          case Threaded -> "threaded";
                                          case Replaying -> "replaying";
                                        },
                                        missionModel.getFactoryName(),
                                        "model",
                                        ModelActions.class,
                                        "activity",
                                        "inputTopic",
                                        "activity",
                                        effectModel.methodName(),
                                        "model",
                                        ModelActions.class,
                                        Unit.class,
                                        "outputTopic",
                                        Unit.class,
                                        "model")
                                    .build()))
                            .orElseGet(() -> CodeBlock
                                .builder()
                                .add(
                                    """
                                      return scheduler -> {
                                        scheduler.emit($L, this.$L);
                                        scheduler.emit($T.UNIT, this.$L);
                                        return $T.completed($T.UNIT);
                                      };
                                      """,
                                    "activity",
                                    "inputTopic",
                                    Unit.class,
                                    "outputTopic",
                                    TaskStatus.class,
                                    Unit.class)
                                .build()))
                    .build())
            .build())
        .map(typeSpec -> JavaFile
            .builder(activityType.mapper().name.packageName(), typeSpec)
            .skipJavaLangImports(true)
            .build());
  }

  private static MethodSpec makeGetReturnValueSchemaMethod() {
    return MethodSpec.methodBuilder("getReturnValueSchema")
                     .addModifiers(Modifier.PUBLIC)
                     .addAnnotation(Override.class)
                     .returns(ValueSchema.class)
                     .addStatement("return this." + COMPUTED_ATTRIBUTES_VALUE_MAPPER_FIELD_NAME + ".getValueSchema()")
                     .build();
  }

  private static MethodSpec makeSerializeReturnValueMethod(final ActivityTypeRecord activityType) {
    return MethodSpec.methodBuilder("serializeReturnValue")
                     .addModifiers(Modifier.PUBLIC)
                     .addAnnotation(Override.class)
                     .returns(SerializedValue.class)
                     .addParameter(
                         activityType.effectModel()
                             .flatMap(EffectModelRecord::returnType)
                             .map(TypeName::get)
                             .orElse(TypeName.get(Unit.class)).box(),
                         "returnValue",
                         Modifier.FINAL)
                     .addStatement(
                         "return this." + COMPUTED_ATTRIBUTES_VALUE_MAPPER_FIELD_NAME + ".serializeValue(returnValue)")
                     .build();
  }

  private Optional<Map<String, CodeBlock>> generateParameterMapperBlocks(final MissionModelRecord missionModel, final ExportTypeRecord exportType)
  {
    final var resolver = new Resolver(this.typeUtils, this.elementUtils, missionModel.typeRules);
    var failed = false;
    final var mapperBlocks = new HashMap<String, CodeBlock>();

    for (final var parameter : exportType.parameters()) {
      final var mapperBlock = resolver.instantiateNullableMapperFor(parameter.type);
      if (mapperBlock.isPresent()) {
        mapperBlocks.put(parameter.name, mapperBlock.get());
      } else {
        failed = true;
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Failed to generate value mapper for parameter",
            parameter.element);
      }
    }

    return failed ? Optional.empty() : Optional.of(mapperBlocks);
  }
}
