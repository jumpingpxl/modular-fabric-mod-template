package io.byteforge.skybuddy.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

public abstract class MultiAnnotationProcessor extends AbstractProcessor {

  private final Set<String> supportedAnnotations;

  protected MultiAnnotationProcessor(String canonicalAnnotationName) {
    this.supportedAnnotations = Set.of(canonicalAnnotationName);
  }

  protected MultiAnnotationProcessor(
      String firstCanonicalAnnotationName,
      String... canonicalAnnotationNames
  ) {
    String[] allAnnotations = new String[canonicalAnnotationNames.length + 1];
    allAnnotations[0] = firstCanonicalAnnotationName;
    System.arraycopy(canonicalAnnotationNames, 0, allAnnotations, 1,
        canonicalAnnotationNames.length);
    this.supportedAnnotations = Set.of(allAnnotations);
  }

  protected MultiAnnotationProcessor(Class<? extends Annotation> annotation) {
    this.supportedAnnotations = Set.of(annotation.getCanonicalName());
  }

  protected MultiAnnotationProcessor(
      Class<? extends Annotation> firstAnnotation,
      Class<? extends Annotation>... annotations
  ) {
    String[] allAnnotations = new String[annotations.length + 1];
    allAnnotations[0] = firstAnnotation.getCanonicalName();
    for (int i = 0; i < annotations.length; i++) {
      allAnnotations[i + 1] = annotations[i].getCanonicalName();
    }

    this.supportedAnnotations = Set.of(allAnnotations);
  }

  @Override
  public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      this.processingFinished(roundEnv);
      return true;
    }

    this.process(roundEnv);
    return true;
  }

  protected abstract void process(RoundEnvironment roundEnv);

  protected abstract void processingFinished(RoundEnvironment roundEnv);

  @Override
  public Set<String> getSupportedOptions() {
    Set<String> supportedOptions = new HashSet<>(super.getSupportedOptions());
    supportedOptions.add("moduleName");
    supportedOptions.add("projectId");
    return supportedOptions;
  }

  @Override
  public final SourceVersion getSupportedSourceVersion() {
    SupportedSourceVersion ssv = this.getClass().getAnnotation(SupportedSourceVersion.class);
    if (ssv != null) {
      return ssv.value();
    }

    return SourceVersion.RELEASE_21;
  }

  protected final String getModuleName() {
    String moduleName = this.processingEnv.getOptions().get("moduleName");
    if (moduleName == null) {
      this.printError("Module name not specified. Please provide a moduleName option.");
      throw new IllegalStateException("Module name not specified");
    }

    return moduleName;
  }

  @Override
  public final Set<String> getSupportedAnnotationTypes() {
    return this.supportedAnnotations;
  }

  protected final String getProjectId() {
    String projectId = this.processingEnv.getOptions().get("projectId");
    if (projectId == null) {
      this.printError("Project ID not specified. Please provide a projectId option.");
      throw new IllegalStateException("Project ID not specified");
    }

    return projectId;
  }

  protected void printInfo(String message) {
    this.processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
  }

  protected void printWarning(String message) {
    this.processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message);
  }

  protected void printError(String message) {
    this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
  }
}
