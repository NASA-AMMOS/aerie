package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.AnnotationSpec;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;

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
        final var parameterTypeReference = entry.getValue();

        if (parameterTypeReference.isPrimitive) {
          switch (parameterTypeReference.typeName) {
            case "double":
            case "float":
              blockBuilder.addStatement("$L.put($S, $T.REAL)", parametersVarName, parameterName, ParameterSchema.class);
              break;

            case "byte":
            case "short":
            case "int":
            case "long":
              blockBuilder.addStatement("$L.put($S, $T.INT)", parametersVarName, parameterName, ParameterSchema.class);
              break;

            case "boolean":
              blockBuilder.addStatement("$L.put($S, $T.BOOLEAN)", parametersVarName, parameterName, ParameterSchema.class);
              break;

            case "char":
            case "string":
              blockBuilder.addStatement("$L.put($S, $T.STRING)", parametersVarName, parameterName, ParameterSchema.class);
              break;

            case "array_int":
              blockBuilder.addStatement("$L.put($S, $T.ofList($T.INT))", parametersVarName, parameterName, ParameterSchema.class, ParameterSchema.class);
              break;

            default:
              throw new RuntimeException("Found parameter of unknown primitive type `" + parameterTypeReference.typeName + "`");
          }
        } else {
          throw new RuntimeException("Found parameter of unknown type `" + parameterTypeReference.typeName + "`");
        }
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
                elementUtils.getTypeElement(ParameterSchema.class.getCanonicalName()).asType()))))
        .addStatement("final var $L = new $T<$T, $T>()", parametersVarName, HashMap.class, String.class, ParameterSchema.class)
        .addCode(schemasBlock)
        .addCode("\n")
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

    final CodeBlock parameterDeclarationsBlock;
    {
      final CodeBlock.Builder blockBuilder = CodeBlock.builder();

      for (final var entry : activityTypeInfo.parameters) {
        final var parameterName = entry.getKey();
        final var parameterTypeReference = entry.getValue();

        if (parameterTypeReference.isPrimitive) {
          switch (parameterTypeReference.typeName) {
            case "double":
              blockBuilder.addStatement("$T<$T> param_$L = $T.empty()", Optional.class, Double.class, parameterName, Optional.class);
              break;
            case "float":
              blockBuilder.addStatement("$T<$T> param_$L = $T.empty()", Optional.class, Float.class, parameterName, Optional.class);
              break;

            case "byte":
              blockBuilder.addStatement("$T<$T> param_$L = $T.empty()", Optional.class, Byte.class, parameterName, Optional.class);
              break;
            case "short":
              blockBuilder.addStatement("$T<$T> param_$L = $T.empty()", Optional.class, Short.class, parameterName, Optional.class);
              break;
            case "int":
              blockBuilder.addStatement("$T<$T> param_$L = $T.empty()", Optional.class, Integer.class, parameterName, Optional.class);
              break;
            case "long":
              blockBuilder.addStatement("$T<$T> param_$L = $T.empty()", Optional.class, Long.class, parameterName, Optional.class);
              break;

            case "boolean":
              blockBuilder.addStatement("$T<$T> param_$L = $T.empty()", Optional.class, Boolean.class, parameterName, Optional.class);
              break;

            case "char":
              blockBuilder.addStatement("$T<$T> param_$L = $T.empty()", Optional.class, Character.class, parameterName, Optional.class);
              break;
            case "string":
              blockBuilder.addStatement("$T<$T> param_$L = $T.empty()", Optional.class, String.class, parameterName, Optional.class);
              break;
              
            case "array_int":
              blockBuilder.addStatement("$T<int[]> param_$L = $T.empty()", Optional.class, parameterName, Optional.class);
              break;

            default:
              throw new RuntimeException("Found parameter of unknown primitive type `" + parameterTypeReference.typeName + "`");
          }
        } else {
          throw new RuntimeException("Found parameter of unknown type `" + parameterTypeReference.typeName + "`");
        }
      }

      parameterDeclarationsBlock = blockBuilder.build();
    }

    final CodeBlock parameterDeserializeBlock;
    {
      final CodeBlock.Builder blockBuilder = CodeBlock.builder();

      blockBuilder
          .beginControlFlow("for (final var $L : $L.getParameters().entrySet())", entryVarName, serializedActivitySpec.name)
          .beginControlFlow("switch ($L.getKey())", entryVarName);
      for (final var entry : activityTypeInfo.parameters) {
        final var parameterName = entry.getKey();
        final var parameterTypeReference = entry.getValue();

        blockBuilder.beginControlFlow("case $S:", parameterName);
        if (parameterTypeReference.isPrimitive) {
          switch (parameterTypeReference.typeName) {
            case "double":
              blockBuilder
                  .addStatement("final var givenValue = $L.getValue().asReal().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected real number\"))", entryVarName)
                  .addStatement("param_$L = $T.of(givenValue)", parameterName, Optional.class);
              break;
            case "float":
              blockBuilder
                  .addStatement("final var givenValue = $L.getValue().asReal().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected real number\"))", entryVarName)
                  .addStatement("final var coercedValue = givenValue.floatValue()")
                  // TODO: check if value converted losslessly, or at least within a tolerance?
                  .addStatement("param_$L = $T.of(coercedValue)", parameterName, Optional.class);
              break;

            case "byte":
              blockBuilder
                  .addStatement("final var givenValue = $L.getValue().asInt().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected integral number\"))", entryVarName)
                  .addStatement("final var coercedValue = givenValue.byteValue()")
                  .addStatement("if (((long)givenValue) != coercedValue) throw new RuntimeException(\"Invalid parameter; value outside range of `byte`\")")
                  .addStatement("param_$L = $T.of(coercedValue)", parameterName, Optional.class);
              break;
            case "short":
              blockBuilder
                  .addStatement("final var givenValue = $L.getValue().asInt().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected integral number\"))", entryVarName)
                  .addStatement("final var coercedValue = givenValue.shortValue()")
                  .addStatement("if (((long)givenValue) != coercedValue) throw new RuntimeException(\"Invalid parameter; value outside range of `short`\")")
                  .addStatement("param_$L = $T.of(coercedValue)", parameterName, Optional.class);
              break;
            case "int":
              blockBuilder
                  .addStatement("final var givenValue = $L.getValue().asInt().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected integral number\"))", entryVarName)
                  .addStatement("final var coercedValue = givenValue.intValue()")
                  .addStatement("if (((long)givenValue) != coercedValue) throw new RuntimeException(\"Invalid parameter; value outside range of `int`\")")
                  .addStatement("param_$L = $T.of(coercedValue)", parameterName, Optional.class);
              break;
            case "long":
              blockBuilder
                  .addStatement("final var givenValue = $L.getValue().asInt().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected integral number\"))", entryVarName)
                  .addStatement("param_$L = $T.of(givenValue)", parameterName, Optional.class);
              break;

            case "boolean":
              blockBuilder
                  .addStatement("final var givenValue = $L.getValue().asBoolean().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected boolean\"))", entryVarName)
                  .addStatement("param_$L = $T.of(givenValue)", parameterName, Optional.class);
              break;

            case "char":
              blockBuilder
                  .addStatement("final var givenValue = $L.getValue().asString().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected character\"))", entryVarName)
                  .addStatement("if (givenValue.length() != 1) throw new RuntimeException(\"Invalid parameter; expected single-character string\")")
                  .addStatement("param_$L = $T.of(givenValue.charAt(0))", parameterName, Optional.class);
              break;
            case "string":
              blockBuilder
                  .addStatement("final var givenValue = $L.getValue().asString().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected string\"))", entryVarName)
                  .addStatement("param_$L = $T.of(givenValue)", parameterName, Optional.class);
              break;

            case "array_int":
              blockBuilder
                      .addStatement("final var givenValue = $L.getValue().asList().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected array_int\"))", entryVarName)
                      .addStatement("int[] convertedValue = new int[givenValue.size()]")
                      .add(         "for (int index=0; index<givenValue.size(); index++) {\n")
                      .add(         "    long value = givenValue.get(index).asInt().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected integral number\"));\n")
                      .add(         "    if (value != (int)value) throw new RuntimeException(\"Invalid parameter; value outside range of `int`\");\n")
                      .add(         "    convertedValue[index] = (int)value;\n")
                      .add(         "}\n")
                      .addStatement("param_$L = $T.of(convertedValue)", parameterName, Optional.class);
              break;

            default:
              throw new RuntimeException("Found parameter of unknown primitive type `" + parameterTypeReference.typeName + "`");
          }
        } else {
          throw new RuntimeException("Found parameter of unknown type `" + parameterTypeReference.typeName + "`");
        }
        blockBuilder
            .addStatement("break")
            .endControlFlow();
      }
      blockBuilder
          .add("default:\n")
          .indent()
          .addStatement("throw new $T(\"Unknown key `\" + $L.getKey() + \"`\")", RuntimeException.class, entryVarName)
          .unindent()
          .endControlFlow()
          .endControlFlow();

      parameterDeserializeBlock = blockBuilder.build();
    }

    final CodeBlock parameterInjectionBlock;
    {
      final CodeBlock.Builder blockBuilder = CodeBlock.builder();

      for (final var entry : activityTypeInfo.parameters) {
        final String parameterName = entry.getKey();

        blockBuilder.addStatement("param_$L.ifPresent(p -> $L.$L = p)", parameterName, activityVarName, parameterName);
      }

      parameterInjectionBlock = blockBuilder.build();
    }
    
    return MethodSpec
        .methodBuilder("deserializeActivity")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(serializedActivitySpec)
        .returns(ParameterizedTypeName.get(
                ClassName.get(Optional.class),
                ParameterizedTypeName.get(
                        ClassName.get(Activity.class),
                        WildcardTypeName.subtypeOf(StateContainer.class))))
        .beginControlFlow("if (!$L.getTypeName().equals($L))", serializedActivitySpec.name, activityTypeNameSpec.name)
        .addStatement("return $T.empty()", Optional.class)
        .endControlFlow()
        .addCode("\n")
        .addCode(parameterDeclarationsBlock)
        .addCode("\n")
        .addCode(parameterDeserializeBlock)
        .addCode("\n")
        .addStatement("final var $L = new $T()", activityVarName, activityTypeInfo.javaType)
        .addCode(parameterInjectionBlock)
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
        final var parameterTypeReference = entry.getValue();

        if (parameterTypeReference.isPrimitive) {
          if (Objects.equals(parameterTypeReference.typeName, "char")) {
            blockBuilder.addStatement("$L.put($S, $T.of(Character.toString($L.$L)))", parametersVarName, parameterName, SerializedParameter.class, activityVarName, parameterName);
          } else if (Objects.equals(parameterTypeReference.typeName, "array_int")) {
            blockBuilder
                    .addStatement("$T<$T> arr = new $T<>()", List.class, SerializedParameter.class, ArrayList.class)
                    .addStatement("for (var val : $L.$L) arr.add($T.of(val))", activityVarName, parameterName, SerializedParameter.class)
                    .addStatement("$L.put($S, $T.of(arr))", parametersVarName, parameterName, SerializedParameter.class);
          } else {
            blockBuilder.addStatement("$L.put($S, $T.of($L.$L))", parametersVarName, parameterName, SerializedParameter.class, activityVarName, parameterName);
          }
        } else {
          // TODO: handle non-primitive parameters
          throw new RuntimeException("Can't handle non-primitive parameters yet");
        }
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
        .beginControlFlow("if (!($L instanceof $T))", abstractActivitySpec.name, activityTypeInfo.javaType)
          .addStatement("return $T.empty()", Optional.class)
        .endControlFlow()
        .addCode("\n")
        .addStatement("final $T $L = ($T)abstractActivity", activityTypeInfo.javaType, activityVarName, activityTypeInfo.javaType)
        .addCode("\n")
        .addStatement("final var $L = new $T<$T, $T>()", parametersVarName, HashMap.class, String.class, SerializedParameter.class)
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
            .builder(gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivitiesMapped.class)
            .addMember("value", "$T.class", activityTypeInfo.javaType)
            .build())
        .addField(activityTypeNameSpec)
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
