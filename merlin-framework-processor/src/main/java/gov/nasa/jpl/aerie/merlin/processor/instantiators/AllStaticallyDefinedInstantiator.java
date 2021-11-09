package gov.nasa.jpl.aerie.merlin.processor.instantiators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.aerie.merlin.framework.EmptyParameterException;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.processor.TypePattern;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AllStaticallyDefinedInstantiator implements ActivityMapperInstantiator {

  @Override
  public MethodSpec makeInstantiateDefaultMethod(final ActivityTypeRecord activityType) {
    var methodBuilder = MethodSpec.methodBuilder("instantiateDefault")
                                  .addModifiers(Modifier.PUBLIC)
                                  .addAnnotation(Override.class)
                                  .returns(TypeName.get(activityType.declaration.asType()));
    // Exists @Template method
    for (final var element : activityType.declaration.getEnclosedElements()) {
      if (element.getKind() != ElementKind.METHOD && element.getKind() != ElementKind.CONSTRUCTOR) continue;
      if (element.getAnnotation(ActivityType.Template.class) == null) continue;
      var templateName = element.getSimpleName().toString();
      methodBuilder = methodBuilder.addStatement(
          "return $T.$N()",
          TypeName.get(activityType.declaration.asType()),
          templateName);
      break;
    }

    return methodBuilder.build();
  }

  @Override
  public MethodSpec makeInstantiateMethod(final ActivityTypeRecord activityType) {
    var activityTypeName = activityType.declaration.getSimpleName().toString();

    var methodBuilder = MethodSpec.methodBuilder("instantiate")
                                  .addModifiers(Modifier.PUBLIC)
                                  .addAnnotation(Override.class)
                                  .returns(TypeName.get(activityType.declaration.asType()))
                                  .addException(gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.UnconstructableTaskSpecException.class)
                                  .addParameter(
                                      ParameterizedTypeName.get(
                                          java.util.Map.class,
                                          String.class,
                                          gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue.class),
                                      "arguments",
                                      Modifier.FINAL);

    for (final var element : activityType.declaration.getEnclosedElements()) {
      if (element.getKind() != ElementKind.METHOD && element.getKind() != ElementKind.CONSTRUCTOR) continue;
      if (element.getAnnotation(ActivityType.Template.class) == null) continue;
      var templateName = element.getSimpleName().toString();
      methodBuilder = methodBuilder.addStatement("final var template = $L.$L()", activityTypeName, templateName);

      methodBuilder = methodBuilder.addCode(
          activityType.parameters
              .stream()
              .map(parameter -> CodeBlock
                  .builder()
                  .addStatement(
                      "$T $L = $T$L",
                      new TypePattern.ClassPattern(
                          ClassName.get(Optional.class),
                          List.of(TypePattern.from(parameter.type))).render(),
                      parameter.name,
                      Optional.class,
                      ".ofNullable(template." + parameter.name + "())"
                  )
              )
              .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
              .build()).addCode("\n");

        methodBuilder = methodBuilder.beginControlFlow("for (final var $L : $L.entrySet())", "entry", "arguments")
            .beginControlFlow("switch ($L.getKey())", "entry")
            .addCode(
                activityType.parameters
                    .stream()
                    .map(parameter -> CodeBlock
                        .builder()
                        .add("case $S:\n", parameter.name)
                        .indent()
                        .addStatement(
                            "$L = $L(this.mapper_$L.deserializeValue($L.getValue()).getSuccessOrThrow($$ -> new $T()))",
                            parameter.name,
                            "Optional.ofNullable",
                            parameter.name,
                            "entry",
                            gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.UnconstructableTaskSpecException.class)
                        .addStatement("break")
                        .unindent())
                    .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                    .build())
            .addCode(
                CodeBlock
                    .builder()
                    .add("default:\n")
                    .indent()
                    .addStatement(
                        "throw new $T()",
                        gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.UnconstructableTaskSpecException.class)
                    .unindent()
                    .build())
            .endControlFlow()
            .endControlFlow().addCode("\n");
      break;
    }

    // Ensure all parameters are non-null
    methodBuilder = methodBuilder.addCode(
        activityType.parameters
            .stream()
            .map(parameter -> CodeBlock
                .builder()
                .addStatement(
                    "if (!$L.isPresent()) throw new $T()",
                    parameter.name,
                    EmptyParameterException.class)
            ).reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
            .build()
    ).addCode("\n");

    // Add return statement with instantiation of class with parameters
    methodBuilder = methodBuilder.addStatement(
        "return new $T($L)",
        activityType.declaration,
        activityType.parameters.stream().map(
            parameter -> parameter.name + ".get()").collect(Collectors.joining(", ")));

    return methodBuilder.build();
  }
}
