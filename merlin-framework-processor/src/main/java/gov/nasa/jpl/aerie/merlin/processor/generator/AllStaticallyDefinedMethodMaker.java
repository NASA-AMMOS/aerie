package gov.nasa.jpl.aerie.merlin.processor.generator;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Export;
import gov.nasa.jpl.aerie.merlin.processor.TypePattern;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ExportTypeRecord;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentsException;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Method maker for defaults style where all default arguments are provided within a @Template static method. */
/*package-private*/ final class AllStaticallyDefinedMethodMaker extends MapperMethodMaker {

  public AllStaticallyDefinedMethodMaker(final ExportTypeRecord exportType) {
    super(exportType);
  }

  @Override
  public MethodSpec makeInstantiateMethod() {
    final var activityTypeName = exportType.declaration().getSimpleName().toString();

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

    for (final var element : exportType.declaration().getEnclosedElements()) {
      if (element.getKind() != ElementKind.METHOD && element.getKind() != ElementKind.CONSTRUCTOR) continue;
      if (element.getAnnotation(Export.Template.class) == null) continue;
      var templateName = element.getSimpleName().toString();
      methodBuilder = methodBuilder.addStatement("final var template = $L.$L()", activityTypeName, templateName);

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
                      ".ofNullable(template." + parameter.name + "())"
                  )
              )
              .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
              .build()).addCode("\n");

        methodBuilder = makeArgumentAssignments(methodBuilder, (builder, parameter) -> builder
            .addStatement(
                "$L = $L(this.mapper_$L.deserializeValue($L.getValue()).getSuccessOrThrow(failure -> $T.unconstructableArgument(\"$L\", failure)))",
                parameter.name,
                "Optional.ofNullable",
                parameter.name,
                "entry",
                unconstructableInstantiateException,
                parameter.name));
      break;
    }

    // Add return statement with instantiation of class with parameters
    methodBuilder = methodBuilder.addStatement(
        "return new $T($L)",
        exportType.declaration(),
        exportType.parameters().stream().map(
            parameter -> parameter.name + ".get()").collect(Collectors.joining(", ")));

    return methodBuilder.build();
  }
}
