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
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import gov.nasa.jpl.aerie.merlin.framework.annotations.ActivityType;
import gov.nasa.jpl.aerie.merlin.framework.annotations.Adaptation;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityExecutionType;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityMapperRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityParameterRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityTypeRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.ActivityValidationRecord;
import gov.nasa.jpl.aerie.merlin.processor.metamodel.AdaptationRecord;

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
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class AdaptationProcessor implements Processor {
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
        Adaptation.class.getCanonicalName(),
        ActivityType.class.getCanonicalName());
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_11;
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
    ///Accumulate any information added in this round.
    this.foundActivityTypes.addAll(roundEnv.getElementsAnnotatedWith(ActivityType.class));

    for (final var element : roundEnv.getElementsAnnotatedWith(Adaptation.class)) {
      final var packageElement = (PackageElement) element;

      try {
        final var adaptationRecord = parseAdaptation(packageElement);

        final var generatedFiles = new ArrayList<JavaFile>();
        generatedFiles.add(generateAdaptationFactory(adaptationRecord));
        generatedFiles.add(generateTaskClass(adaptationRecord));
        generatedFiles.add(generateModelClass(adaptationRecord));
        generatedFiles.add(generateActivityTypes(adaptationRecord));
        for (final var activityRecord : adaptationRecord.activityTypes) {
          this.ownedActivityTypes.add(activityRecord.declaration);
          if (!activityRecord.mapper.isCustom) {
            generatedFiles.add(generateActivityMapper(adaptationRecord, activityRecord));
          }
        }

        for (final var generatedFile : generatedFiles) {
          this.messager.printMessage(
              Diagnostic.Kind.NOTE,
              "Generating " + generatedFile.packageName + "." + generatedFile.typeSpec.name);

          generatedFile.writeTo(this.filer);
        }
      } catch (final InvalidAdaptationException ex) {
        final var trace = ex.getStackTrace();
        this.messager.printMessage(
            Diagnostic.Kind.ERROR,
            ( ex.getMessage()
              + "\n"
              + Arrays
                  .stream(trace)
                  .filter(x -> x.getClassName().startsWith("gov.nasa.jpl.aerie.merlin."))
                  .map(Object::toString)
                  .collect(Collectors.joining("\n")) ),
            ex.element,
            ex.annotation,
            ex.attribute);
      } catch (final Throwable ex) {
        final var trace = ex.getStackTrace();
        this.messager.printMessage(
            Diagnostic.Kind.ERROR,
            ( ex.getMessage()
              + "\n"
              + Arrays
                  .stream(trace)
                  .filter(x -> x.getClassName().startsWith("gov.nasa.jpl.aerie.merlin."))
                  .map(Object::toString)
                  .collect(Collectors.joining("\n")) ));
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

    ///Allow other annotation processors to process the framework annotations.
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


  private AdaptationRecord
  parseAdaptation(final PackageElement adaptationElement)
  throws InvalidAdaptationException {
    final var topLevelModel = this.getAdaptationModel(adaptationElement);
    final var activityTypes = new ArrayList<ActivityTypeRecord>();

    // TODO: Get any mapper groups registered using @WithMappers

    for (final var activityTypeElement : this.getAdaptationActivityTypes(adaptationElement)) {
      activityTypes.add(this.parseActivityType(adaptationElement, activityTypeElement));
    }

    return new AdaptationRecord(adaptationElement, topLevelModel, activityTypes);
  }

  private ActivityTypeRecord
  parseActivityType(final PackageElement adaptationElement, final TypeElement activityTypeElement)
  throws InvalidAdaptationException {
    final var name = this.getActivityTypeName(activityTypeElement);
    final var mapper = this.getActivityMapper(adaptationElement, activityTypeElement);
    final var validations = this.getActivityValidations(activityTypeElement);
    final var parameters = this.getActivityParameters(activityTypeElement);
    final var effectModel = this.getActivityExecutionType(activityTypeElement);

    return new ActivityTypeRecord(activityTypeElement, name, mapper, validations, parameters, effectModel);
  }

  private String
  getActivityTypeName(final TypeElement activityTypeElement)
  throws InvalidAdaptationException {
    final var annotationMirror = this
        .getAnnotationMirrorByType(activityTypeElement, ActivityType.class)
        .orElseThrow(() -> new InvalidAdaptationException(
            "An activity is somehow missing an @Activity annotation",
            activityTypeElement));

    final var nameAttribute = this
        .getAnnotationAttribute(annotationMirror, "value")
        .orElseThrow(() -> new InvalidAdaptationException(
            "Unable to get value attribute of annotation",
            activityTypeElement,
            annotationMirror));

    return (String) nameAttribute.getValue();
  }

  private ActivityMapperRecord
  getActivityMapper(final PackageElement adaptationElement, final TypeElement activityTypeElement)
  throws InvalidAdaptationException {
    final var annotationMirror = this.getAnnotationMirrorByType(activityTypeElement, ActivityType.WithMapper.class);
    if (annotationMirror.isEmpty()) {
      return ActivityMapperRecord.generatedFor(
          ClassName.get(activityTypeElement),
          adaptationElement);
    }

    final var mapperType = (DeclaredType) this
        .getAnnotationAttribute(annotationMirror.get(), "value")
        .orElseThrow(() -> new InvalidAdaptationException(
          "Unable to get value attribute of annotation",
          activityTypeElement,
          annotationMirror.get()))
        .getValue();

    return ActivityMapperRecord.custom(
        ClassName.get((TypeElement) mapperType.asElement()));
  }

  private List<ActivityValidationRecord>
  getActivityValidations(final TypeElement activityTypeElement) {
    final var validations = new ArrayList<ActivityValidationRecord>();

    for (final var element : activityTypeElement.getEnclosedElements()) {
      if (element.getAnnotation(ActivityType.Validation.class) == null) continue;

      final var name = element.getSimpleName().toString();
      final var message = element.getAnnotation(ActivityType.Validation.class).value();

      validations.add(new ActivityValidationRecord(name, message));
    }

    return validations;
  }

  private List<ActivityParameterRecord>
  getActivityParameters(final TypeElement activityTypeElement) {
    final var parameters = new ArrayList<ActivityParameterRecord>();

    for (final var element : activityTypeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.FIELD) continue;
      if (element.getAnnotation(ActivityType.Parameter.class) == null) continue;

      final var name = element.getSimpleName().toString();
      final var type = element.asType();

      parameters.add(new ActivityParameterRecord(name, type));
    }

    return parameters;
  }

  private ActivityExecutionType
  getActivityExecutionType(final TypeElement activityTypeElement) {
    for (final var element : activityTypeElement.getEnclosedElements()) {
      if (element.getKind() != ElementKind.CLASS) continue;
      if (!element.getSimpleName().toString().equals("EffectModel")) continue;

      final var executorAnnotation = activityTypeElement.getAnnotation(ActivityType.WithExecutor.class);
      if (executorAnnotation == null) return ActivityExecutionType.Threaded;

      switch (executorAnnotation.value()) {
        case Threaded: return ActivityExecutionType.Threaded;
        case Replaying: return ActivityExecutionType.Replaying;
      }
    }

    return ActivityExecutionType.None;
  }

  private List<TypeElement>
  getAdaptationActivityTypes(final PackageElement adaptationElement)
  throws InvalidAdaptationException {
    final var activityTypeElements = new ArrayList<TypeElement>();

    for (final var activityTypeAnnotation : getRepeatableAnnotation(adaptationElement, Adaptation.WithActivityType.class)) {
      final var attribute = getAnnotationAttribute(activityTypeAnnotation, "value").orElseThrow();

      if (!(attribute.getValue() instanceof DeclaredType)) {
        throw new InvalidAdaptationException(
            "Activity type not yet defined",
            adaptationElement,
            activityTypeAnnotation,
            attribute);
      }

      activityTypeElements.add((TypeElement) ((DeclaredType) attribute.getValue()).asElement());
    }

    return activityTypeElements;
  }

  private TypeElement
  getAdaptationModel(final PackageElement adaptationElement)
  throws InvalidAdaptationException {
    final var annotationMirror = this
        .getAnnotationMirrorByType(adaptationElement, Adaptation.class)
        .orElseThrow(() -> new InvalidAdaptationException(
            "The adaptation package is somehow missing an @Adaptation annotation",
            adaptationElement));

    final var modelAttribute = getAnnotationAttribute(annotationMirror, "model").orElseThrow();
    if (!(modelAttribute.getValue() instanceof DeclaredType)) {
      throw new InvalidAdaptationException(
          "The top-level model is not yet defined",
          adaptationElement,
          annotationMirror,
          modelAttribute);
    }

    // TODO: Check that the given model conforms to the expected protocol.
    //   * Has a (1,1) constructor that takes a type $Schema and a Registrar<$Schema>.
    //   It doesn't actually need to subclass Model.
    // TODO: Consider enrolling the given model in a dependency injection framework,
    //   such that the Cursor can be injected like any other constructor argument,
    //   and indeed such that other arguments can flexibly be supported.


    return (TypeElement) ((DeclaredType) modelAttribute.getValue()).asElement();
  }


  private JavaFile generateActivityMapper(final AdaptationRecord adaptation, final ActivityTypeRecord activityType) {
    final var resolver = new Resolver(this.typeUtils, this.elementUtils);

    final var typeSpec =
        TypeSpec
            .classBuilder(activityType.mapper.name)
            // The location of the adaptation package determines where to put this class.
            .addOriginatingElement(adaptation.$package)
            // The fields and methods of the activity determines the overall behavior of this class.
            .addOriginatingElement(activityType.declaration)
            // TODO: Add an originating element for each of the mapper rulesets associated with the adaptation.
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", AdaptationProcessor.class.getCanonicalName())
                    .build())
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.ActivityMapper.class),
                    TypeName.get(activityType.declaration.asType())))
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addFields(
                activityType.parameters
                    .stream()
                    .map(parameter -> FieldSpec
                        .builder(
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.ValueMapper.class),
                                TypeName.get(parameter.type).box()),
                            "mapper_" + parameter.name)
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .build())
                    .collect(Collectors.toList()))
            .addMethod(
                MethodSpec
                    .constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addCode(
                        activityType.parameters
                            .stream()
                            .map(parameter -> CodeBlock
                                .builder()
                                .addStatement(
                                    "this.mapper_$L =\n$L",
                                    parameter.name,
                                    resolver.instantiateMapperFor(parameter.type)))
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
            .addMethod(
                MethodSpec
                    .methodBuilder("getParameters")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(
                        java.util.Map.class,
                        String.class,
                        gov.nasa.jpl.aerie.merlin.protocol.ValueSchema.class))
                    .addStatement(
                        "final var $L = new $T()",
                        "parameters",
                        ParameterizedTypeName.get(
                            java.util.HashMap.class,
                            String.class,
                            gov.nasa.jpl.aerie.merlin.protocol.ValueSchema.class))
                    .addCode(
                        activityType.parameters
                            .stream()
                            .map(parameter -> CodeBlock
                                .builder()
                                .addStatement(
                                    "$L.put($S, this.mapper_$L.getValueSchema())",
                                    "parameters",
                                    parameter.name,
                                    parameter.name))
                            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                            .build())
                    .addStatement(
                        "return $L",
                        "parameters")
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("getArguments")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(ParameterizedTypeName.get(
                        java.util.Map.class,
                        String.class,
                        gov.nasa.jpl.aerie.merlin.protocol.SerializedValue.class))
                    .addParameter(
                        TypeName.get(activityType.declaration.asType()),
                        "activity",
                        Modifier.FINAL)
                    .addStatement(
                        "final var $L = new $T()",
                        "arguments",
                        ParameterizedTypeName.get(
                            java.util.HashMap.class,
                            String.class,
                            gov.nasa.jpl.aerie.merlin.protocol.SerializedValue.class))
                    .addCode(
                        activityType.parameters
                            .stream()
                            .map(parameter -> CodeBlock
                                .builder()
                                .addStatement(
                                    "$L.put($S, this.mapper_$L.serializeValue($L.$L))",
                                    "arguments",
                                    parameter.name,
                                    parameter.name,
                                    "activity",
                                    parameter.name))
                            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                            .build())
                    .addStatement(
                        "return $L",
                        "arguments")
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("instantiateDefault")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(TypeName.get(activityType.declaration.asType()))
                    .addStatement("return new $T()", TypeName.get(activityType.declaration.asType()))
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("instantiate")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(TypeName.get(activityType.declaration.asType()))
                    .addException(gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType.UnconstructableTaskSpecException.class)
                    .addParameter(
                        ParameterizedTypeName.get(
                            java.util.Map.class,
                            String.class,
                            gov.nasa.jpl.aerie.merlin.protocol.SerializedValue.class),
                        "arguments",
                        Modifier.FINAL)
                    .addStatement(
                        "final var $L = new $T()",
                        "activity",
                        TypeName.get(activityType.declaration.asType()))
                    .beginControlFlow(
                        "for (final var $L : $L.entrySet())",
                        "entry",
                        "arguments")
                    .beginControlFlow(
                        "switch ($L.getKey())",
                        "entry")
                    .addCode(
                        activityType.parameters
                            .stream()
                            .map(parameter -> CodeBlock
                                .builder()
                                .add("case $S:\n", parameter.name)
                                .indent()
                                .addStatement(
                                    "$L.$L = this.mapper_$L"
                                    + "\n" + ".deserializeValue($L.getValue())"
                                    + "\n" + ".getSuccessOrThrow($$ -> new $T())",
                                    "activity",
                                    parameter.name,
                                    parameter.name,
                                    "entry",
                                    gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType.UnconstructableTaskSpecException.class)
                                .addStatement("break")
                                .unindent())
                            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                            .build())
                    .addCode(
                        CodeBlock
                            .builder()
                            .add("default:\n")
                            .indent()
                            .addStatement(
                                "throw new $T()",
                                gov.nasa.jpl.aerie.merlin.protocol.TaskSpecType.UnconstructableTaskSpecException.class)
                            .unindent()
                            .build())
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement(
                        "return $L",
                        "activity")
                    .build())
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
            .build();

    return JavaFile
        .builder(activityType.mapper.name.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  private JavaFile generateActivityTypes(final AdaptationRecord adaptation) {
    final var typeName = adaptation.getMasterActivityTypesName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            // The location of the adaptation package determines where to put this class.
            .addOriginatingElement(adaptation.$package)
            // TODO: List found task spec types as originating elements.
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", AdaptationProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(
                MethodSpec
                    .constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("register")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addTypeVariable(TypeVariableName.get("$Schema"))
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.framework.AdaptationBuilder.class),
                            TypeVariableName.get("$Schema")),
                        "builder",
                        Modifier.FINAL)
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get(adaptation.topLevelModel),
                            TypeVariableName.get("$Schema")),
                        "model",
                        Modifier.FINAL)
                    .addStatement(
                        "final var $L = $L.getRootContext();",
                        "rootContext",
                        "builder")
                    .addCode(
                        adaptation.activityTypes
                            .stream()
                            .map(activityType ->
                                     (activityType.effectModel == ActivityExecutionType.None)
                                         ? CodeBlock
                                             .builder()
                                             .addStatement(
                                                 "$L.noopTask(new $T())",
                                                 "builder",
                                                 activityType.mapper.name)
                                         : (activityType.effectModel == ActivityExecutionType.Threaded)
                                             ? CodeBlock
                                                 .builder()
                                                 .addStatement(
                                                     "$L.threadedTask("
                                                     + "\n" + "new $T(),"
                                                     + "\n" + "activity -> $>$>activity"
                                                     + "\n" + ".new EffectModel<$T>()"
                                                     + "\n" + ".runWith($L.get(), $L)$<$<)",
                                                     "builder",
                                                     activityType.mapper.name,
                                                     TypeVariableName.get("$Schema"),
                                                     "rootContext",
                                                     "model")
                                             : CodeBlock
                                                 .builder()
                                                 .addStatement(
                                                     "$L.replayingTask("
                                                     + "\n" + "new $T(),"
                                                     + "\n" + "activity -> $>$>activity"
                                                     + "\n" + ".new EffectModel<$T>())"
                                                     + "\n" + ".runWith($L.get(), $L)$<$<)",
                                                     "builder",
                                                     activityType.mapper.name,
                                                     TypeVariableName.get("$Schema"),
                                                     "rootContext",
                                                     "model"))
                            .reduce(CodeBlock.builder(), (x, y) -> x.add(y.build()))
                            .build())
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  private JavaFile generateModelClass(final AdaptationRecord adaptation) {
    final var typeName = adaptation.getModelName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            // The location of the adaptation package determines where to put this class.
            .addOriginatingElement(adaptation.$package)
            // TODO: List found task spec types as dependencies of this generated file.
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", AdaptationProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addTypeVariable(TypeVariableName.get("$Schema"))
            .superclass(
                ParameterizedTypeName.get(
                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.Model.class),
                    TypeVariableName.get("$Schema")))
            .addMethod(
                MethodSpec
                    .constructorBuilder()
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(
                        ParameterSpec
                            .builder(
                                ParameterizedTypeName.get(
                                    ClassName.get(java.util.function.Supplier.class),
                                    WildcardTypeName.subtypeOf(
                                        ParameterizedTypeName.get(
                                            ClassName.get(gov.nasa.jpl.aerie.merlin.framework.Context.class),
                                            TypeVariableName.get("$Schema")))),
                                "context")
                            .addModifiers(Modifier.FINAL)
                            .build())
                    .addStatement("super($L)", "context")
                    .build())
            .addMethod(
                MethodSpec
                    .constructorBuilder()
                    .addModifiers(Modifier.PROTECTED)
                    .addParameter(
                        ParameterSpec
                            .builder(
                                ParameterizedTypeName.get(
                                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.Registrar.class),
                                    TypeVariableName.get("$Schema")),
                                "registrar")
                            .addModifiers(Modifier.FINAL)
                            .build())
                    .addStatement(
                        "super($L)",
                        "registrar")
                    .build())
            .addMethods(
                adaptation.activityTypes
                    .stream()
                    .flatMap(entry -> List
                        .of(
                            MethodSpec
                                .methodBuilder("spawn")
                                .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                                .returns(String.class)
                                .addParameter(
                                    ClassName.get(entry.declaration),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "final var $L = new $L()",
                                    "mapper",
                                    entry.mapper.name)
                                .addStatement(
                                    "return spawn($L.getName(), $L.getArguments($L))",
                                    "mapper",
                                    "mapper",
                                    "activity")
                                .build(),
                            MethodSpec
                                .methodBuilder("defer")
                                .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                                .returns(String.class)
                                .addParameter(
                                    ParameterSpec
                                        .builder(
                                            gov.nasa.jpl.aerie.time.Duration.class,
                                            "duration")
                                        .addModifiers(Modifier.FINAL)
                                        .build())
                                .addParameter(
                                    ClassName.get(entry.declaration),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "final var $L = new $L()",
                                    "mapper",
                                    entry.mapper.name)
                                .addStatement(
                                    "return defer($L, $L.getName(), $L.getArguments($L))",
                                    "duration",
                                    "mapper",
                                    "mapper",
                                    "activity")
                                .build(),
                            MethodSpec
                                .methodBuilder("defer")
                                .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                                .returns(String.class)
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
                                            gov.nasa.jpl.aerie.time.Duration.class,
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
                                .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                                .returns(TypeName.VOID)
                                .addParameter(
                                    ClassName.get(entry.declaration),
                                    "activity",
                                    Modifier.FINAL)
                                .addStatement(
                                    "waitFor(spawn($L))",
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

  private JavaFile generateTaskClass(final AdaptationRecord adaptation) {
    final var typeName = adaptation.getTaskName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            // The location of the adaptation package determines where to put this class.
            .addOriginatingElement(adaptation.$package)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", AdaptationProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addTypeVariable(TypeVariableName.get("$Schema"))
            .superclass(
                ParameterizedTypeName.get(
                    adaptation.getModelName(),
                    TypeVariableName.get("$Schema")))
            .addField(
                FieldSpec
                    .builder(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.framework.Scoped.class),
                            ParameterizedTypeName.get(
                                ClassName.get(gov.nasa.jpl.aerie.merlin.framework.Context.class),
                                TypeVariableName.get("$Schema"))),
                        "context")
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                    .build())
            .addMethod(
                MethodSpec
                    .constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .addParameter(
                        ParameterSpec
                            .builder(
                                ParameterizedTypeName.get(
                                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.Scoped.class),
                                    ParameterizedTypeName.get(
                                        ClassName.get(gov.nasa.jpl.aerie.merlin.framework.Context.class),
                                        TypeVariableName.get("$Schema"))),
                                "context")
                            .addModifiers(Modifier.FINAL)
                            .build())
                    .addStatement(
                        "super($L)",
                        "context")
                    .addStatement(
                        "this.$L = $L",
                        "context",
                        "context")
                    .build())
            .addMethod(
                MethodSpec
                    .constructorBuilder()
                    .addModifiers(Modifier.PROTECTED)
                    .addStatement(
                        "this($T.create())",
                        gov.nasa.jpl.aerie.merlin.framework.Scoped.class)
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("run")
                    .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                    .returns(TypeName.VOID)
                    .addParameter(
                        ParameterSpec
                            .builder(
                                ParameterizedTypeName.get(
                                    ClassName.get(adaptation.topLevelModel),
                                    TypeVariableName.get("$Schema")),
                                "model")
                            .build())
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("runWith")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(TypeName.VOID)
                    .addParameter(
                        ParameterSpec
                            .builder(
                                ParameterizedTypeName.get(
                                    ClassName.get(gov.nasa.jpl.aerie.merlin.framework.Context.class),
                                    TypeVariableName.get("$Schema")),
                                "context")
                            .build())
                    .addParameter(
                        ParameterSpec
                            .builder(
                                ParameterizedTypeName.get(
                                    ClassName.get(adaptation.topLevelModel),
                                    TypeVariableName.get("$Schema")),
                                "model")
                            .build())
                    .addStatement(
                        "this.$L.setWithin($L, () -> this.run($L))",
                        "context",
                        "context",
                        "model")
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }

  private JavaFile generateAdaptationFactory(final AdaptationRecord adaptation) {
    final var typeName = adaptation.getFactoryName();

    final var typeSpec =
        TypeSpec
            .classBuilder(typeName)
            .addAnnotation(
                AnnotationSpec
                    .builder(javax.annotation.processing.Generated.class)
                    .addMember("value", "$S", AdaptationProcessor.class.getCanonicalName())
                    .build())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(gov.nasa.jpl.aerie.merlin.protocol.AdaptationFactory.class)
            .addMethod(
                MethodSpec
                    .methodBuilder("instantiate")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.Adaptation.class),
                            WildcardTypeName.get(this.typeUtils.getWildcardType(null, null))))
                    .addStatement(
                        "return this.instantiate($T.builder())",
                        gov.nasa.jpl.aerie.merlin.timeline.Schema.class)
                    .build())
            .addMethod(
                MethodSpec
                    .methodBuilder("instantiate")
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariable(TypeVariableName.get("$Schema"))
                    .returns(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.protocol.Adaptation.class),
                            TypeVariableName.get("$Schema")))
                    .addParameter(
                        ParameterizedTypeName.get(
                            ClassName.get(gov.nasa.jpl.aerie.merlin.timeline.Schema.Builder.class),
                            TypeVariableName.get("$Schema")),
                        "schemaBuilder",
                        Modifier.FINAL)
                    .addStatement(
                        "final var $L = new $T<>($L)",
                        "builder",
                        gov.nasa.jpl.aerie.merlin.framework.AdaptationBuilder.class,
                        "schemaBuilder")
                    .addStatement(
                        "final var $L = new $T<>($L.getRegistrar())",
                        "model",
                        ClassName.get(adaptation.topLevelModel),
                        "builder")
                    .addStatement(
                        "$T.register($L, $L)",
                        adaptation.getMasterActivityTypesName(),
                        "builder",
                        "model")
                    .addCode("\n")
                    .addStatement(
                        "return $L.build()",
                        "builder")
                    .build())
            .build();

    return JavaFile
        .builder(typeName.packageName(), typeSpec)
        .skipJavaLangImports(true)
        .build();
  }


  private List<AnnotationMirror> getRepeatableAnnotation(final Element element, final Class<? extends Annotation> annotationClass) {
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
  getAnnotationAttribute(final AnnotationMirror annotationMirror, final String attributeName) {
    for (final var entry : annotationMirror.getElementValues().entrySet()) {
      if (Objects.equals(attributeName, entry.getKey().getSimpleName().toString())) {
        return Optional.of(entry.getValue());
      }
    }

    return Optional.empty();
  }

  private Optional<AnnotationMirror>
  getAnnotationMirrorByType(final Element element, final Class<? extends Annotation> annotationClass) {
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
