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
import com.squareup.javapoet.WildcardTypeName;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.EnumValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.VoidEnum;
import gov.nasa.jpl.aerie.merlin.processor.MissionModelProcessor;
import gov.nasa.jpl.aerie.merlin.processor.Resolver;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ConfigurationTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.EffectModelRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ExportTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.MissionModelRecord;
import gov.nasa.jpl.aerie.merlin.protocol.model.MerlinPlugin;
import gov.nasa.jpl.aerie.merlin.protocol.model.MissionModelFactory;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerModel;
import gov.nasa.jpl.aerie.merlin.protocol.model.SchedulerPlugin;
import gov.nasa.jpl.aerie.merlin.protocol.types.DurationType;
import gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue;
import gov.nasa.jpl.aerie.merlin.protocol.types.ValueSchema;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
                        .orElse(ClassName.get(gov.nasa.jpl.aerie.merlin.framework.VoidEnum.class)),
                    ParameterizedTypeName.get(
                        ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                        ClassName.get(missionModel.topLevelModel))))
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
                    .methodBuilder("getTaskSpecTypes")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(
                        ClassName.get(Map.class),
                        ClassName.get(String.class),
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.class),
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                                ClassName.get(missionModel.topLevelModel)),
                            WildcardTypeName.subtypeOf(Object.class),
                            WildcardTypeName.subtypeOf(Object.class))))
                    .addStatement("return $T.activityTypes", missionModel.getTypesName())
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("instantiate")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(
                        missionModel.modelConfigurationType
                            .map($ -> ClassName.get($.declaration()))
                            .orElse(ClassName.get(gov.nasa.jpl.aerie.merlin.framework.VoidEnum.class)),
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
                        (missionModel.modelConfigurationType.isPresent())
                            ? CodeBlock.of(
                                "new $T($L, $L)",
                                ClassName.get(missionModel.topLevelModel),
                                "registrar",
                                "configuration")
                            : CodeBlock.of(
                                "new $T($L))",
                                ClassName.get(missionModel.topLevelModel),
                                "registrar"))
                    .addStatement(
                        "return new $T<$T>($L, $L)",
                        gov.nasa.jpl.aerie.merlin.framework.RootModel.class,
                        ClassName.get(missionModel.topLevelModel),
                        "model",
                        "executor")
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
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
                                    "final var $L = new $T()",
                                    "mapper",
                                    entry.mapper().name)
                                .addStatement(
                                    "return $T.spawn($L.getName(), $L.getArguments($L))",
                                    gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                    "mapper",
                                    "mapper",
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
                                    "final var $L = new $T()",
                                    "mapper",
                                    entry.mapper().name)
                                .addStatement(
                                    "return $T.defer($L, $L.getName(), $L.getArguments($L))",
                                    gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                    "duration",
                                    "mapper",
                                    "mapper",
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
            .addField(
                FieldSpec
                    .builder(
                        ParameterizedTypeName.get(
                            ClassName.get(Map.class),
                            ClassName.get(String.class),
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.class),
                                ParameterizedTypeName.get(
                                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                                    ClassName.get(missionModel.topLevelModel)),
                                WildcardTypeName.subtypeOf(Object.class),
                                WildcardTypeName.subtypeOf(Object.class))),
                        "activityTypes",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer(
                        "$T.ofEntries($>$>\n$L$<$<)",
                        Map.class,
                        missionModel.activityTypes
                            .stream()
                            .map(activityType -> CodeBlock.builder().add(
                                "$T.entry($S, new $T())",
                                Map.class,
                                activityType.name(),
                                activityType.mapper().name))
                            .reduce((x, y) -> x.add(",\n$L", y.build()))
                            .orElse(CodeBlock.builder())
                            .build())
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
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
      effectModelReturnMapperBlock = Optional.of(CodeBlock.of("new $T(VoidEnum.class)", EnumValueMapper.class));
      computedAttributesTypeName = TypeName.get(VoidEnum.class);
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
    return generateCommonMapperMethods(missionModel, activityType).map(typeSpec -> typeSpec.toBuilder()
        .addMethod(makeGetReturnValueSchemaMethod())
        .addMethod(makeSerializeReturnValueMethod(activityType))
        .addMethod(
            MethodSpec
                .methodBuilder("getName")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", activityType.name())
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
                        .orElse(TypeName.get(VoidEnum.class))))
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
                        .map(effectModel -> CodeBlock
                              .builder()
                              .addStatement(
                                  "return $T.$L(() -> $L.$L($L.model())).create($L.executor())",
                                  gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                  switch (effectModel.executor()) {
                                    case Threaded -> "threaded";
                                    case Replaying -> "replaying";
                                  },
                                  "activity",
                                  effectModel.methodName(),
                                  "model",
                                  "model")
                              .build())
                        .orElseGet(() -> CodeBlock
                            .builder()
                            .addStatement(
                                "return new $T($$ -> {})",
                                gov.nasa.jpl.aerie.merlin.framework.OneShotTask.class)
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
                             .orElse(TypeName.get(VoidEnum.class)).box(),
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
