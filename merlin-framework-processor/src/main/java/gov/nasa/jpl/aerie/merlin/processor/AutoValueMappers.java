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
import com.squareup.javapoet.TypeVariableName;
import gov.nasa.jpl.aerie.contrib.serialization.mappers.RecordValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.ValueMapper;
import gov.nasa.jpl.aerie.merlin.framework.annotations.AutoValueMapper;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.MissionModelRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.TypeRule;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class AutoValueMappers {
  private static final Pattern JAVA_IDENTIFIER_ILLEGAL_CHARACTERS = Pattern.compile("[.><,\\[\\]]");

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
                List.of(TypePattern.from(component).box())))
            .toList(),
        generatedClassName,
        ClassName.get((TypeElement) autoValueMapperElement).canonicalName().replace(".", "_"));
  }

  static JavaFile generateAutoValueMappers(final MissionModelRecord missionModel, final Iterable<? extends Element> recordTypes) {
    final var typeName = missionModel.getAutoValueMappersName();

    final var builder =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addAnnotation(
                AnnotationSpec
                    .builder(SuppressWarnings.class)
                    .addMember("value", "$S", "unchecked")
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    record ComponentMapperNamePair(String componentName, String mapperName) {}
    for (final var record : recordTypes) {
      final var methodName = ClassName.get((TypeElement) record).canonicalName().replace(".", "_");
      final var componentToMapperName = new ArrayList<ComponentMapperNamePair>();
      final var necessaryMappers = new HashMap<TypeMirror, String>();
      for (final var element : record.getEnclosedElements()) {
        if (!(element instanceof RecordComponentElement el)) continue;
        final var typeMirror = el.getAccessor().getReturnType();
        final var elementName = element.toString();
        final var valueMapperIdentifier = JAVA_IDENTIFIER_ILLEGAL_CHARACTERS
                                              .matcher(typeMirror.toString())
                                              .replaceAll("_")
                                          + "ValueMapper";
        componentToMapperName.add(new ComponentMapperNamePair(elementName, valueMapperIdentifier));
        necessaryMappers.put(typeMirror, valueMapperIdentifier);
      }

      builder.addMethod(
          MethodSpec
              .methodBuilder(methodName)
              .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
              .addTypeVariables(((TypeElement) record).getTypeParameters().stream().map(TypeVariableName::get).toList())
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
                      .add("return new $T<>($>\n", ClassName.get(RecordValueMapper.class))
                      .add("$L$T.class,\n",
                           // SAFETY: This cast cannot fail - it merely exists to compensate for an inability
                           // to express a MyRecord<T>.class. Instead, we use MyRecord.class, and cast it, via
                           // Object, to a `Class<MyRecord<T>>`. We need to go via Object because Java generics
                           // do not allow casting a Foo<Bar> to a Foo<Bar<Baz>> - since Bar may be a container
                           // that already contains non-Baz objects. In this case, since MyRecord.class is not
                           // a container, the Java type checker is being overly conservative
                           castIfGeneric(record),
                           ClassName.get((TypeElement) record))
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
                                               ClassName.get((TypeElement) record),
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

  private static CodeBlock castIfGeneric(final Element record) {
    final var typeParameters = ((Parameterizable) record).getTypeParameters();
    if (typeParameters.isEmpty()) {
      return CodeBlock.of("");
    } else {
      return CodeBlock.of("(Class<$T>) (Object) ", TypeName.get(record.asType()));
    }
  }
}
