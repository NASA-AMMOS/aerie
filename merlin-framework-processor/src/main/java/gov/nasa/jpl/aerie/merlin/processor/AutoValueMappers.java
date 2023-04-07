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
import gov.nasa.jpl.aerie.merlin.processor.metamodel.MissionModelRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.TypeRule;
import org.apache.commons.lang3.tuple.Pair;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class AutoValueMappers {
  static Pair<JavaFile, List<TypeRule>> generateAutoValueMappers(
      final MissionModelRecord missionModel,
      final Iterable<TypeElement> recordTypes) {
    final var typeRules = new ArrayList<TypeRule>();

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

      final var typeRule = new TypeRule(
          new TypePattern.ClassPattern(ClassName.get(ValueMapper.class), List.of(TypePattern.from(record.asType()))),
          Set.of(),
          necessaryMappers
              .keySet()
              .stream()
              .map(component -> (TypePattern) new TypePattern.ClassPattern(
                  ClassName.get(ValueMapper.class),
                  List.of(new TypePattern.ClassPattern((ClassName) ClassName.get(component).box(), List.of()))))
              .toList(),
          typeName,
          methodName);
      typeRules.add(typeRule);

      final var methodBuilder = MethodSpec
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
                  .toList());

      final var codeBlockBuilder = CodeBlock.builder();
      codeBlockBuilder.add("return new $T<>(\n", RecordValueMapper.class);
      codeBlockBuilder.add("  $T.class,\n", ClassName.get(record));
      codeBlockBuilder.add("  $T.of(\n", List.class);

      codeBlockBuilder.add(
          CodeBlock.join(componentToMapperName
                             .stream()
                             .map(recordComponent ->
                                      CodeBlock
                                          .builder()
                                          .add("    new $T<>(\n", RecordValueMapper.Component.class)
                                          .add("      $S,\n", recordComponent.componentName())
                                          .add("      $T::$L,\n", ClassName.get(record), recordComponent.componentName())
                                          .add("    $L)", recordComponent.mapperName())
                                          .build())
                             .toList(), ",\n"));
      codeBlockBuilder.add("));");

      methodBuilder.addCode(codeBlockBuilder.build());
      builder.addMethod(methodBuilder.build());
    }

    return Pair.of(JavaFile
                       .builder(typeName.packageName(), builder.build())
                       .skipJavaLangImports(true)
                       .build(), typeRules);
  }
}
