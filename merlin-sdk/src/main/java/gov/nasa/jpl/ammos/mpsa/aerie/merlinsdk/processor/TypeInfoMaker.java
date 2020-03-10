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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.List;

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
  private final TypeMirror STRING_TYPE;
  private final TypeMirror INTEGER_TYPE;
  private final TypeMirror LIST_TYPE;

  private final DocTrees docTrees;
  private final Types typeUtils;
  private final Messager messager;

  public TypeInfoMaker(final ProcessingEnvironment processingEnv) {
    this.docTrees = DocTrees.instance(processingEnv);
    this.typeUtils = processingEnv.getTypeUtils();
    this.messager = processingEnv.getMessager();

    this.STRING_TYPE = processingEnv.getElementUtils().getTypeElement(String.class.getCanonicalName()).asType();
    this.INTEGER_TYPE = processingEnv.getElementUtils().getTypeElement(Integer.class.getCanonicalName()).asType();
    this.LIST_TYPE = processingEnv.getElementUtils().getTypeElement(List.class.getCanonicalName()).asType();
  }

  public ParameterTypeReference getParameterReferenceInfo(final Element parameterElement) throws ParameterTypeException {
    final ParameterTypeReference typeReference = new ParameterTypeReference();

    final TypeMirror parameterType = parameterElement.asType();
    switch (parameterType.getKind()) {
      case DOUBLE:
        typeReference.isPrimitive = true;
        typeReference.typeName = "double";
        break;
      case FLOAT:
        typeReference.isPrimitive = true;
        typeReference.typeName = "float";
        break;
      case BYTE:
        typeReference.isPrimitive = true;
        typeReference.typeName = "byte";
        break;
      case SHORT:
        typeReference.isPrimitive = true;
        typeReference.typeName = "short";
        break;
      case INT:
        typeReference.isPrimitive = true;
        typeReference.typeName = "int";
        break;
      case LONG:
        typeReference.isPrimitive = true;
        typeReference.typeName = "long";
        break;
      case BOOLEAN:
        typeReference.isPrimitive = true;
        typeReference.typeName = "boolean";
        break;
      case CHAR:
        typeReference.isPrimitive = true;
        typeReference.typeName = "char";
        break;
      case DECLARED:
        if (typeUtils.isSameType(STRING_TYPE, parameterType)) {
          typeReference.isPrimitive = true;
          typeReference.typeName = "string";
        } else if (typeUtils.isAssignable(((DeclaredType)parameterType).asElement().asType(), LIST_TYPE)) {
          typeReference.isPrimitive = true;
          typeReference.typeName = getListTypeString((DeclaredType)parameterType, parameterElement);
        } else {
          typeReference.isPrimitive = false;
          typeReference.typeName = ((TypeElement)((DeclaredType)parameterType).asElement()).getQualifiedName().toString();
        }
        break;
      case ARRAY:
        typeReference.isPrimitive = true;
        typeReference.typeName = getArrayTypeString((ArrayType)parameterType, parameterElement);
        break;
      default:
        throw new ParameterTypeException("Unknown parameter type: " + parameterType.toString(), parameterElement);
    }

    return typeReference;
  }

  // Write more of these for List and Map then instead of a loop, use recursion among the three functions
  private String getListTypeString(DeclaredType providedType, Element parameterElement) throws ParameterTypeException {
    String typeName = "list_";

    TypeMirror componentType = providedType.getTypeArguments().get(0);
    String type = componentType.getKind().name();
    String actualType;
    switch(type) {
      case "DOUBLE":
      case "FLOAT":
      case "BYTE":
      case "SHORT":
      case "INT":
      case "LONG":
      case "BOOLEAN":
      case "CHAR":
      case "ARRAY":
        actualType = getArrayTypeString((ArrayType)componentType, parameterElement);
        break;
      case "DECLARED":
        if (typeUtils.isSameType(STRING_TYPE, componentType)) {
          actualType = "string";
        } else if (typeUtils.isSameType(INTEGER_TYPE, componentType)) {
          actualType = "int";
        } else if (typeUtils.isAssignable(((DeclaredType)componentType).asElement().asType(), LIST_TYPE)) {
          actualType = getListTypeString((DeclaredType)componentType, parameterElement);
        } else {
          actualType = ((TypeElement)((DeclaredType)componentType).asElement()).getQualifiedName().toString();
        }
        break;
      default:
        throw new ParameterTypeException("Unknown parameter type: " + componentType.toString(), parameterElement);
    }

    return typeName + actualType;
  }

  private String getArrayTypeString(ArrayType providedType, Element parameterElement) throws ParameterTypeException {
    String typeName = "array_";

    TypeMirror componentType = providedType.getComponentType();
    String type = componentType.getKind().name();
    String actualType;
    switch(type) {
      case "DOUBLE":
      case "FLOAT":
      case "BYTE":
      case "SHORT":
      case "INT":
      case "LONG":
      case "BOOLEAN":
      case "CHAR":
      case "ARRAY":
        actualType = getArrayTypeString((ArrayType)componentType, parameterElement);
        break;
      case "DECLARED":
        if (typeUtils.isSameType(STRING_TYPE, componentType)) {
          actualType = "string";
        } else if (typeUtils.isSameType(INTEGER_TYPE, componentType)) {
          actualType = "int";
        } else if (typeUtils.isAssignable(((DeclaredType)componentType).asElement().asType(), LIST_TYPE)) {
          actualType = getListTypeString((DeclaredType)componentType, parameterElement);
        } else {
          actualType = ((TypeElement)((DeclaredType)componentType).asElement()).getQualifiedName().toString();
        }
        break;
      default:
        throw new ParameterTypeException("Unknown parameter type: " + componentType.toString(), parameterElement);
    }

    return typeName + actualType;
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
      final ParameterTypeReference parameterTypeRef = this.getParameterReferenceInfo(element);

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
      final ParameterTypeReference parameterTypeRef = this.getParameterReferenceInfo(element);

      info.parameters.add(Pair.of(parameterName, parameterTypeRef));
    }

    return info;
  }
}
