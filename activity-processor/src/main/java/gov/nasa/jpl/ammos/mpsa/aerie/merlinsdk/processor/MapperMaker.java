package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.AnnotationSpec;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.ValueSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.serialization.SerializedValue;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.ActivitiesMapped;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.ValueMapper;

import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;

class MapperMaker {
  private final Elements elementUtils;
  private final Types typeUtils;

  public MapperMaker(final ProcessingEnvironment processingEnv) {
    this.elementUtils = processingEnv.getElementUtils();
    this.typeUtils = processingEnv.getTypeUtils();
  }

  protected List<FieldSpec> makeMapperFieldSpecs(final ActivityTypeInfo activityTypeInfo) {
    final var mapperFieldSpecs = new ArrayList<FieldSpec>();
    for (final var entry : activityTypeInfo.parameters) {
      final var parameterName = entry.getKey();
      final var parameterTypeReference = entry.getValue();

      mapperFieldSpecs.add(FieldSpec
        .builder(
            ParameterizedTypeName.get(ClassName.get(ValueMapper.class), parameterTypeReference.getTypeName()),
            "mapper_" + parameterName,
            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .initializer("$L", parameterTypeReference.getMapper())
        .build());
    }

    return mapperFieldSpecs;
  }

  protected MethodSpec makeGetActivitySchemas(
      final FieldSpec activityTypeNameSpec,
      final ActivityTypeInfo activityTypeInfo
  ) {
    final String parametersVarName = "parameters";

    final CodeBlock schemasBlock;
    {
      final CodeBlock.Builder blockBuilder = CodeBlock.builder();

      for (final var entry : activityTypeInfo.parameters) {
        final var parameterName = entry.getKey();

        blockBuilder.addStatement("$1L.put($2S, this.mapper_$2L.getValueSchema())", parametersVarName, parameterName);
      }

      schemasBlock = blockBuilder.build();
    }

    return MethodSpec
        .methodBuilder("getActivitySchemas")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(TypeName.get(typeUtils.getDeclaredType(
            elementUtils.getTypeElement(Map.class.getCanonicalName()),
            elementUtils.getTypeElement(String.class.getCanonicalName()).asType(),
            typeUtils.getDeclaredType(
                elementUtils.getTypeElement(Map.class.getCanonicalName()),
                elementUtils.getTypeElement(String.class.getCanonicalName()).asType(),
                elementUtils.getTypeElement(ValueSchema.class.getCanonicalName()).asType()))))
        .addStatement("final var $L = new $T<$T, $T>()", parametersVarName, HashMap.class, String.class, ValueSchema.class)
        .addCode(schemasBlock)
        .addStatement("return $T.of($L, $L)", Map.class, activityTypeNameSpec.name, parametersVarName)
        .build();
  }

  protected MethodSpec makeDeserializeActivity(
      final FieldSpec activityTypeNameSpec,
      final ActivityTypeInfo activityTypeInfo
  ) {
    final ParameterSpec serializedActivitySpec = ParameterSpec
        .builder(TypeName.get(SerializedActivity.class), "serializedActivity", Modifier.FINAL)
        .build();
    final String entryVarName = "entry";
    final String activityVarName = "activity";

    final CodeBlock parameterDeserializeBlock;
    {
      final CodeBlock.Builder blockBuilder = CodeBlock.builder();

      blockBuilder
          .beginControlFlow("for (final var $L : $L.getParameters().entrySet())", entryVarName, serializedActivitySpec.name)
          .beginControlFlow("switch ($L.getKey())", entryVarName);
      for (final var entry : activityTypeInfo.parameters) {
        final var parameterName = entry.getKey();

        blockBuilder
            .add("case $S:\n", parameterName)
            .indent()
            .addStatement("$L.$L = this.mapper_$L.deserializeValue($L.getValue()).getSuccessOrThrow()",
                activityVarName, parameterName, parameterName, entryVarName)
            .addStatement("break")
            .unindent();
      }
      blockBuilder
          .add("default:\n")
          .indent()
          .addStatement("return Optional.empty()")
          .unindent()
          .endControlFlow()
          .endControlFlow();

      parameterDeserializeBlock = blockBuilder.build();
    }

    return MethodSpec
        .methodBuilder("deserializeActivity")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(serializedActivitySpec)
        .returns(ParameterizedTypeName.get(
                ClassName.get(Optional.class),
                ClassName.get(Activity.class)))
        .beginControlFlow("if (!$L.getTypeName().equals($L))", serializedActivitySpec.name, activityTypeNameSpec.name)
        .addStatement("return $T.empty()", Optional.class)
        .endControlFlow()
        .addCode("\n")
        .addStatement("final var $L = new $T()", activityVarName, activityTypeInfo.javaType)
        .addCode(parameterDeserializeBlock)
        .addCode("\n")
        .addStatement("return $T.of($L)", Optional.class, activityVarName)
        .build();
  }

  protected MethodSpec makeSerializeActivity(
      final FieldSpec activityTypeNameSpec,
      final ActivityTypeInfo activityTypeInfo
  ) {
    final ParameterSpec abstractActivitySpec = ParameterSpec
        .builder(TypeName.get(Activity.class), "abstractActivity", Modifier.FINAL)
        .build();

    final String activityVarName = "activity";
    final String parametersVarName = "parameters";

    final CodeBlock serializeParametersBlock;
    {
      final CodeBlock.Builder blockBuilder = CodeBlock.builder();

      for (final var entry : activityTypeInfo.parameters) {
        final var parameterName = entry.getKey();

        blockBuilder.addStatement("$L.put($S, this.mapper_$L.serializeValue($L.$L))",
            parametersVarName, parameterName, parameterName, activityVarName, parameterName);
      }

      serializeParametersBlock = blockBuilder.build();
    }

    return MethodSpec
        .methodBuilder("serializeActivity")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(abstractActivitySpec)
        .returns(TypeName.get(typeUtils.getDeclaredType(
            elementUtils.getTypeElement(Optional.class.getCanonicalName()),
            elementUtils.getTypeElement(SerializedActivity.class.getCanonicalName()).asType())))
        .addStatement("if (!($L instanceof $T)) return $T.empty()", abstractActivitySpec.name, activityTypeInfo.javaType, Optional.class)
        .addStatement("final $T $L = ($T)abstractActivity", activityTypeInfo.javaType, activityVarName, activityTypeInfo.javaType)
        .addCode("\n")
        .addStatement("final var $L = new $T<$T, $T>()", parametersVarName, HashMap.class, String.class, SerializedValue.class)
        .addCode(serializeParametersBlock)
        .addCode("\n")
        .addStatement("return $T.of(new $T($L, $L))", Optional.class, SerializedActivity.class, activityTypeNameSpec.name, parametersVarName)
        .build();
  }

  public JavaFile makeActivityMapper(final ActivityTypeInfo activityTypeInfo) {
    final FieldSpec activityTypeNameSpec = FieldSpec
        .builder(String.class, "ACTIVITY_TYPE", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .initializer("$S", activityTypeInfo.name)
        .build();

    final List<FieldSpec> mapperFieldSpecs = makeMapperFieldSpecs(activityTypeInfo);

    final MethodSpec getActivitySchemasSpec = makeGetActivitySchemas(activityTypeNameSpec, activityTypeInfo);
    final MethodSpec deserializeActivitySpec = makeDeserializeActivity(activityTypeNameSpec, activityTypeInfo);
    final MethodSpec serializeActivitySpec = makeSerializeActivity(activityTypeNameSpec, activityTypeInfo);


    final TypeSpec activityMapperSpec = TypeSpec
        .classBuilder(activityTypeInfo.javaType.asElement().getSimpleName().toString() + "$$ActivityMapper")
        .addSuperinterface(ActivityMapper.class)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addAnnotation(AnnotationSpec
            .builder(Generated.class)
            .addMember("value", "$S", this.getClass().getCanonicalName())
            .build())
        .addAnnotation(AnnotationSpec
            .builder(ActivitiesMapped.class)
            .addMember("value", "$T.class", activityTypeInfo.javaType)
            .build())
        .addField(activityTypeNameSpec)
        .addFields(mapperFieldSpecs)
        .addMethod(getActivitySchemasSpec)
        .addMethod(deserializeActivitySpec)
        .addMethod(serializeActivitySpec)
        .build();

    final JavaFile javaFile = JavaFile
        .builder(
            elementUtils.getPackageOf(activityTypeInfo.javaType.asElement()).getQualifiedName().toString(),
            activityMapperSpec)
        .build();

    return javaFile;
  }
}
