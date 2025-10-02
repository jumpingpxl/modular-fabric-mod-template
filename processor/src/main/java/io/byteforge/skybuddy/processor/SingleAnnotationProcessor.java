package io.byteforge.skybuddy.processor;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Set;

public abstract class SingleAnnotationProcessor extends MultiAnnotationProcessor {

  private final String canonicalAnnotationName;
  private Class<? extends Annotation> annotationClass;

  protected SingleAnnotationProcessor(String canonicalAnnotationName) {
    super(canonicalAnnotationName);
    this.canonicalAnnotationName = canonicalAnnotationName;
  }

  protected SingleAnnotationProcessor(Class<? extends Annotation> annotation) {
    super(annotation);
    this.canonicalAnnotationName = annotation.getCanonicalName();
    this.annotationClass = annotation;
  }

  @Override
  protected final void process(RoundEnvironment roundEnv) {
    if (this.annotationClass == null) {
      try {
        this.annotationClass = (Class<? extends Annotation>) Class.forName(
            this.canonicalAnnotationName
        );
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    this.process(
        roundEnv,
        roundEnv.getElementsAnnotatedWith(this.annotationClass)
    );
  }

  protected abstract void process(
      RoundEnvironment roundEnv,
      Set<? extends Element> annotatedElements
  );
}
