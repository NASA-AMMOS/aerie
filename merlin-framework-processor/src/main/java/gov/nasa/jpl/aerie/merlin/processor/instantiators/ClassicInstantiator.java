package gov.nasa.jpl.aerie.merlin.processor.instantiators;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.aerie.merlin.framework.EmptyParameterException;
import gov.nasa.jpl.aerie.merlin.processor.TypePattern;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;

public class ClassicInstantiator implements ActivityMapperInstantiator {

  @Override
  public MethodSpec makeInstantiateDefaultMethod(final ActivityTypeRecord activityType) {
    return MethodSpec.methodBuilder("instantiateDefault")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(TypeName.get(activityType.declaration.asType()))
        .addStatement("return new $T()", TypeName.get(activityType.declaration.asType()))
        .build();
  }

  @Override
  public MethodSpec makeInstantiateMethod(final ActivityTypeRecord activityType) {
    // Create instantiate Method header
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

    methodBuilder = methodBuilder.addStatement(
        "final var template = new $T()",
        TypeName.get(activityType.declaration.asType()));

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
                    ".ofNullable(template." + parameter.name + ")"
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
                        "template.$L = this.mapper_$L.deserializeValue($L.getValue()).getSuccessOrThrow($$ -> new $T())",
                        parameter.name,
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
    ).addCode("\n").addStatement("return template");

    return methodBuilder.build();
  }

  @Override
  public MethodSpec makeGetArgumentsMethod(final ActivityTypeRecord activityType) {
    return MethodSpec
        .methodBuilder("getArguments")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(
            java.util.Map.class,
            String.class,
            gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue.class))
        .addParameter(
            TypeName.get(activityType.declaration.asType()),
            "activity",
            Modifier.FINAL)
        .addStatement(
            "final var $L = new $T()",
            "arguments",
            ParameterizedTypeName.get(
                java.util.HashMap.class,
                String.class,
                gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue.class))
        .addCode(
            activityType.parameters
                .stream()
                .map(parameter -> CodeBlock
                    .builder()
                    .addStatement(
                        "$L.put($S, this.mapper_$L.serializeValue($L.$L))",
                        "arguments",
                        parameter.name,
                        parameter.name,
                        "activity",
                        parameter.name
                    ))
                .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                .build())
        .addStatement(
            "return $L",
            "arguments")
        .build();
  }
}
