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
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.typemappers.*;

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

  // Works for lists and arrays, will need to be refactored.
  private String makeSubtypeSchema(String type) {
    switch (type.split("_")[0]) {
      case "double": case "float":
        return "$1T.REAL";

      case "byte": case "short": case "int": case "long":
        return "$1T.INT";

      case "boolean":
        return "$1T.BOOLEAN";

      case "char": case "string":
        return "$1T.STRING";

      case "list": case "array":
        return "$1T.ofList(" + makeSubtypeSchema(type.substring(type.indexOf('_')+1)) + ")";

      default:
        // TODO: Deal with this
        throw new RuntimeException("Unknown parameter type " + type);
    }
  }

  private void addMapper(CodeBlock.Builder blockBuilder, String type) {
    switch (type.split("_")[0]) {
      case "double":
        blockBuilder.add("new $T()", DoubleParameterMapper.class);
        break;
      case "float":
        blockBuilder.add("new $T()", FloatParameterMapper.class);
        break;
      case "byte":
        blockBuilder.add("new $T()", ByteParameterMapper.class);
        break;
      case "short":
        blockBuilder.add("new $T()", ShortParameterMapper.class);
        break;
      case "int":
        blockBuilder.add("new $T()", IntegerParameterMapper.class);
        break;
      case "long":
        blockBuilder.add("new $T()", LongParameterMapper.class);
        break;
      case "boolean":
        blockBuilder.add("new $T()", BooleanParameterMapper.class);
        break;
      case "char":
        blockBuilder.add("new $T()", CharacterParameterMapper.class);
        break;
      case "string":
        blockBuilder.add("new $T()", StringParameterMapper.class);
        break;
      case "list": {
        String elementType = type.substring(type.indexOf('_') + 1);
        blockBuilder.add("new $T<>(", ListParameterMapper.class);
        addMapper(blockBuilder, elementType);
        blockBuilder.add(")");
        break;
      }
      case "array": {
        String elementType = type.substring(type.indexOf('_') + 1);
        blockBuilder.add("new $T<>(", ArrayParameterMapper.class);
        addMapper(blockBuilder, elementType);
        blockBuilder.add(", ");
        addType(blockBuilder, elementType);
        blockBuilder.add(".class)");
        break;
      }
      default:
        // TODO: Deal with this
        throw new RuntimeException("Unknown parameter type " + type);
    }
  }

  private void addType(CodeBlock.Builder blockBuilder, String type) {
    switch (type.split("_")[0]) {
      case "double":
        blockBuilder.add("$T", Double.class);
        break;
      case "float":
        blockBuilder.add("$T", Float.class);
        break;
      case "byte":
        blockBuilder.add("$T", Byte.class);
        break;
      case "short":
        blockBuilder.add("$T", Short.class);
        break;
      case "int":
        blockBuilder.add("$T", Integer.class);
        break;
      case "long":
        blockBuilder.add("$T", Long.class);
        break;
      case "boolean":
        blockBuilder.add("$T", Boolean.class);
        break;
      case "char":
        blockBuilder.add("$T", Character.class);
        break;
      case "string":
        blockBuilder.add("$T", String.class);
        break;
      case "list":
        blockBuilder.add("$T<", List.class);
        addType(blockBuilder, type.substring(type.indexOf('_')+1));
        blockBuilder.add(">");
        break;
      case "array":
        addType(blockBuilder, type.substring(type.indexOf('_')+1));
        blockBuilder.add("[]");
        break;
      default:
        // TODO: Deal with this
        throw new RuntimeException("Unknown array type " + type);
    }
  }

  private CodeBlock makeTypeSchema(String type) {
    return CodeBlock.builder()
            .add(makeSubtypeSchema(type), ParameterSchema.class)
            .build();
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
          blockBuilder.addStatement("$L.put($S, $L)", parametersVarName, parameterName, makeTypeSchema(parameterTypeReference.typeName));
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
          blockBuilder.add("$T<", Optional.class);
          addType(blockBuilder, parameterTypeReference.typeName);
          blockBuilder.add("> param_$L = $T.empty();\n", parameterName, Optional.class);
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
          blockBuilder.add("final var mapper = ");
          addMapper(blockBuilder, parameterTypeReference.typeName);
          blockBuilder.add(";\n");
          blockBuilder.addStatement("final var givenValue = mapper.deserializeParameter($L.getValue()).getSuccessOrThrow()", entryVarName);
          blockBuilder.addStatement("param_$L = $T.of(givenValue)", parameterName, Optional.class);
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
          } else if ( List.of("array", "list").contains(parameterTypeReference.typeName.split("_")[0]) ) {
            blockBuilder.beginControlFlow("");
            blockBuilder.add("final var mapper = ");
            addMapper(blockBuilder, parameterTypeReference.typeName);
            blockBuilder.add(";\n");
            blockBuilder.addStatement("$L.put($S, mapper.serializeParameter($L.$L))", parametersVarName, parameterName, activityVarName, parameterName);
            blockBuilder.endControlFlow();

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
