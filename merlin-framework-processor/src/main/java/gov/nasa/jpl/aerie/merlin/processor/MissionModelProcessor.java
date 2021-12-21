package gov.nasa.jpl.aerie.merlin.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.MissionModel;
import gov.nasa.jpl.aerie.merlin.processor.instantiators.ActivityMapperInstantiator;
import gov.nasa.jpl.aerie.merlin.processor.instantiators.AllStaticallyDefinedInstantiator;
import gov.nasa.jpl.aerie.merlin.processor.instantiators.NoneDefinedInstantiator;
import gov.nasa.jpl.aerie.merlin.processor.instantiators.AllDefinedInstantiator;
import gov.nasa.jpl.aerie.merlin.processor.instantiators.SomeStaticallyDefinedInstantiator;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityMapperRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityParameterRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityValidationRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.EffectModelRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.MissionModelRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.TypeRule;
import gov.nasa.jpl.aerie.merlin.protocol.driver.Scheduler;
import gov.nasa.jpl.aerie.merlin.protocol.model.MerlinPlugin;
import gov.nasa.jpl.aerie.merlin.protocol.model.MissionModelFactory;
import gov.nasa.jpl.aerie.merlin.protocol.types.Parameter;

import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class MissionModelProcessor implements Processor {
  private Messager messager = null;
  private Filer filer = null;
  private Elements elementUtils = null;
  private Types typeUtils = null;

  @Override
  public Set<String> getSupportedOptions() {
    return Set.of();
  }

  // Elements marked by these annotations will be treated as processing roots.
  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(
        MissionModel.class.getCanonicalName(),
        ActivityType.class.getCanonicalName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public void init(final ProcessingEnvironment processingEnv) {
    this.messager = processingEnv.getMessager();
    this.filer = processingEnv.getFiler();
    this.elementUtils = processingEnv.getElementUtils();
    this.typeUtils = processingEnv.getTypeUtils();
  }

  private final Set<Element> foundActivityTypes = new HashSet<>();
  private final Set<Element> ownedActivityTypes = new HashSet<>();

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    /// Accumulate any information added in this round.
    this.foundActivityTypes.addAll(roundEnv.getElementsAnnotatedWith(ActivityType.class));

    // Iterate over all elements annotated with @MissionModel
    for (final var element : roundEnv.getElementsAnnotatedWith(MissionModel.class)) {
      final var packageElement = (PackageElement) element;

      try {
        final var missionModelRecord = parseMissionModel(packageElement);

        final var generatedFiles = new ArrayList<JavaFile>();
        generatedFiles.add(generateMerlinPlugin(missionModelRecord));
        generatedFiles.add(generateMissionModelFactory(missionModelRecord));
        generatedFiles.add(generateActivityActions(missionModelRecord));
        generatedFiles.add(generateActivityTypes(missionModelRecord));
        for (final var activityRecord : missionModelRecord.activityTypes) {
          this.ownedActivityTypes.add(activityRecord.declaration);
          if (!activityRecord.mapper.isCustom) {
            generateActivityMapper(missionModelRecord, activityRecord).ifPresent(generatedFiles::add);
          }
        }

        for (final var generatedFile : generatedFiles) {
          this.messager.printMessage(
              Diagnostic.Kind.NOTE,
              "Generating " + generatedFile.packageName + "." + generatedFile.typeSpec.name);

          generatedFile.writeTo(this.filer);
        }
      } catch (final InvalidMissionModelException ex) {
        final var trace = ex.getStackTrace();
        this.messager.printMessage(
            Diagnostic.Kind.ERROR,
            (ex.getMessage()
             + "\n"
             + Arrays
                 .stream(trace)
                 .filter(x -> x.getClassName().startsWith("gov.nasa.jpl.aerie.merlin."))
                 .map(Object::toString)
                 .collect(Collectors.joining("\n"))),
            ex.element,
            ex.annotation,
            ex.attribute);
      } catch (final Throwable ex) {
        final var trace = ex.getStackTrace();
        this.messager.printMessage(
            Diagnostic.Kind.ERROR,
            (ex.getMessage()
             + "\n"
             + Arrays
                 .stream(trace)
                 .filter(x -> x.getClassName().startsWith("gov.nasa.jpl.aerie.merlin."))
                 .map(Object::toString)
                 .collect(Collectors.joining("\n"))));
      }
    }

    if (roundEnv.processingOver()) {
      for (final var foundActivityType : this.foundActivityTypes) {
        if (this.ownedActivityTypes.contains(foundActivityType)) continue;

        this.messager.printMessage(
            Diagnostic.Kind.WARNING,
            "@ActivityType-annotated class is not referenced by any @WithActivity",
            foundActivityType);
      }
    }

    /// Allow other annotation processors to process the framework annotations.
    return false;
  }

  @Override
  public Iterable<? extends Completion> getCompletions(
      final Element element,
      final AnnotationMirror annotation,
      final ExecutableElement member,
      final String userText)
  {
    return Collections::emptyIterator;
  }

  private ActivityMapperInstantiator getMapperInstantiator(final ActivityDefaultsStyle style) {
    return switch (style) {
      case AllStaticallyDefined -> new AllStaticallyDefinedInstantiator();
      case NoneDefined -> new NoneDefinedInstantiator();
      case AllDefined -> new AllDefinedInstantiator();
      case SomeStaticallyDefined -> new SomeStaticallyDefinedInstantiator();
    };
  }

  private MissionModelRecord

  parseMissionModel(final PackageElement missionModelElement)
  throws InvalidMissionModelException
  {
    final var topLevelModel = this.getMissionModelModel(missionModelElement);
    final var modelConfiguration = this.getMissionModelConfiguration(missionModelElement);
    final var activityTypes = new ArrayList<ActivityTypeRecord>();
    final var typeRules = new ArrayList<TypeRule>();

    for (final var factory : this.getMissionModelMapperClasses(missionModelElement)) {
      typeRules.addAll(this.parseValueMappers(factory));
    }

    for (final var activityTypeElement : this.getMissionModelActivityTypes(missionModelElement)) {
      activityTypes.add(this.parseActivityType(missionModelElement, activityTypeElement));
    }

    return new MissionModelRecord(missionModelElement, topLevelModel, modelConfiguration, typeRules, activityTypes);
  }

  private List<TypeRule>
  parseValueMappers(final TypeElement factory)
  throws InvalidMissionModelException
  {
    final var valueMappers = new ArrayList<TypeRule>();

    for (final var element : factory.getEnclosedElements()) {
      if (element.getKind().equals(ElementKind.METHOD)) {
        valueMappers.add(this.parseValueMapperMethod((ExecutableElement) element, ClassName.get(factory)));
      }
    }

    return valueMappers;
  }

  private TypeRule
  parseValueMapperMethod(final ExecutableElement element, final ClassName factory)
  throws InvalidMissionModelException
  {
    if (!element.getModifiers().containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC))) {
      throw new InvalidMissionModelException(
          "Value Mapper method must be public and static",
          element
      );
    }

    final var head = TypePattern.from(element.getReturnType());
    final var enumBoundedTypeParameters = getEnumBoundedTypeParameters(element);
    final var method = element.getSimpleName().toString();
    final var parameters = new ArrayList<TypePattern>();
    for (final var parameter : element.getParameters()) {
      parameters.add(TypePattern.from(parameter));
    }

    return new TypeRule(head, enumBoundedTypeParameters, parameters, factory, method);
  }

  private Set<String>
  getEnumBoundedTypeParameters(final ExecutableElement element)
  throws InvalidMissionModelException
  {
    final var enumBoundedTypeParameters = new HashSet<String>();
    // Ensure type parameters are unbounded or bounded only by enum type.
    // Supporting value mapper resolvers for types like:
    // - `List<? extends Foo>` or
    // - `List<? extends Map<? super Foo, ? extends Bar>>`
    // is not straightforward.
    for (final var typeParameter : element.getTypeParameters()) {
      final var bounds = typeParameter.getBounds();
      for (final var bound : bounds) {
        final var erasure = typeUtils.erasure(bound);
        final var objectType = elementUtils.getTypeElement("java.lang.Object").asType();
        final var enumType = typeUtils.erasure(elementUtils.getTypeElement("java.lang.Enum").asType());
        if (typeUtils.isSameType(erasure, objectType)) {
          // Nothing to do
        } else if (typeUtils.isSameType(erasure, enumType)) {
          enumBoundedTypeParameters.add(typeParameter.getSimpleName().toString());
        } else {
          throw new InvalidMissionModelException(
              "Value Mapper method type parameter must be unbounded, or bounded by enum type only",
              element
          );
        }
      }
    }
    return enumBoundedTypeParameters;
  }

  private ActivityTypeRecord
  parseActivityType(final PackageElement missionModelElement, final TypeElement activityTypeElement)
  throws InvalidMissionModelException
  {
    final var name = this.getActivityTypeName(activityTypeElement);
    final var mapper = this.getActivityMapper(missionModelElement, activityTypeElement);
    final var validations = this.getActivityValidations(activityTypeElement);
    final var parameters = this.getActivityParameters(activityTypeElement);
    final var effectModel = this.getActivityEffectModel(activityTypeElement);

    /*
    The following parameter was created as a result of AERIE-1295/1296/1297 on JIRA
    In order to allow for optional/required parameters, the processor
    must extract the factory method call that creates the default
    template values for some activity. Additionally, a helper method
    is used to determine whether some activity is written as a
    class (old-style) or as a record (new-style) by determining
    whether there are @Parameter tags (old-style) or not
     */
    final var activityDefaultsStyle = this.getActivityDefaultsStyle(activityTypeElement);

    return new ActivityTypeRecord(activityTypeElement, name, mapper,
                                  validations, parameters, effectModel, activityDefaultsStyle);
  }

  private ActivityDefaultsStyle getActivityDefaultsStyle(final TypeElement activityTypeElement)
  {
    for (final var element : activityTypeElement.getEnclosedElements()) {
      if (element.getAnnotation(ActivityType.Parameter.class) != null)
        return ActivityDefaultsStyle.AllDefined;
      if (element.getAnnotation(ActivityType.Template.class) != null)
        return ActivityDefaultsStyle.AllStaticallyDefined;
      if (element.getAnnotation(ActivityType.WithDefaults.class) != null)
        return ActivityDefaultsStyle.SomeStaticallyDefined;
    }
    return ActivityDefaultsStyle.NoneDefined; // No default arguments provided
  }

  private String
  getActivityTypeName(final TypeElement activityTypeElement)
  throws InvalidMissionModelException
  {
    final var annotationMirror = this
        .getAnnotationMirrorByType(activityTypeElement, ActivityType.class)
        .orElseThrow(() -> new InvalidMissionModelException(
            "An activity is somehow missing an @Activity annotation",
            activityTypeElement));

    final var nameAttribute = this
        .getAnnotationAttribute(annotationMirror, "value")
        .orElseThrow(() -> new InvalidMissionModelException(
            "Unable to get value attribute of annotation",
            activityTypeElement,
            annotationMirror));

    return (String) nameAttribute.getValue();
  }

  private ActivityMapperRecord
  getActivityMapper(final PackageElement missionModelElement, final TypeElement activityTypeElement)
  throws InvalidMissionModelException
  {
    final var annotationMirror = this.getAnnotationMirrorByType(activityTypeElement, ActivityType.WithMapper.class);
    if (annotationMirror.isEmpty()) {
      return ActivityMapperRecord.generatedFor(
          ClassName.get(activityTypeElement),
          missionModelElement);
    }

    final var mapperType = (DeclaredType) this
        .getAnnotationAttribute(annotationMirror.get(), "value")
        .orElseThrow(() -> new InvalidMissionModelException(
            "Unable to get value attribute of annotation",
            activityTypeElement,
            annotationMirror.get()))
        .getValue();

    return ActivityMapperRecord.custom(
        ClassName.get((TypeElement) mapperType.asElement()));
  }

  private List<ActivityValidationRecord>
  getActivityValidations(final TypeElement activityTypeElement)
  {
    final var validations = new ArrayList<ActivityValidationRecord>();

    for (final var element : activityTypeElement.getEnclosedElements()) {
      if (element.getAnnotation(ActivityType.Validation.class) == null) continue;

      final var name = element.getSimpleName().toString();
      final var message = element.getAnnotation(ActivityType.Validation.class).value();

      validations.add(new ActivityValidationRecord(name, message));
    }

    return validations;
  }

  private List<ActivityParameterRecord> getActivityParameters(final TypeElement activityTypeElement)
  {
    return getMapperInstantiator(this.getActivityDefaultsStyle(activityTypeElement))
        .getActivityParameters(activityTypeElement);
  }

  private Optional<EffectModelRecord>
  getActivityEffectModel(final TypeElement activityTypeElement)
  {
    for (final var element : activityTypeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.METHOD) continue;

      final var executorAnnotation = element.getAnnotation(ActivityType.EffectModel.class);
      if (executorAnnotation == null) continue;

      if (!(element instanceof ExecutableElement executableElement)) continue;

      final var returnType = executableElement.getReturnType();
      final Optional<TypeMirror> nonVoidReturnType = returnType.getKind() == TypeKind.VOID
          ? Optional.empty()
          : Optional.of(returnType);

      return Optional.of(new EffectModelRecord(element.getSimpleName().toString(), executorAnnotation.value(), nonVoidReturnType));
    }

    return Optional.empty();
  }

  private List<TypeElement>
  getMissionModelMapperClasses(final PackageElement missionModelElement)
  throws InvalidMissionModelException
  {
    final var mapperClassElements = new ArrayList<TypeElement>();

    for (final var withMappersAnnotation : getRepeatableAnnotation(missionModelElement, MissionModel.WithMappers.class)) {
      final var attribute =
          getAnnotationAttribute(withMappersAnnotation, "value").orElseThrow();

      if (!(attribute.getValue() instanceof DeclaredType)) {
        throw new InvalidMissionModelException(
            "Mappers class not yet defined",
            missionModelElement,
            withMappersAnnotation,
            attribute);
      }

      mapperClassElements.add((TypeElement) ((DeclaredType) attribute.getValue()).asElement());
    }

    return mapperClassElements;
  }

  private List<TypeElement>
  getMissionModelActivityTypes(final PackageElement missionModelElement)
  throws InvalidMissionModelException
  {
    final var activityTypeElements = new ArrayList<TypeElement>();

    for (final var activityTypeAnnotation : getRepeatableAnnotation(
        missionModelElement,
        MissionModel.WithActivityType.class)) {
      final var attribute =
          getAnnotationAttribute(activityTypeAnnotation, "value").orElseThrow();

      if (!(attribute.getValue() instanceof DeclaredType)) {
        throw new InvalidMissionModelException(
            "Activity type not yet defined",
            missionModelElement,
            activityTypeAnnotation,
            attribute);
      }

      // DeclaredType cast works because we checked above that attribute is indeed a DeclaredType
      // TypeElement cast works because the element of a DeclaredType must be a TypeElement
      activityTypeElements.add((TypeElement) ((DeclaredType) attribute.getValue()).asElement());
    }

    return activityTypeElements;
  }

  private TypeElement
  getMissionModelModel(final PackageElement missionModelElement)
  throws InvalidMissionModelException
  {
    final var annotationMirror = this
        .getAnnotationMirrorByType(missionModelElement, MissionModel.class)
        .orElseThrow(() -> new InvalidMissionModelException(
            "The missionModel package is somehow missing an @MissionModel annotation",
            missionModelElement));

    final var modelAttribute =
        getAnnotationAttribute(annotationMirror, "model").orElseThrow();
    if (!(modelAttribute.getValue() instanceof DeclaredType)) {
      throw new InvalidMissionModelException(
          "The top-level model is not yet defined",
          missionModelElement,
          annotationMirror,
          modelAttribute);
    }

    // TODO: Check that the given model conforms to the expected protocol.
    //   * Has a (1,1) constructor that takes a Registrar.
    //   It doesn't actually need to subclass Model.
    // TODO: Consider enrolling the given model in a dependency injection framework,
    //   such that the Cursor can be injected like any other constructor argument,
    //   and indeed such that other arguments can flexibly be supported.


    return (TypeElement) ((DeclaredType) modelAttribute.getValue()).asElement();
  }

  private Optional<TypeElement>
  getMissionModelConfiguration(final PackageElement missionModelElement)
  throws InvalidMissionModelException
  {
    final var annotationMirror =
        this.getAnnotationMirrorByType(missionModelElement, MissionModel.WithConfiguration.class);
    if (annotationMirror.isEmpty()) return Optional.empty();

    final var attribute = getAnnotationAttribute(annotationMirror.get(), "value").orElseThrow();
    if (!(attribute.getValue() instanceof DeclaredType)) {
      throw new InvalidMissionModelException(
          "MissionModel configuration type not yet defined",
          missionModelElement,
          annotationMirror.get(),
          attribute);
    }

    return Optional.of((TypeElement) ((DeclaredType) attribute.getValue()).asElement());
  }

  private String
  getRecordInstantiatorWithParams(final String declarationName, final List<ActivityParameterRecord> params)
  {
    return "new "
           + declarationName
           + "("
           + params.stream().map(parameter -> parameter.name + ".get()").collect(Collectors.joining(", "))
           + ")";
  }

  private Optional<Map<String, CodeBlock>>
  buildParameterMapperBlocks(final MissionModelRecord missionModel, final ActivityTypeRecord activityType)
  {
    final var resolver = new Resolver(this.typeUtils, this.elementUtils, missionModel.typeRules);
    var failed = false;
    final var mapperBlocks = new HashMap<String, CodeBlock>();

    for (final var parameter : activityType.parameters) {
      final var mapperBlock = resolver.instantiateNullableMapperFor(parameter.type);
      if (mapperBlock.isPresent()) {
        mapperBlocks.put(parameter.name, mapperBlock.get());
      } else {
        failed = true;
        messager.printMessage(
            Diagnostic.Kind.ERROR,
            "Failed to generate value mapper for parameter",
            parameter.element);
      }
    }

    return failed ? Optional.empty() : Optional.of(mapperBlocks);
  }

  private CodeBlock buildConfigurationMapperBlock(final MissionModelRecord missionModel, final TypeElement typeElem) {
    final var resolver = new Resolver(this.typeUtils, this.elementUtils, missionModel.typeRules);
    final var mapperBlock = resolver.instantiateMapperFor(typeElem.asType());
    if (mapperBlock.isEmpty()) {
      messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate value mapper for configuration", typeElem);
    }
    return mapperBlock.get();
  }

  private Optional<JavaFile>
  generateActivityMapper(final MissionModelRecord missionModel, final ActivityTypeRecord activityType)
  {
    final var maybeMapperBlocks = buildParameterMapperBlocks(missionModel, activityType);
    if (maybeMapperBlocks.isEmpty()) return Optional.empty();
    final var effectModelReturnType = activityType.effectModel.flatMap(EffectModelRecord::returnType);
    final Optional<CodeBlock> effectModelReturnMapperBlock =
        effectModelReturnType
            .flatMap(returnType ->  new Resolver(this.typeUtils, this.elementUtils, missionModel.typeRules)
                  .instantiateNullableMapperFor(returnType));
    if (effectModelReturnType.isPresent() && effectModelReturnMapperBlock.isEmpty()) {
      messager.printMessage(
          Diagnostic.Kind.ERROR,
          "Failed to generate value mapper for effect model return type "
          + effectModelReturnType.get()
          + " of activity "
          + activityType.name);
      return Optional.empty();
    }
    final var mapperBlocks = maybeMapperBlocks.get();

    final var typeSpec =
        TypeSpec
            .classBuilder(activityType.mapper.name)
            // The location of the missionModel package determines where to put this class.
            .addOriginatingElement(missionModel.$package)
            // The fields and methods of the activity determines the overall behavior of this class.
            .addOriginatingElement(activityType.declaration)
            // TODO: Add an originating element for each of the mapper rulesets associated with the mission model.
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.class),
                    ParameterizedTypeName.get(
                        ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                        ClassName.get(missionModel.topLevelModel)),
                    ClassName.get(activityType.declaration)))
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addFields(
                activityType.parameters
                    .stream()
                    .map(parameter -> FieldSpec
                        .builder(
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.framework.ValueMapper.class),
                                TypeName.get(parameter.type).box()),
                            "mapper_" + parameter.name)
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                    .collect(Collectors.toList()))
            .addFields(
                effectModelReturnType
                    .stream()
                    .map(returnType -> FieldSpec
                        .builder(
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.framework.ValueMapper.class),
                                TypeName.get(returnType).box()),
                            "effectModelReturnTypeMapper")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                    .collect(Collectors.toList()))
            .addMethod(
                MethodSpec
                    .constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    /* Suppress unchecked warnings because the resolver has to
                        put some big casting in for Class parameters
                     */
                    .addAnnotation(
                        AnnotationSpec
                            .builder(SuppressWarnings.class)
                            .addMember("value", "$S", "unchecked")
                            .build())
                    .addCode(
                        activityType.parameters
                            .stream()
                            .map(parameter -> CodeBlock
                                .builder()
                                .addStatement(
                                    "this.mapper_$L =\n$L",
                                    parameter.name,
                                    mapperBlocks.get(parameter.name)))
                            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                            .build())
                    .addCode(
                        effectModelReturnMapperBlock
                            .stream()
                            .map(mapperBlock -> CodeBlock
                                .builder()
                                .addStatement(
                                    "this.effectModelReturnTypeMapper =\n$L",
                                    mapperBlock
                                ))
                            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                            .build())
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("getName")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(String.class)
                    .addStatement("return $S", activityType.name)
                    .build())
            .addMethod(getMapperInstantiator(activityType.activityDefaultsStyle).makeGetRequiredParametersMethod(activityType))
            .addMethod(getMapperInstantiator(activityType.activityDefaultsStyle).makeGetParametersMethod(activityType))
            .addMethod(getMapperInstantiator(activityType.activityDefaultsStyle).makeGetReturnValueSchemaMethod(activityType))
            .addMethod(getMapperInstantiator(activityType.activityDefaultsStyle).makeSerializeReturnValueMethod(activityType))
            .addMethod(getMapperInstantiator(activityType.activityDefaultsStyle).makeGetArgumentsMethod(activityType))
            .addMethod(getMapperInstantiator(activityType.activityDefaultsStyle).makeInstantiateMethod(activityType))
            .addMethod(
                MethodSpec
                    .methodBuilder("getValidationFailures")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(
                        java.util.List.class,
                        String.class))
                    .addParameter(
                        TypeName.get(activityType.declaration.asType()),
                        "activity",
                        Modifier.FINAL)
                    .addStatement(
                        "final var $L = new $T()",
                        "failures",
                        ParameterizedTypeName.get(
                            java.util.ArrayList.class,
                            String.class))
                    .addCode(
                        activityType.validations
                            .stream()
                            .map(validation -> CodeBlock
                                .builder()
                                .addStatement(
                                    "if (!$L.$L()) failures.add($S)",
                                    "activity",
                                    validation.methodName,
                                    validation.failureMessage))
                            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                            .build())
                    .addStatement(
                        "return $L",
                        "failures")
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("createTask")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.Task.class))
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                            ClassName.get(missionModel.topLevelModel)),
                        "model",
                        Modifier.FINAL)
                    .addParameter(
                        TypeName.get(activityType.declaration.asType()),
                        "activity",
                        Modifier.FINAL)
                    .addCode(
                        activityType.effectModel
                            .map(effectModel -> CodeBlock
                                  .builder()
                                  .addStatement(
                                      "return $T.$L(() -> $L.$L($L.model())).create($L.executor())",
                                      gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                      switch (effectModel.executor()) {
                                        case Threaded -> "threaded";
                                        case Replaying -> "replaying";
                                      },
                                      "activity",
                                      effectModel.methodName(),
                                      "model",
                                      "model")
                                  .build())
                            .orElseGet(() -> CodeBlock
                                .builder()
                                .addStatement(
                                    "return new $T($$ -> {})",
                                    gov.nasa.jpl.aerie.merlin.framework.OneShotTask.class)
                                .build()))
                    .build())
            .build();

    return Optional.of(JavaFile
                           .builder(activityType.mapper.name.packageName(), typeSpec)
                           .skipJavaLangImports(true)
                           .build());
  }

  private JavaFile generateActivityActions(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getActivityActionsName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            // The location of the mission model package determines where to put this class.
            .addOriginatingElement(missionModel.$package)
            // TODO: List found task spec types as dependencies of this generated file.
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC)
            .superclass(gov.nasa.jpl.aerie.merlin.framework.ModelActions.class)
            .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
            .addMethods(
                missionModel.activityTypes
                    .stream()
                    .flatMap(entry -> List
                        .of(
                            MethodSpec
                                .methodBuilder("spawn")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(Scheduler.TaskIdentifier.class)
                                .addParameter(
                                    ClassName.get(entry.declaration),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "final var $L = new $T()",
                                    "mapper",
                                    entry.mapper.name)
                                .addStatement(
                                    "return $T.spawn($L.getName(), $L.getArguments($L))",
                                    gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                    "mapper",
                                    "mapper",
                                    "activity")
                                .build(),
                            MethodSpec
                                .methodBuilder("defer")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(Scheduler.TaskIdentifier.class)
                                .addParameter(
                                    ParameterSpec
                                        .builder(
                                            gov.nasa.jpl.aerie.merlin.protocol.types.Duration.class,
                                            "duration")
                                        .addModifiers(Modifier.FINAL)
                                        .build())
                                .addParameter(
                                    ClassName.get(entry.declaration),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "final var $L = new $T()",
                                    "mapper",
                                    entry.mapper.name)
                                .addStatement(
                                    "return $T.defer($L, $L.getName(), $L.getArguments($L))",
                                    gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                    "duration",
                                    "mapper",
                                    "mapper",
                                    "activity")
                                .build(),
                            MethodSpec
                                .methodBuilder("defer")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(Scheduler.TaskIdentifier.class)
                                .addParameter(
                                    ParameterSpec
                                        .builder(
                                            TypeName.LONG,
                                            "quantity")
                                        .addModifiers(Modifier.FINAL)
                                        .build())
                                .addParameter(
                                    ParameterSpec
                                        .builder(
                                            gov.nasa.jpl.aerie.merlin.protocol.types.Duration.class,
                                            "unit")
                                        .addModifiers(Modifier.FINAL)
                                        .build())
                                .addParameter(
                                    ClassName.get(entry.declaration),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "return defer($L.times($L), $L)",
                                    "unit",
                                    "quantity",
                                    "activity")
                                .build(),
                            MethodSpec
                                .methodBuilder("call")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(TypeName.VOID)
                                .addParameter(
                                    ClassName.get(entry.declaration),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "$T.waitFor(spawn($L))",
                                    gov.nasa.jpl.aerie.merlin.framework.ModelActions.class,
                                    "activity")
                                .build())
                        .stream())
                    .collect(Collectors.toList()))
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  private JavaFile generateMissionModelFactory(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getFactoryName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(MissionModelFactory.class),
                    ParameterizedTypeName.get(
                        ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                        ClassName.get(missionModel.topLevelModel))))
            .addMethod(
                MethodSpec
                    .methodBuilder("getTaskSpecTypes")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(
                        ClassName.get(Map.class),
                        ClassName.get(String.class),
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.class),
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                                ClassName.get(missionModel.topLevelModel)),
                            WildcardTypeName.subtypeOf(Object.class))))
                    .addStatement("return $T.activityTypes", missionModel.getTypesName())
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("instantiate")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(
                        TypeName.get(gov.nasa.jpl.aerie.merlin.protocol.types.SerializedValue.class),
                        "configuration",
                        Modifier.FINAL)
                    .addParameter(
                        ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.driver.Initializer.class),
                        "builder",
                        Modifier.FINAL)
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                            ClassName.get(missionModel.topLevelModel)))
                    .addStatement(
                        "final var $L = new $T($L)",
                        "registrar",
                        gov.nasa.jpl.aerie.merlin.framework.Registrar.class,
                        "builder")
                    .addStatement(
                        "final var $L = $T.makeExecutorService()",
                        "executor",
                        gov.nasa.jpl.aerie.merlin.framework.RootModel.class)
                    .addCode("\n")
                    .addCode(
                        missionModel.modelConfiguration
                            .map(configElem -> CodeBlock  // if configuration is provided
                                                          .builder()
                                                          .addStatement(
                                                              "final var $L = $L",
                                                              "configMapper",
                                                              buildConfigurationMapperBlock(missionModel, configElem))
                                                          .addStatement(
                                                              "final var $L = $L.deserializeValue($L).getSuccessOrThrow()",
                                                              "deserializedConfig",
                                                              "configMapper",
                                                              "configuration")
                                                          .addStatement(
                                                              "final var $L = $T.initializing($L, $L, () -> new $T($L, $L))",
                                                              "model",
                                                              gov.nasa.jpl.aerie.merlin.framework.InitializationContext.class,
                                                              "executor",
                                                              "builder",
                                                              ClassName.get(missionModel.topLevelModel),
                                                              "registrar",
                                                              "deserializedConfig")
                                                          .build())
                            .orElseGet(() -> CodeBlock  // if configuration is not provided
                                                        .builder()
                                                        .addStatement(
                                                            "final var $L = $T.initializing($L, $L, () -> new $T($L))",
                                                            "model",
                                                            gov.nasa.jpl.aerie.merlin.framework.InitializationContext.class,
                                                            "executor",
                                                            "builder",
                                                            ClassName.get(missionModel.topLevelModel),
                                                            "registrar")
                                                        .build()))
                    .addCode("\n")
                    .addStatement(
                        "return new $T<$T>($L, $L)",
                        gov.nasa.jpl.aerie.merlin.framework.RootModel.class,
                        ClassName.get(missionModel.topLevelModel),
                        "model",
                        "executor")
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("getParameters")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(List.class, Parameter.class))
                    .addCode(
                        missionModel.modelConfiguration
                            .map(configElem -> CodeBlock  // if configuration is provided
                                .builder()
                                .addStatement("return $L.getValueSchema().asStruct().map(parameterMap ->\n"+
                                              "parameterMap.keySet().stream().map(name -> new $T(name, parameterMap.get(name))).toList())\n"+
                                              ".orElse($T.of())",
                                    buildConfigurationMapperBlock(missionModel, configElem),
                                    Parameter.class,
                                    List.class)
                                .build())
                            .orElse(CodeBlock
                                .builder()
                                .addStatement("return $T.of()", List.class)
                                .build()))
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  private JavaFile generateActivityTypes(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getTypesName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addField(
                FieldSpec
                    .builder(
                        ParameterizedTypeName.get(
                            ClassName.get(List.class),
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.class),
                                ParameterizedTypeName.get(
                                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                                    ClassName.get(missionModel.topLevelModel)),
                                WildcardTypeName.subtypeOf(Object.class))),
                        "activityTypeList",
                        Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer(
                        "$T.of($>$>\n$L$<$<)",
                        List.class,
                        missionModel.activityTypes
                            .stream()
                            .map(activityType -> CodeBlock.builder().add("new $T()", activityType.mapper.name))
                            .reduce((x, y) -> x.add(",\n$L", y.build()))
                            .orElse(CodeBlock.builder())
                            .build())
                    .build())
            .addField(
                FieldSpec
                    .builder(
                        ParameterizedTypeName.get(
                            ClassName.get(Map.class),
                            ClassName.get(String.class),
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.model.TaskSpecType.class),
                                ParameterizedTypeName.get(
                                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.RootModel.class),
                                    ClassName.get(missionModel.topLevelModel)),
                                WildcardTypeName.subtypeOf(Object.class))),
                        "activityTypes",
                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer(
                        "$L.stream().collect($T.toMap($$ -> $$.getName(), $$ -> $$));",
                        "activityTypeList",
                        java.util.stream.Collectors.class)
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  private JavaFile generateMerlinPlugin(final MissionModelRecord missionModel) {
    final var typeName = missionModel.getPluginName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", MissionModelProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(MerlinPlugin.class)
            .addMethod(
                MethodSpec
                    .methodBuilder("getFactory")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(missionModel.getFactoryName())
                    .addStatement(
                        "return new $T()",
                        missionModel.getFactoryName())
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }


  private List<AnnotationMirror>
  getRepeatableAnnotation(final Element element, final Class<? extends Annotation> annotationClass)
  {
    final var containerClass = annotationClass.getAnnotation(Repeatable.class).value();

    final var annotationType = this.elementUtils.getTypeElement(annotationClass.getCanonicalName()).asType();
    final var containerType = this.elementUtils.getTypeElement(containerClass.getCanonicalName()).asType();

    final var mirrors = new ArrayList<AnnotationMirror>();
    for (final var mirror : element.getAnnotationMirrors()) {
      if (this.typeUtils.isSameType(annotationType, mirror.getAnnotationType())) {
        mirrors.add(mirror);
      } else if (this.typeUtils.isSameType(containerType, mirror.getAnnotationType())) {
        // SAFETY: a container annotation has a value() attribute that is an array of annotations
        @SuppressWarnings("unchecked")
        final var containedMirrors =
            (List<AnnotationMirror>)
                getAnnotationAttribute(mirror, "value")
                    .orElseThrow()
                    .getValue();

        mirrors.addAll(containedMirrors);
      }
    }

    return mirrors;
  }

  private Optional<AnnotationValue>
  getAnnotationAttribute(final AnnotationMirror annotationMirror, final String attributeName)
  {
    for (final var entry : annotationMirror.getElementValues().entrySet()) {
      if (Objects.equals(attributeName, entry.getKey().getSimpleName().toString())) {
        return Optional.of(entry.getValue());
      }
    }

    return Optional.empty();
  }

  private Optional<AnnotationMirror>
  getAnnotationMirrorByType(final Element element, final Class<? extends Annotation> annotationClass)
  {
    final var annotationType = this.elementUtils
        .getTypeElement(annotationClass.getCanonicalName())
        .asType();

    for (final var x : element.getAnnotationMirrors()) {
      if (this.typeUtils.isSameType(annotationType, x.getAnnotationType())) {
        return Optional.of(x);
      }
    }

    return Optional.empty();
  }
}
