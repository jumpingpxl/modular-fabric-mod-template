package modularmodtemplate.processor.mixin;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.Runtime.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
@SupportedOptions({"moduleName", "projectId", "javaVersion"})
public class MixinAnnotationProcessor extends AbstractProcessor {

  private final List<MixinClass> mixinClasses = new ArrayList<>();

  private void process(RoundEnvironment roundEnv, Set<? extends Element> annotatedElements) {
    for (Element element : annotatedElements) {
      if (!(element instanceof TypeElement typeElement)) {
        this.printWarning("Mixin annotation can only be applied to classes.");
        continue;
      }

      Mixin mixinAnnotation = typeElement.getAnnotation(Mixin.class);
      if (mixinAnnotation == null) {
        continue; // Not a mixin class
      }

      String className = typeElement.getSimpleName().toString();
      String qualifiedName = typeElement.getQualifiedName().toString();
      String packageName = qualifiedName.substring(
          0,
          qualifiedName.length() - (className.length() + 1)
      );

      this.mixinClasses.add(new MixinClass(className, qualifiedName, packageName));
    }
  }

  private void processingFinished(RoundEnvironment roundEnv) {
    String moduleName = this.getModuleName();
    if (this.mixinClasses.isEmpty()) {
      return;
    }

    String projectId = this.getProjectId();
    Gson gson = new Gson();
    String fileName = projectId + "-" + moduleName + ".mixins.json";
    String commonTopLevelPackage = this.findCommonTopLevelPackage();
    try {
      // Create index file to dynamically load the configuration in development env
      this.createEmptyFile(
          this.processingEnv,
          "META-INF/mixins/" + fileName
      );

      // Load default mixin config
      JsonObject mixinConfig = this.loadDefaultMixinConfig(gson);

      // Set the package name
      mixinConfig.addProperty("package", commonTopLevelPackage);

      // Set the java version
      mixinConfig.addProperty("compatibilityLevel", "JAVA_" + this.getJavaVersion());

      // Add mixin classes
      JsonArray mixinArray = new JsonArray();
      for (MixinClass mixinClass : this.mixinClasses) {
        if (!mixinClass.packageName.startsWith(commonTopLevelPackage)) {
          this.printWarning(
              "Skipping mixin class outside of common top-level package: "
                  + mixinClass.qualifiedName
          );
          continue;
        }

        mixinArray.add(mixinClass.qualifiedName.substring(
            commonTopLevelPackage.length() + 1
        ));
      }

      mixinConfig.add("mixins", mixinArray);

      // Write the mixin config to a file
      this.writeFile(
          this.processingEnv,
          fileName,
          gson.toJson(mixinConfig)
      );
    } catch (IOException e) {
      this.printError("Failed to write mixin file " + fileName + " - " + e.getMessage());
    }
  }

  private JsonObject loadDefaultMixinConfig(Gson gson) throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    try (InputStream stream = classLoader.getResourceAsStream("default.mixins.json")) {
      if (stream == null) {
        throw new IOException("Default mixin config not found in resources.");
      }

      return gson.fromJson(new InputStreamReader(stream), JsonObject.class);
    }
  }

  // find the highest level package containing mixins
  private String findCommonTopLevelPackage() {
    String[] baseParts = this.mixinClasses.getFirst().packageName.split("\\.");
    StringBuilder commonPackage = new StringBuilder();

    for (int i = 0; i < baseParts.length; i++) {
      String part = baseParts[i];
      for (MixinClass cls : this.mixinClasses) {
        String[] parts = cls.packageName.split("\\.");
        if (i >= parts.length || !parts[i].equals(part)) {
          return commonPackage.toString();
        }
      }

      if (!commonPackage.isEmpty()) {
        commonPackage.append(".");
      }

      commonPackage.append(part);
    }

    return commonPackage.toString();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      this.processingFinished(roundEnv);
      return true;
    }

    this.process(roundEnv, roundEnv.getElementsAnnotatedWith(Mixin.class));
    return true;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.valueOf(Version.parse(this.getJavaVersion()));
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(Mixin.class.getCanonicalName());
  }

  private String getModuleName() {
    return this.getOption("moduleName");
  }

  private String getProjectId() {
    return this.getOption("projectId");
  }

  private String getJavaVersion() {
    return this.getOption("javaVersion");
  }

  private String getOption(String key) {
    String projectId = this.processingEnv.getOptions().get(key);
    if (projectId == null) {
      this.printError("Processor Option \"" + key + "\" not specified. Please provide a "
          + "projectId option.");
      throw new IllegalStateException("Processor Option \"" + key + "\" not specified");
    }

    return projectId;
  }

  private void printWarning(String message) {
    this.processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message);
  }

  private void printError(String message) {
    this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
  }

  private FileObject createFile(
      ProcessingEnvironment processingEnv,
      String path
  ) throws IOException {
    return processingEnv.getFiler().createResource(
        StandardLocation.CLASS_OUTPUT,
        "",
        path
    );
  }

  private FileObject writeFile(
      ProcessingEnvironment processingEnv,
      String path,
      String content
  ) throws IOException {
    FileObject file = this.createFile(processingEnv, path);
    try (Writer writer = file.openWriter()) {
      writer.write(content);
    }

    return file;
  }

  private FileObject createEmptyFile(
      ProcessingEnvironment processingEnv,
      String path
  ) throws IOException {
    return this.writeFile(processingEnv, path, "");
  }

  private record MixinClass(String className, String qualifiedName, String packageName) {

  }
}
