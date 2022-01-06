package gov.nasa.jpl.aerie.merlin.processor.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.processor.TypePattern;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ParameterRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.SpecificationTypeRecord;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AllDefinedMethodMaker implements MapperMethodMaker {

  @Override
  public MethodSpec makeInstantiateMethod(final SpecificationTypeRecord specType) {
    // Create instantiate Method header
    var methodBuilder = MethodSpec.methodBuilder("instantiate")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(TypeName.get(specType.declaration().asType()))
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
        TypeName.get(specType.declaration().asType()));

    methodBuilder = methodBuilder.addCode(
        specType.parameters()
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
            specType.parameters()
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

    methodBuilder = MapperMethodMaker
        .makeArgumentPresentCheck(methodBuilder, specType).addCode("\n").addStatement("return template");

    return methodBuilder.build();
  }

  @Override
  public MethodSpec makeGetArgumentsMethod(final SpecificationTypeRecord specType) {
    return MethodSpec
        .methodBuilder("getArguments")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(
            java.util.Map.class,
            String.class,
            gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue.class))
        .addParameter(
            TypeName.get(specType.declaration().asType()),
            specType.specificationName(),
            Modifier.FINAL)
        .addStatement(
            "final var $L = new $T()",
            "arguments",
            ParameterizedTypeName.get(
                java.util.HashMap.class,
                String.class,
                gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue.class))
        .addCode(
            specType.parameters()
                .stream()
                .map(parameter -> CodeBlock
                    .builder()
                    .addStatement(
                        "$L.put($S, this.mapper_$L.serializeValue($L.$L))",
                        "arguments",
                        parameter.name,
                        parameter.name,
                        specType.specificationName(),
                        parameter.name
                    ))
                .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                .build())
        .addStatement(
            "return $L",
            "arguments")
        .build();
  }

  @Override
  public List<ParameterRecord> getParameters(final TypeElement activityTypeElement) {
    final var parameters = new ArrayList<ParameterRecord>();
    for (final var element : activityTypeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.FIELD) continue;
      if (element.getAnnotation(ActivityType.Parameter.class) == null) continue;
      final var name = element.getSimpleName().toString();
      final var type = element.asType();
      parameters.add(new ParameterRecord(name, type, element));
    }
    return parameters;
  }
}
