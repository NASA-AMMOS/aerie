package gov.nasa.jpl.aerie.merlin.processor.generator;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityDefaultsStyle;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.SpecificationTypeRecord;
import gov.nasa.jpl.aerie.merlin.protocol.types.MissingArgumentException;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ParameterRecord;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface MapperMethodMaker {

  MethodSpec makeInstantiateMethod(final SpecificationTypeRecord specType);

  default List<String> getParametersWithDefaults(final SpecificationTypeRecord specType) {
    return specType.parameters().stream().map(p -> p.name).toList();
  }

  default MethodSpec makeGetRequiredParametersMethod(final SpecificationTypeRecord specType) {
    final var optionalParams = getParametersWithDefaults(specType);
    final var requiredParams = specType.parameters().stream().filter(p -> !optionalParams.contains(p.name)).toList();

    return MethodSpec.methodBuilder("getRequiredParameters")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(
            java.util.List.class,
            String.class))
        .addStatement(
            "return $T.of($L)",
            List.class,
            requiredParams.stream().map(p -> "\"%s\"".formatted(p.name)).collect(Collectors.joining(", ")))
        .build();
  }

  default MethodSpec makeGetParametersMethod(final SpecificationTypeRecord specType) {
    return MethodSpec.methodBuilder("getParameters")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(
            java.util.ArrayList.class,
            Parameter.class))
        .addStatement(
            "final var $L = new $T()",
            "parameters",
            ParameterizedTypeName.get(
                java.util.ArrayList.class,
                Parameter.class))
        .addCode(
            specType.parameters()
                .stream()
                .map(parameter -> CodeBlock
                    .builder()
                    .addStatement(
                        "$L.add(new $T($S, this.mapper_$L.getValueSchema()))",
                        "parameters",
                        Parameter.class,
                        parameter.name,
                        parameter.name))
                .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                .build())
        .addStatement(
            "return $L",
            "parameters")
        .build();
  }

  default MethodSpec makeGetArgumentsMethod(final SpecificationTypeRecord specType) {
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
                        "$L.put($S, this.mapper_$L.serializeValue($L.$L()))",
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

  default List<ParameterRecord> getParameters(final TypeElement activityTypeElement)
  {
    final var parameters = new ArrayList<ParameterRecord>();
    for (final var element : activityTypeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.FIELD) continue;
      if (element.getModifiers().contains(Modifier.STATIC)) continue;
      final var name = element.getSimpleName().toString();
      final var type = element.asType();
      parameters.add(new ParameterRecord(name, type, element));
    }
    return parameters;
  }

  static MethodSpec.Builder makeArgumentPresentCheck(final MethodSpec.Builder methodBuilder, final SpecificationTypeRecord specType) {
    // Ensure all parameters are non-null
    return methodBuilder.addCode(
        specType.parameters()
            .stream()
            .map(parameter -> CodeBlock
                .builder()
                .addStatement(
                    "if (!$L.isPresent()) throw new $T(\"$L\", \"$L\", this.mapper_$L.getValueSchema())",
                    parameter.name,
                    MissingArgumentException.class,
                    specType.name(),
                    parameter.name,
                    parameter.name))
            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
            .build());
  }

  static MapperMethodMaker make(final ActivityDefaultsStyle style) {
    return switch (style) {
      case AllStaticallyDefined -> new AllStaticallyDefinedMethodMaker();
      case NoneDefined -> new NoneDefinedMethodMaker();
      case AllDefined -> new AllDefinedMethodMaker();
      case SomeStaticallyDefined -> new SomeStaticallyDefinedMethodMaker();
    };
  }
}
