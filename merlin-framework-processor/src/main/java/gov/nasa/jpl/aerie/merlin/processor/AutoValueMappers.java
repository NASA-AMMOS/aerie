package gov.nasa.jpl.aerie.merlin.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.RecordValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.MissionModelRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.TypeRule;
import org.apache.commons.lang3.tuple.Pair;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AutoValueMappers {
  static TypeRule typeRule(final Element autoValueMapperElement, final ClassName generatedClassName) throws InvalidMissionModelException {
    if (!autoValueMapperElement.getKind().equals(ElementKind.RECORD)) {
      throw new InvalidMissionModelException(
          "@%s.%s is only allowed on records".formatted(
              AutoValueMapper.class.getSimpleName(),
              AutoValueMapper.Record.class.getSimpleName()),
          autoValueMapperElement);
    }

    final var typeMirrors = new HashSet<TypeMirror>();
    for (final var enclosedElement : autoValueMapperElement.getEnclosedElements()) {
      if (!(enclosedElement instanceof RecordComponentElement el)) continue;
      final var typeMirror = el.getAccessor().getReturnType();
      typeMirrors.add(typeMirror);
    }

    return new TypeRule(
        new TypePattern.ClassPattern(
            ClassName.get(ValueMapper.class),
            List.of(TypePattern.from(autoValueMapperElement.asType()))),
        Set.of(),
        typeMirrors
            .stream()
            .map(component -> (TypePattern) new TypePattern.ClassPattern(
                ClassName.get(ValueMapper.class),
                List.of(new TypePattern.ClassPattern((ClassName) ClassName.get(component).box(), List.of()))))
            .toList(),
        generatedClassName,
        ClassName.get((TypeElement) autoValueMapperElement).canonicalName().replace(".", "_"));
  }

  static JavaFile generateAutoValueMappers(final MissionModelRecord missionModel, final Iterable<TypeElement> recordTypes) {
    final var typeName = missionModel.getAutoValueMappersName();

    final var builder =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    record ComponentMapperNamePair(String componentName, String mapperName) {}
    for (final var record : recordTypes) {
      final var methodName = ClassName.get(record).canonicalName().replace(".", "_");
      final var componentToMapperName = new ArrayList<ComponentMapperNamePair>();
      final var necessaryMappers = new HashMap<TypeMirror, String>();
      for (final var element : record.getEnclosedElements()) {
        if (!(element instanceof RecordComponentElement el)) continue;
        final var typeMirror = el.getAccessor().getReturnType();
        final var elementName = element.toString();
        final var valueMapperIdentifier = typeMirror.toString().replace(".", "_") + "ValueMapper";
        componentToMapperName.add(new ComponentMapperNamePair(elementName, valueMapperIdentifier));
        necessaryMappers.put(typeMirror, valueMapperIdentifier);
      }

      builder.addMethod(
          MethodSpec
              .methodBuilder(methodName)
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
              .returns(ParameterizedTypeName.get(ClassName.get(ValueMapper.class), TypeName.get(record.asType())))
              .addParameters(
                  necessaryMappers
                      .entrySet()
                      .stream()
                      .map(mapperRequest -> ParameterSpec
                          .builder(
                              ParameterizedTypeName.get(
                                  ClassName.get(ValueMapper.class),
                                  ClassName.get(mapperRequest.getKey()).box()),
                              mapperRequest.getValue(),
                              Modifier.FINAL)
                          .build())
                      .toList())
              .addCode(
                  CodeBlock
                      .builder()
                      .add("return new $T<>($>\n", RecordValueMapper.class)
                      .add("$T.class,\n", ClassName.get(record))
                      .add("$T.of($>\n", List.class)
                      .add(CodeBlock.join(
                          componentToMapperName
                              .stream()
                              .map(recordComponent ->
                                       CodeBlock
                                           .builder()
                                           .add(
                                               "new $T<>($>\n" + "$S,\n" + "$T::$L,\n" + "$L" + "$<)",
                                               RecordValueMapper.Component.class,
                                               recordComponent.componentName(),
                                               ClassName.get(record),
                                               recordComponent.componentName(),
                                               recordComponent.mapperName())
                                           .build())
                              .toList(),
                          ",\n"))
                      .add("$<)$<);")
                      .build())
              .build());
    }

    return JavaFile
        .builder(typeName.packageName(), builder.build())
        .skipJavaLangImports(true)
        .build();
  }
}
