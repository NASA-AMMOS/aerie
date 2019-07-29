package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.util.DocTrees;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;

class TypeInfoMaker {
  private final TypeMirror STRING_TYPE;

  private final DocTrees docTrees;
  private final Types typeUtils;

  public TypeInfoMaker(final ProcessingEnvironment processingEnv) {
    this.docTrees = DocTrees.instance(processingEnv);
    this.typeUtils = processingEnv.getTypeUtils();

    this.STRING_TYPE = processingEnv.getElementUtils().getTypeElement(String.class.getCanonicalName()).asType();
  }

  public ParameterTypeReference getParameterReferenceInfo(final TypeMirror parameterType) {
    final ParameterTypeReference typeReference = new ParameterTypeReference();

    switch (parameterType.getKind()) {
      case DOUBLE:
        typeReference.isPrimitive = true;
        typeReference.typeName = "double";
        break;
      case INT:
        typeReference.isPrimitive = true;
        typeReference.typeName = "int";
        break;
      case DECLARED:
        if (typeUtils.isSameType(STRING_TYPE, parameterType)) {
          typeReference.isPrimitive = true;
          typeReference.typeName = "string";
        } else {
          typeReference.isPrimitive = false;
          typeReference.typeName = ((TypeElement)((DeclaredType)parameterType).asElement()).getQualifiedName().toString();
        }
        break;
      default:
        throw new RuntimeException("Unknown parameter type: " + parameterType.toString());
    }

    return typeReference;
  }

  public ParameterTypeInfo getParameterInfo(final Element typeElement) {
    if (!List.of(ElementKind.CLASS, ElementKind.ENUM).contains(typeElement.getKind())) {
      throw new RuntimeException("A parameter type must be either a class or an enum");
    }

    // TODO: Check that this parameter type has a default constructor.

    final ParameterTypeInfo info = new ParameterTypeInfo();
    info.javaType = (DeclaredType)typeElement.asType();

    // Gather information from the Javadoc for this type.
    final DocCommentTree docTree = this.docTrees.getDocCommentTree(typeElement);
    if (docTree != null) {
      info.briefDescription = docTree.getFirstSentence().toString();
      info.verboseDescription = docTree.getBody().toString();
    }

    // Gather information from the @Parameter fields of this type.
    for (final Element element : typeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.FIELD) {
        continue;
      } else if (element.getAnnotation(Parameter.class) == null) {
        continue;
      }

      // TODO: Check that @Parameter fields are public.

      final String parameterName = element.getSimpleName().toString();
      final ParameterTypeReference parameterTypeRef = this.getParameterReferenceInfo(element.asType());

      info.parameters.add(Pair.of(parameterName, parameterTypeRef));
    }

    return info;
  }

  public ActivityTypeInfo getActivityInfo(final Element typeElement) {
    if (typeElement.getKind() != ElementKind.CLASS) {
      throw new RuntimeException("An activity type must be a class");
    }

    // TODO: Check that this activity type has a default constructor.

    final ActivityTypeInfo info = new ActivityTypeInfo();
    info.javaType = (DeclaredType)typeElement.asType();

    // Gather information from the @ActivityType annotation on this type.
    final ActivityType annotation = typeElement.getAnnotation(ActivityType.class);
    info.name = annotation.value();

    // Gather information from the Javadoc for this type.
    final DocCommentTree docTree = this.docTrees.getDocCommentTree(typeElement);
    if (docTree != null) {
      info.briefDescription = docTree.getFirstSentence().toString();
      info.verboseDescription = docTree.getBody().toString();

      for (final var blockTag : docTree.getBlockTags()) {
        if (blockTag.getKind() != DocTree.Kind.UNKNOWN_BLOCK_TAG) {
          continue;
        }

        final var tag = (UnknownBlockTagTree)blockTag;
        switch (tag.getTagName()) {
          case "subsystem":
            info.subsystem = tag.getContent().toString();
            break;
          case "contact":
            info.contact = tag.getContent().toString();
            break;
        }
      }
    }

    // Gather information from the @Parameter fields of this type.
    for (final Element element : typeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.FIELD) {
        continue;
      } else if (element.getAnnotation(Parameter.class) == null) {
        continue;
      }

      // TODO: Check that @Parameter fields are public.

      final String parameterName = element.getSimpleName().toString();
      final ParameterTypeReference parameterTypeRef = this.getParameterReferenceInfo(element.asType());

      info.parameters.add(Pair.of(parameterName, parameterTypeRef));
    }

    return info;
  }
}
