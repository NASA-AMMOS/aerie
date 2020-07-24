package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.security.auth.login.Configuration.Parameters;
import javax.tools.JavaFileObject;
import javax.tools.Diagnostic.Kind;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.MerlinEvent;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.AbstractProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.effects.EffectTrait;

@SupportedAnnotationTypes({ "gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations.MerlinEvent" })
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class EventProcessor extends AbstractProcessor {

  private Filer filer;
  private Messager messager;

  @Override
  public void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
  }

  static String unqualified(final String input) {
    return input.substring(input.lastIndexOf(".") + 1, input.length());
  }

  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    for (final Element element : roundEnv.getElementsAnnotatedWith(MerlinEvent.class)) {

      if (element.getKind() != ElementKind.INTERFACE) {
        this.processingEnv.getMessager().printMessage(Kind.ERROR,
            "The MerlinEvent annotation may only be used on interfaces.", element);
        return true;
      }

      // Preprocess the element.
      final TypeElement typeElement = (TypeElement) element;
      final String qualifiedName = typeElement.getQualifiedName().toString();
      final String packageName = (qualifiedName.lastIndexOf(".") == -1) ? ""
          : qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
      final String baseName = unqualified(qualifiedName).substring(0,
          unqualified(qualifiedName).lastIndexOf("EventHandler"));
      final String qualifiedBaseName = (packageName == "") ? baseName : packageName + "." + baseName;

      // Create TypeVariables for classes
      final TypeVariableName resultType = TypeVariableName.get("Result");
      final TypeVariableName eventType = TypeVariableName.get(baseName + "Event");
      final TypeVariableName effectType = TypeVariableName.get("Effect");
      final ParameterizedTypeName eventHandlerType = ParameterizedTypeName
          .get(ClassName.get(packageName, unqualified(typeElement.getSimpleName().toString())), resultType);

      // Get members of type
      final List<MethodSpec> eventMethods = new ArrayList<MethodSpec>();
      final List<MethodSpec> defaultHandlerMethods = new ArrayList<MethodSpec>();
      for (final Element member : element.getEnclosedElements()) {
        if (member.getKind() != ElementKind.METHOD) {
          this.processingEnv.getMessager().printMessage(Kind.ERROR,
              "MerlinEvent's may only include members who are of kind method.", member);
        }

        // We know that the member is of type ExecutableElement because it is of kind
        // METHOD.
        final ExecutableElement executable = (ExecutableElement) member;

        // Build list of parameters
        final List<ParameterSpec> params = new ArrayList<ParameterSpec>();
        final List<String> paramStrings = new ArrayList<String>();

        for (final VariableElement var : executable.getParameters()) {
          params.add(ParameterSpec.builder(TypeName.get(var.asType()), var.toString(), Modifier.FINAL).build());
          paramStrings.add(var.toString());
        }

        // Build the methods for each output class using java poet
        final MethodSpec eventMethod = MethodSpec.methodBuilder(executable.getSimpleName().toString())
            .returns(eventType).addParameters(params)
            .addCode(
                "return new $T() {\n"
                + "  @$T\n"
                + "  public <Result> Result visit(final $T visitor) {\n"
                + "    return visitor.$L($L);\n"
                + "  }\n"
                + "};\n",
                eventType, Override.class, eventHandlerType, executable.getSimpleName().toString(),
                String.join(",", paramStrings))
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC).build();
        eventMethods.add(eventMethod);

        final MethodSpec eventHandlerMethod = MethodSpec.methodBuilder(executable.getSimpleName().toString())
            .returns(resultType).addParameters(params).addAnnotation(Override.class)
            .addModifiers(Modifier.DEFAULT, Modifier.PUBLIC).addCode("return this.unhandled();").build();
        defaultHandlerMethods.add(eventHandlerMethod);
      }

      // Build Event class.
      final MethodSpec constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build();
      final MethodSpec visit = MethodSpec.methodBuilder("visit").addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .addTypeVariable(resultType).returns(resultType).addParameter(eventHandlerType, "visitor").build();

      final TypeSpec event = TypeSpec.classBuilder(baseName + "Event").addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
          .addMethods(eventMethods).addMethod(constructor).addMethod(visit).build();

      // Build DefaultEventHandler class.
      final MethodSpec unhandled = MethodSpec.methodBuilder("unhandled").returns(resultType)
          .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC).build();

      final TypeSpec defaultEventHandler = TypeSpec.interfaceBuilder(baseName + "DefaultEventHandler")
          .addSuperinterface(eventHandlerType).addModifiers(Modifier.PUBLIC).addMethods(defaultHandlerMethods)
          .addMethod(unhandled).addTypeVariable(resultType).build();

      // Build EventProjection class.
      final MethodSpec projectionConstructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
          .addCode("super(trait);").addParameter(
              ParameterizedTypeName.get(ClassName.get(EffectTrait.class), effectType), "trait", Modifier.FINAL)
          .build();
      final MethodSpec unhandledProjection = MethodSpec.methodBuilder("unhandled").returns(effectType)
          .addCode("return this.empty();").addModifiers(Modifier.FINAL, Modifier.PUBLIC).addAnnotation(Override.class)
          .build();
      final MethodSpec atomProjection = MethodSpec.methodBuilder("atom").returns(effectType)
          .addCode("return atom.visit(this);").addModifiers(Modifier.FINAL, Modifier.PUBLIC)
          .addParameter(eventType, "atom", Modifier.FINAL).addAnnotation(Override.class).build();

      final TypeSpec eventProjection = TypeSpec.classBuilder(baseName + "EventProjection")
          .addMethod(projectionConstructor).addMethod(atomProjection).addMethod(unhandledProjection)
          .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
          .superclass(ParameterizedTypeName.get(ClassName.get(AbstractProjection.class), eventType, effectType))
          .addSuperinterface(
              ParameterizedTypeName.get(ClassName.get(packageName, baseName + "DefaultEventHandler"), effectType))
          .addTypeVariable(effectType).build();
      try {
        // Create Event, DefaultEventHandler, and Event Projection writers
        final JavaFileObject jfoEvent = this.processingEnv.getFiler().createSourceFile(qualifiedBaseName + "Event");
        final Writer writerEvent = jfoEvent.openWriter();

        final JavaFileObject jfoEventHandler = this.processingEnv.getFiler()
            .createSourceFile(qualifiedBaseName + "DefaultEventHandler");
        final Writer writerEventHandler = jfoEventHandler.openWriter();

        final JavaFileObject jfoEventProjection = this.processingEnv.getFiler()
            .createSourceFile(qualifiedBaseName + "EventProjection");
        final Writer writerEventProjection = jfoEventProjection.openWriter();

        // Write the classes to their files.
        final JavaFile jfDefaultHandler = JavaFile.builder(packageName, defaultEventHandler).build();
        jfDefaultHandler.writeTo(writerEventHandler);
        writerEventHandler.close();

        final JavaFile jfEvent = JavaFile.builder(packageName, event).build();
        jfEvent.writeTo(writerEvent);
        writerEvent.close();

        final JavaFile jfEventProjection = JavaFile.builder(packageName, eventProjection).build();
        jfEventProjection.writeTo(writerEventProjection);
        writerEventProjection.close();
      } catch (final IOException e) {
        messager.printMessage(Kind.ERROR,
            "Failed to open or write to the files for " + typeElement.getQualifiedName() + ":\n" + e.toString());
        return true;
      }
    }
    return true;
  }
}
