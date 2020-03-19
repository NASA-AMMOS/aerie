package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.util.DocTrees;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Map;

class ParameterTypeException extends Exception {
  private final Element relatedElement;

  public ParameterTypeException(final String message, final Element relatedElement) {
    super(message);
    this.relatedElement = relatedElement;
  }

  public Element getRelatedElement() {
    return this.relatedElement;
  }
}

class TypeInfoMaker {
  private final TypeMirror DOUBLE_TYPE;
  private final TypeMirror FLOAT_TYPE;
  private final TypeMirror BYTE_TYPE;
  private final TypeMirror SHORT_TYPE;
  private final TypeMirror INTEGER_TYPE;
  private final TypeMirror LONG_TYPE;
  private final TypeMirror BOOLEAN_TYPE;
  private final TypeMirror CHAR_TYPE;
  private final TypeMirror STRING_TYPE;
  private final TypeMirror LIST_TYPE;
  private final TypeMirror MAP_TYPE;

  private final DocTrees docTrees;
  private final Types typeUtils;
  private final Messager messager;

  public TypeInfoMaker(final ProcessingEnvironment processingEnv) {
    this.docTrees = DocTrees.instance(processingEnv);
    this.typeUtils = processingEnv.getTypeUtils();
    this.messager = processingEnv.getMessager();

    this.DOUBLE_TYPE = processingEnv.getElementUtils().getTypeElement(Double.class.getCanonicalName()).asType();
    this.FLOAT_TYPE = processingEnv.getElementUtils().getTypeElement(Float.class.getCanonicalName()).asType();
    this.BYTE_TYPE = processingEnv.getElementUtils().getTypeElement(Byte.class.getCanonicalName()).asType();
    this.SHORT_TYPE = processingEnv.getElementUtils().getTypeElement(Short.class.getCanonicalName()).asType();
    this.INTEGER_TYPE = processingEnv.getElementUtils().getTypeElement(Integer.class.getCanonicalName()).asType();
    this.LONG_TYPE = processingEnv.getElementUtils().getTypeElement(Long.class.getCanonicalName()).asType();
    this.BOOLEAN_TYPE = processingEnv.getElementUtils().getTypeElement(Boolean.class.getCanonicalName()).asType();
    this.CHAR_TYPE = processingEnv.getElementUtils().getTypeElement(Character.class.getCanonicalName()).asType();
    this.STRING_TYPE = processingEnv.getElementUtils().getTypeElement(String.class.getCanonicalName()).asType();
    this.LIST_TYPE = processingEnv.getElementUtils().getTypeElement(List.class.getCanonicalName()).asType();
    this.MAP_TYPE = processingEnv.getElementUtils().getTypeElement(Map.class.getCanonicalName()).asType();
  }

  public ParameterTypeReference getParameterTypeReference(final Element parameterElement) throws ParameterTypeException {
    return getParameterTypeReference(parameterElement, parameterElement.asType());
  }

  private ParameterTypeReference getParameterTypeReference(final Element declaration, final TypeMirror parameterType) throws ParameterTypeException {
    switch (parameterType.getKind()) {
      case DOUBLE: return ParameterTypeReference.DOUBLE;
      case FLOAT: return ParameterTypeReference.FLOAT;
      case BYTE: return ParameterTypeReference.BYTE;
      case SHORT: return ParameterTypeReference.SHORT;
      case INT: return ParameterTypeReference.INT;
      case LONG: return ParameterTypeReference.LONG;
      case BOOLEAN: return ParameterTypeReference.BOOLEAN;
      case CHAR: return ParameterTypeReference.CHAR;
      case DECLARED:
        if (typeUtils.isSameType(STRING_TYPE, parameterType)) {
          return ParameterTypeReference.STRING;
        } else if (typeUtils.isSameType(DOUBLE_TYPE, parameterType)) {
          return ParameterTypeReference.DOUBLE;
        } else if (typeUtils.isSameType(FLOAT_TYPE, parameterType)) {
          return ParameterTypeReference.FLOAT;
        } else if (typeUtils.isSameType(BYTE_TYPE, parameterType)) {
          return ParameterTypeReference.BYTE;
        } else if (typeUtils.isSameType(SHORT_TYPE, parameterType)) {
          return ParameterTypeReference.SHORT;
        } else if (typeUtils.isSameType(INTEGER_TYPE, parameterType)) {
          return ParameterTypeReference.INT;
        } else if (typeUtils.isSameType(LONG_TYPE, parameterType)) {
          return ParameterTypeReference.LONG;
        } else if (typeUtils.isSameType(BOOLEAN_TYPE, parameterType)) {
          return ParameterTypeReference.BOOLEAN;
        } else if (typeUtils.isSameType(CHAR_TYPE, parameterType)) {
          return ParameterTypeReference.CHAR;
        } else if (typeUtils.isAssignable(((DeclaredType)parameterType).asElement().asType(), LIST_TYPE)) {
          return ParameterTypeReference.ofList(
              getParameterTypeReference(declaration, ((DeclaredType)parameterType).getTypeArguments().get(0)));
        } else if (typeUtils.isAssignable(((DeclaredType)parameterType).asElement().asType(), MAP_TYPE)) {
          return ParameterTypeReference.ofMap(
              getParameterTypeReference(declaration, ((DeclaredType)parameterType).getTypeArguments().get(0)),
              getParameterTypeReference(declaration, ((DeclaredType)parameterType).getTypeArguments().get(1)));
        } else {
          throw new ParameterTypeException("Unknown parameter type: " + parameterType.toString(), declaration);
        }
      case ARRAY:
        return ParameterTypeReference.ofArray(
            getParameterTypeReference(declaration, ((ArrayType)parameterType).getComponentType()));
      default:
        throw new ParameterTypeException("Unknown parameter type: " + parameterType.toString(), declaration);
    }
  }

  public ParameterTypeInfo getParameterInfo(final Element typeElement) throws ParameterTypeException {
    if (!List.of(ElementKind.CLASS, ElementKind.ENUM).contains(typeElement.getKind())) {
      throw new ParameterTypeException("A parameter type must be either a class or an enum", typeElement);
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
      final ParameterTypeReference parameterTypeRef = this.getParameterTypeReference(element);

      info.parameters.add(Pair.of(parameterName, parameterTypeRef));
    }

    return info;
  }

  public ActivityTypeInfo getActivityInfo(final Element typeElement) throws ParameterTypeException {
    if (typeElement.getKind() != ElementKind.CLASS) {
      throw new ParameterTypeException("An activity type must be a class", typeElement);
    }

    // TODO: Check that this activity type has a default constructor.

    final ActivityTypeInfo info = new ActivityTypeInfo();
    info.javaType = (DeclaredType)typeElement.asType();

    // Gather information from the @ActivityType annotation on this type.
    final ActivityType annotation = typeElement.getAnnotation(ActivityType.class);
    info.name = annotation.name();

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
      final ParameterTypeReference parameterTypeRef = this.getParameterTypeReference(element);

      info.parameters.add(Pair.of(parameterName, parameterTypeRef));
    }

    return info;
  }
}
