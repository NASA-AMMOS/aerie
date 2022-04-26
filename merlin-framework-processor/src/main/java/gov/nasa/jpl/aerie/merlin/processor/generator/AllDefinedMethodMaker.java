package gov.nasa.jpl.aerie.merlin.processor.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.aerie.merlin.processor.TypePattern;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ExportTypeRecord;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentsException;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;

/** Method maker for defaults style where all default arguments are provided within @Parameter annotations. */
/*package-private*/ final class AllDefinedMethodMaker extends MapperMethodMaker {

  public AllDefinedMethodMaker(final ExportTypeRecord exportType) {
    super(exportType);
  }

  @Override
  public MethodSpec makeInstantiateMethod() {
    // Create instantiate Method header
    var methodBuilder = MethodSpec.methodBuilder("instantiate")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(TypeName.get(exportType.declaration().asType()))
        .addException(unconstructableInstantiateException)
        .addException(MissingArgumentsException.class)
        .addParameter(
            ParameterizedTypeName.get(
                java.util.Map.class,
                String.class,
                gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue.class),
            "arguments",
            Modifier.FINAL);

    methodBuilder = methodBuilder.addStatement(
        "final var template = new $T()",
        TypeName.get(exportType.declaration().asType()));

    methodBuilder = methodBuilder.addCode(
        exportType.parameters()
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
            exportType.parameters()
                .stream()
                .map(parameter -> CodeBlock
                    .builder()
                    .add("case $S:\n", parameter.name)
                    .indent()
                    .addStatement(
                        "template.$L = this.mapper_$L.deserializeValue($L.getValue()).getSuccessOrThrow(failure -> $T.unconstructableArgument(\"$L\", failure))",
                        parameter.name,
                        parameter.name,
                        "entry",
                        unconstructableInstantiateException,
                        parameter.name)
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
                    "throw $T.nonexistentArgument($L.getKey())",
                    unconstructableInstantiateException,
                    "entry")
                .unindent()
                .build())
        .endControlFlow()
        .endControlFlow().addCode("\n");

    methodBuilder = makeArgumentsPresentCheck(methodBuilder).addCode("\n").addStatement("return template");

    return methodBuilder.build();
  }

  @Override
  public MethodSpec makeGetArgumentsMethod() {
    return MethodSpec
        .methodBuilder("getArguments")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(
            java.util.Map.class,
            String.class,
            gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue.class))
        .addParameter(
            TypeName.get(exportType.declaration().asType()),
            metaName,
            Modifier.FINAL)
        .addStatement(
            "final var $L = new $T()",
            "arguments",
            ParameterizedTypeName.get(
                java.util.HashMap.class,
                String.class,
                gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue.class))
        .addCode(
            exportType.parameters()
                .stream()
                .map(parameter -> CodeBlock
                    .builder()
                    .addStatement(
                        "$L.put($S, this.mapper_$L.serializeValue($L.$L))",
                        "arguments",
                        parameter.name,
                        parameter.name,
                        metaName,
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
