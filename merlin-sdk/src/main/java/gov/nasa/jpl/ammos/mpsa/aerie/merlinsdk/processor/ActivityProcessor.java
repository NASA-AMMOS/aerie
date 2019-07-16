package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ParameterSchema;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ParameterType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedActivity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation.SerializedParameter;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Generated;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@SupportedAnnotationTypes({
    "gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType",
    "gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ParameterType",
    "gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter",
})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public final class ActivityProcessor extends AbstractProcessor {
  private final Map<DeclaredType, ParameterInfo> parameterTypes = new HashMap<>();
  private final Map<DeclaredType, ActivityType> activityTypes = new HashMap<>();

  private ParameterArg toParameterArg(final TypeMirror element) {
    final Types typeUtils = processingEnv.getTypeUtils();
    final Elements elementUtils = processingEnv.getElementUtils();

    final TypeMirror optionalType = typeUtils.erasure(elementUtils.getTypeElement(Optional.class.getCanonicalName()).asType());

    final TypeMirror wrapperType = typeUtils.erasure(element);

    final boolean optional;
    final TypeMirror innerType;
    if (typeUtils.isSameType(optionalType, wrapperType)) {
      optional = true;
      innerType = ((DeclaredType)element).getTypeArguments().get(0);
    } else {
      optional = false;
      innerType = element;
    }

    TypeMirror reducedType;
    try {
      reducedType = typeUtils.unboxedType(innerType);
    } catch (final IllegalArgumentException ex) {
      reducedType = innerType;
    }

    return new ParameterArg(reducedType, optional);
  }

  private void addParameterType(final TypeElement parameterType) {
    final DeclaredType parameterTypeDeclaration = (DeclaredType)parameterType.asType();

    if (parameterTypeDeclaration.getTypeArguments().size() != 0) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "A parameter type cannot have generic type parameters", parameterType);
      return;
    }

    final List<Pair<String, ParameterArg>> activityParameters = new ArrayList<>();
    for (final Element element : parameterType.getEnclosedElements()) {
      if (element.getKind() != ElementKind.FIELD) {
        continue;
      }

      if (element.getAnnotation(Parameter.class) != null) {
        activityParameters.add(Pair.of(element.getSimpleName().toString(), toParameterArg(element.asType())));
      }
    }

    parameterTypes.put(parameterTypeDeclaration, new ParameterInfo(parameterTypeDeclaration, activityParameters));
  }

  private void updateParameterTypes(final RoundEnvironment roundEnv) {
    for (final Element element : roundEnv.getElementsAnnotatedWith(ParameterType.class)) {
      if (!List.of(ElementKind.CLASS, ElementKind.ENUM).contains(element.getKind())) {
        throw new RuntimeException("@ParameterType must be applied to a class or enum");
      }

      addParameterType((TypeElement)element);
    }

    for (final Element element : roundEnv.getElementsAnnotatedWith(ActivityType.class)) {
      if (!List.of(ElementKind.CLASS).contains(element.getKind())) {
        throw new RuntimeException("@ActivityType must be applied to a class");
      }

      addParameterType((TypeElement)element);
    }
  }

  private void updateActivityTypes(final RoundEnvironment roundEnv) {
    for (final Element element : roundEnv.getElementsAnnotatedWith(ActivityType.class)) {
      if (!List.of(ElementKind.CLASS).contains(element.getKind())) {
        throw new RuntimeException("@ActivityType must be applied to a class");
      }
      final DeclaredType activityType = (DeclaredType)element.asType();
      final ActivityType activityTypeAnnotation = element.getAnnotation(ActivityType.class);

      activityTypes.put(activityType, activityTypeAnnotation);

      final com.sun.source.util.DocTrees x = com.sun.source.util.DocTrees.instance(processingEnv);
      final com.sun.source.doctree.DocCommentTree t = x.getDocCommentTree(element);
      System.out.println("Activity type: " + activityTypeAnnotation.value());
      if (t != null) {
        System.out.println("\tFirst sentence: " + t.getFirstSentence());
      }
    }
  }

  private JavaFile makeActivityMapper(final DeclaredType activityType) {
    final var elementUtils = this.processingEnv.getElementUtils();
    final var typeUtils = this.processingEnv.getTypeUtils();
    final TypeMirror stringType = elementUtils.getTypeElement(String.class.getCanonicalName()).asType();

    ParameterInfo parameterInfo = parameterTypes.get(activityType);

    final FieldSpec activityTypeNameSpec = FieldSpec
        .builder(String.class, "ACTIVITY_TYPE", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .initializer("$S", activityTypes.get(activityType).value())
        .build();

    final MethodSpec getActivitySchemasSpec;
    {
      final String parametersVarName = "parameters";

      final CodeBlock schemasBlock;
      {
        final CodeBlock.Builder blockBuilder = CodeBlock.builder();

        for (final var nameTypePair : parameterInfo.getParameterTypes()) {
          final var parameterName = nameTypePair.getKey();
          final var parameterType = nameTypePair.getValue().getType();

          if (parameterType.getKind() == TypeKind.DOUBLE) {
            blockBuilder.addStatement("$L.put($S, $T.ofDouble())", parametersVarName, parameterName, ParameterSchema.class);
          } else if (parameterType.getKind() == TypeKind.INT) {
            blockBuilder.addStatement("$L.put($S, $T.ofInt())", parametersVarName, parameterName, ParameterSchema.class);
          } else if (parameterType.getKind() == TypeKind.DECLARED) {
            if (typeUtils.isSameType(stringType, parameterType)) {
              blockBuilder.addStatement("$L.put($S, $T.ofString())", parametersVarName, parameterName, ParameterSchema.class);
            } else {
              throw new RuntimeException("Found parameter `" + parameterType + "`, which is not a known parameter type");
            }
          }
        }

        schemasBlock = blockBuilder.build();
      }

      getActivitySchemasSpec = MethodSpec
          .methodBuilder("getActivitySchemas")
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Override.class)
          .returns(TypeName.get(typeUtils.getDeclaredType(
              elementUtils.getTypeElement(Map.class.getCanonicalName()),
              elementUtils.getTypeElement(String.class.getCanonicalName()).asType(),
              elementUtils.getTypeElement(ParameterSchema.class.getCanonicalName()).asType())))
          .addStatement("final var $L = new $T<$T, $T>()", parametersVarName, HashMap.class, String.class, ParameterSchema.class)
          .addCode(schemasBlock)
          .addCode("\n")
          .addStatement("return $T.of($L, $T.ofMap($L))", Map.class, activityTypeNameSpec.name, ParameterSchema.class, parametersVarName)
          .build();
    }

    final MethodSpec deserializeActivitySpec;
    {
      final ParameterSpec serializedActivitySpec = ParameterSpec
          .builder(TypeName.get(SerializedActivity.class), "serializedActivity", Modifier.FINAL)
          .build();
      final String entryVarName = "entry";
      final String activityVarName = "activity";

      final CodeBlock parameterDeclarationsBlock;
      {
        final CodeBlock.Builder blockBuilder = CodeBlock.builder();

        for (final var nameTypePair : parameterInfo.getParameterTypes()) {
          final var parameterType = nameTypePair.getValue().getType();
          final TypeMirror boxedType;
          if (parameterType instanceof PrimitiveType) {
            boxedType = typeUtils.boxedClass((PrimitiveType)parameterType).asType();
          } else {
            boxedType = parameterType;
          }
          blockBuilder.addStatement("$T<$T> param_$L = $T.empty()", Optional.class, boxedType, nameTypePair.getKey(), Optional.class);
        }

        parameterDeclarationsBlock = blockBuilder.build();
      }

      final CodeBlock parameterDeserializeBlock;
      {
        final CodeBlock.Builder blockBuilder = CodeBlock.builder();

        blockBuilder
            .beginControlFlow("for (final var $L : $L.getParameters().entrySet())", entryVarName, serializedActivitySpec.name)
            .beginControlFlow("switch ($L.getKey())", entryVarName);
        for (final var nameTypePair : parameterInfo.getParameterTypes()) {
          final var parameterName = nameTypePair.getKey();
          final var parameterType = nameTypePair.getValue().getType();

          blockBuilder
              .add("case $S:\n", nameTypePair.getKey())
              .indent();
          if (parameterType.getKind() == TypeKind.DOUBLE) {
            blockBuilder.addStatement("param_$L = $T.of($L.getValue().asDouble().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected double\")))", parameterName, Optional.class, entryVarName);
          } else if (parameterType.getKind() == TypeKind.INT) {
            blockBuilder.addStatement("param_$L = $T.of($L.getValue().asInt().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected int\")))", parameterName, Optional.class, entryVarName);
          } else if (parameterType.getKind() == TypeKind.DECLARED) {
            if (typeUtils.isSameType(stringType, parameterType)) {
              blockBuilder.addStatement("param_$L = $T.of($L.getValue().asString().orElseThrow(() -> new RuntimeException(\"Invalid parameter; expected string\")))", parameterName, Optional.class, entryVarName);
              // TODO: Lookup and deserialize custom parameter types from the parameterTypes map.
            } else {
              throw new RuntimeException("SerializedParameter has argument `" + parameterType + "`, which is not a parameter type");
            }
          }
          blockBuilder
              .addStatement("break")
              .unindent();
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

        for (final var nameTypePair : parameterInfo.getParameterTypes()) {
          final var parameterName = nameTypePair.getKey();
          blockBuilder.addStatement("param_$L.ifPresent(p -> $L.$L = p)", parameterName, activityVarName, parameterName);
        }

        parameterInjectionBlock = blockBuilder.build();
      }

      deserializeActivitySpec = MethodSpec
          .methodBuilder("deserializeActivity")
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Override.class)
          .addParameter(serializedActivitySpec)
          .returns(TypeName.get(typeUtils.getDeclaredType(
              elementUtils.getTypeElement(Optional.class.getCanonicalName()),
              elementUtils.getTypeElement(Activity.class.getCanonicalName()).asType())))
          .beginControlFlow("if (!$L.getTypeName().equals($L))", serializedActivitySpec.name, activityTypeNameSpec.name)
          .addStatement("return $T.empty()", Optional.class)
          .endControlFlow()
          .addCode("\n")
          .addCode(parameterDeclarationsBlock)
          .addCode("\n")
          .addCode(parameterDeserializeBlock)
          .addCode("\n")
          .addStatement("final var $L = new $T()", activityVarName, activityType)
          .addCode(parameterInjectionBlock)
          .addCode("\n")
          .addStatement("return $T.of($L)", Optional.class, activityVarName)
          .build();
    }

    final MethodSpec serializeActivitySpec;
    {
      final ParameterSpec abstractActivitySpec = ParameterSpec
          .builder(TypeName.get(Activity.class), "abstractActivity", Modifier.FINAL)
          .build();

      final String activityVarName = "activity";
      final String parametersVarName = "parameters";

      final CodeBlock serializeParametersBlock;
      {
        final CodeBlock.Builder blockBuilder = CodeBlock.builder();

        for (final var nameTypePair : parameterInfo.getParameterTypes()) {
          final var parameterName = nameTypePair.getKey();
          final var parameterType = nameTypePair.getValue().getType();

          if (parameterType instanceof PrimitiveType || typeUtils.isSameType(stringType, parameterType)) {
            blockBuilder.addStatement("$L.put($S, $T.of($L.$L))", parametersVarName, parameterName, SerializedParameter.class, activityVarName, parameterName);
          } else {
            // TODO: handle non-primitive parameters
            throw new RuntimeException("Can't handle non-primitive parameters yet");
          }
        }

        serializeParametersBlock = blockBuilder.build();
      }

      serializeActivitySpec = MethodSpec
          .methodBuilder("serializeActivity")
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Override.class)
          .addParameter(abstractActivitySpec)
          .returns(TypeName.get(typeUtils.getDeclaredType(
              elementUtils.getTypeElement(Optional.class.getCanonicalName()),
              elementUtils.getTypeElement(SerializedActivity.class.getCanonicalName()).asType())))
          .beginControlFlow("if (!($L instanceof $T))", abstractActivitySpec.name, activityType)
            .addStatement("return $T.empty()", Optional.class)
          .endControlFlow()
          .addCode("\n")
          .addStatement("final $T $L = ($T)abstractActivity", activityType, activityVarName, activityType)
          .addCode("\n")
          .addStatement("final var $L = new $T<$T, $T>()", parametersVarName, HashMap.class, String.class, SerializedParameter.class)
          .addCode(serializeParametersBlock)
          .addCode("\n")
          .addStatement("return $T.of(new $T($L, $L))", Optional.class, SerializedActivity.class, activityTypeNameSpec.name, parametersVarName)
          .build();
    }

    final TypeSpec activityMapperSpec = TypeSpec
        .classBuilder(activityType.asElement().getSimpleName().toString() + "$$ActivityMapper")
        .addSuperinterface(ActivityMapper.class)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addAnnotation(AnnotationSpec
            .builder(Generated.class)
            .addMember("value", "$S", this.getClass().getCanonicalName())
            .build())
        .addAnnotation(AnnotationSpec
            .builder(gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivitiesMapped.class)
            .addMember("value", "$T.class", activityType)
            .build())
        .addField(activityTypeNameSpec)
        .addMethod(getActivitySchemasSpec)
        .addMethod(deserializeActivitySpec)
        .addMethod(serializeActivitySpec)
        .build();

    final JavaFile javaFile = JavaFile
        .builder(
            elementUtils.getPackageOf(activityType.asElement()).getQualifiedName().toString(),
            activityMapperSpec)
        .build();

    return javaFile;
  }

  private boolean shouldWriteDictionary(
      final Set<? extends TypeElement> annotations,
      final RoundEnvironment roundEnv
  ) {
    // NOTE:
    //   Morally, `useLastRound` should always be true. However, a curious behavior
    //   of OpenJDK 12 is that code produced during the final round of processing
    //   does not influence identifier resolution. Thus, if another file in the
    //   project references the class to be created by this processor, a "cannot
    //   find symbol" error would be produced, even though that class has clearly
    //   just been generated. Emitting the class earlier in the process avoids this,
    //   but is technically incorrect if classes with our annotations are themselves
    //   being generated by another processor. (Such classes would trigger another
    //   round of processing by us, resulting in an attempt to illegally recreate
    //   the helper classes generated in a previous round.)
    //
    //   We default to the morally-correct behavior, since the user can perform a
    //   second compilation (which should succeed due to the code generated the first
    //   time), but allow the users of this processor to override this behavior.
    final boolean useLastRound = Boolean.parseBoolean(
        this.processingEnv.getOptions().getOrDefault("merlin.lastRound", "true"));

    if (useLastRound) {
      return roundEnv.processingOver();
    } else {
      return (!annotations.isEmpty() && !roundEnv.processingOver());
    }
  }

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    // Collect any parameter types produced in the previous round.
    updateParameterTypes(roundEnv);
    // Collect any activity types produced in the previous round.
    updateActivityTypes(roundEnv);

    if (shouldWriteDictionary(annotations, roundEnv)) {
      // TODO: Check that every parameter type references only other parameter types.
      //       This may include primitives like double / Double.

      // TODO: Check that every activity type and parameter type has a default constructor.

      // For each parameter type, generate a mapping between it and the generic SerializedParameter.
      for (final var entry : activityTypes.entrySet()) {
        final JavaFile file = makeActivityMapper(entry.getKey());

        try {
          file.writeTo(this.processingEnv.getFiler());
        } catch (final IOException e) {
          throw new RuntimeException("Unable to open file to generate class `" + file.packageName + "." + file.typeSpec.name + "`");
        }
      }

      // TODO: For all activity types, generate methods mapping its name to the appropriate
      //       parameter type helpers.

      // TODO: Generate a component collecting the whole dependency graph behind a facade.
    }

    return true;
  }
}
