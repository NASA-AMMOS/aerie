package gov.nasa.jpl.aerie.merlin.processor.generator;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ExportDefaultsStyle;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ExportTypeRecord;
import gov.nasa.jpl.aerie.merlin.protocol.model.ConfigurationType;
import gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType;
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

  MethodSpec makeInstantiateMethod(final ExportTypeRecord exportType);

  default List<String> getParametersWithDefaults(final ExportTypeRecord exportType) {
    return exportType.parameters().stream().map(p -> p.name).toList();
  }

  default MethodSpec makeGetRequiredParametersMethod(final ExportTypeRecord exportType) {
    final var optionalParams = getParametersWithDefaults(exportType);
    final var requiredParams = exportType.parameters().stream().filter(p -> !optionalParams.contains(p.name)).toList();

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

  default MethodSpec makeGetParametersMethod(final ExportTypeRecord exportType) {
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
            exportType.parameters()
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

  default MethodSpec makeGetArgumentsMethod(final ExportTypeRecord exportType) {
    final var metaName = getMetaName(exportType);
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
                        "$L.put($S, this.mapper_$L.serializeValue($L.$L()))",
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

  default MethodSpec makeGetValidationFailures(final ExportTypeRecord exportType) {
    final var metaName = getMetaName(exportType);
    return MethodSpec
        .methodBuilder("getValidationFailures")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(
            java.util.List.class,
            String.class))
        .addParameter(
            TypeName.get(exportType.declaration().asType()),
            metaName,
            Modifier.FINAL)
        .addStatement(
            "final var $L = new $T()",
            "failures",
            ParameterizedTypeName.get(
                java.util.ArrayList.class,
                String.class))
        .addCode(
            exportType.validations()
                .stream()
                .map(validation -> CodeBlock
                    .builder()
                    .addStatement(
                        "if (!$L.$L()) failures.add($S)",
                        metaName,
                        validation.methodName,
                        validation.failureMessage))
                .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                .build())
        .addStatement(
            "return $L",
            "failures")
        .build();
  }

  static MethodSpec.Builder makeArgumentPresentCheck(final MethodSpec.Builder methodBuilder, final ExportTypeRecord exportType) {
    // Ensure all parameters are non-null
    return methodBuilder.addCode(
        exportType.parameters()
            .stream()
            .map(parameter -> CodeBlock
                .builder()
                .addStatement(
                    "if (!$L.isPresent()) throw new $T(\"$L\", \"$L\", \"$L\", this.mapper_$L.getValueSchema())",
                    parameter.name,
                    MissingArgumentException.class,
                    getMetaName(exportType),
                    exportType.name(),
                    parameter.name,
                    parameter.name))
            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
            .build());
  }

  static String getMetaName(final ExportTypeRecord exportType) {
    // TODO currently only 2 permitted classes (activity and config. type records),
    //  this should be changed to a switch expression once sealed class pattern-matching switch expressions exist
    if (exportType instanceof ActivityTypeRecord) {
      return "activity";
    } else { // is instanceof ConfigurationTypeRecord
      return "configuration";
    }
  }

  static Class<?> getInstantiateException(final ExportTypeRecord exportType) {
    // TODO currently only 2 permitted classes (activity and config. type records),
    //  this should be changed to a switch expression once sealed class pattern-matching switch expressions exist
    if (exportType instanceof ActivityTypeRecord) {
      return TaskSpecType.UnconstructableTaskSpecException.class;
    } else { // is instanceof ConfigurationTypeRecord
      return ConfigurationType.UnconstructableConfigurationException.class;
    }
  }

  static MapperMethodMaker make(final ExportDefaultsStyle style) {
    return switch (style) {
      case AllStaticallyDefined -> new AllStaticallyDefinedMethodMaker();
      case NoneDefined -> new NoneDefinedMethodMaker();
      case AllDefined -> new AllDefinedMethodMaker();
      case SomeStaticallyDefined -> new SomeStaticallyDefinedMethodMaker();
    };
  }
}
