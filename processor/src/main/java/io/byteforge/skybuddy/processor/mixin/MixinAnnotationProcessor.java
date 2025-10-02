package io.byteforge.skybuddy.processor.mixin;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.byteforge.skybuddy.processor.util.ProcessorUtil;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class MixinAnnotationProcessor extends AbstractProcessor {

  private final List<MixinClass> mixinClasses = new ArrayList<>();

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    String moduleName = this.processingEnv.getOptions().get("moduleName");
    if (roundEnv.processingOver()) {
      this.processingEnv.getMessager().printMessage(
          Diagnostic.Kind.NOTE,
          "SkyBuddy Mixin Annotation Processor found " + this.mixinClasses.size() + " "
              + "mixins for module " + moduleName + "..."
      );

      this.save(moduleName);
      return true;
    }

    // log info
    this.processingEnv.getMessager().printMessage(
        Diagnostic.Kind.NOTE,
        "SkyBuddy Mixin Annotation Processor is running for module " + moduleName + "..."
    );

    for (Element element : roundEnv.getElementsAnnotatedWith(Mixin.class)) {
      if (!(element instanceof TypeElement typeElement)) {
        this.processingEnv.getMessager().printMessage(
            Diagnostic.Kind.ERROR,
            "Mixin annotation can only be applied to classes."
        );

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

    return true;
  }

  private void save(String moduleName) {
    if (this.mixinClasses.isEmpty()) {
      return;
    }

    String projectId = this.processingEnv.getOptions().get("projectId");
    Gson gson = new Gson();
    String fileName = projectId + "-" + moduleName + ".mixins.json";
    String commonTopLevelPackage = this.findCommonTopLevelPackage();
    try {
      // Create index file to dynamically load the configuration later
      FileObject indexFile = ProcessorUtil.createEmptyFile(
          this.processingEnv,
          "META-INF/mixins/" + fileName
      );

      // Get classes output directory (step up 3 levels from the index file)
      Path classesDir = Path.of(indexFile.getName()).getParent().getParent().getParent();

      // Load default mixin config
      JsonObject mixinConfig = this.loadDefaultMixinConfig(gson);

      // Set the package name
      mixinConfig.addProperty("package", commonTopLevelPackage);

      // Add mixin classes
      JsonArray mixinArray = new JsonArray();
      for (MixinClass mixinClass : this.mixinClasses) {
        if (!mixinClass.packageName.startsWith(commonTopLevelPackage)) {
          this.processingEnv.getMessager().printMessage(
              Diagnostic.Kind.WARNING,
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

      // search for *.refmap.json file in classesDir
      try (Stream<Path> stream = Files.list(classesDir)) {
        Optional<Path> first = stream.filter(
            file -> file.getFileName().toString().endsWith(".refmap.json")).findFirst();
        String refmapName;
        if (first.isPresent()) {
          refmapName = first.get().getFileName().toString();
          this.processingEnv.getMessager().printMessage(
              Diagnostic.Kind.NOTE,
              "Found refmap file name: " + refmapName
          );
        } else {
          refmapName = projectId + "-" + moduleName + ".refmap.json";
          this.processingEnv.getMessager().printMessage(
              Diagnostic.Kind.WARNING,
              "No refmap file found, defaulting to: " + refmapName
          );
        }

        mixinConfig.addProperty("refmap", refmapName);
      }

      // Write the mixin config to a file
      ProcessorUtil.writeFile(
          this.processingEnv,
          fileName,
          gson.toJson(mixinConfig)
      );

      this.processingEnv.getMessager().printMessage(
          Diagnostic.Kind.NOTE,
          "Wrote mixin file: " + fileName + " with "
              + this.mixinClasses.size() + " mixins."
      );
    } catch (IOException e) {
      this.processingEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Failed to write mixin file: " + fileName + " - " + e.getMessage()
      );
    }
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return Set.of(
        Mixin.class.getCanonicalName()
    );
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

  private String findCommonTopLevelPackage() {
    String[] baseParts = this.mixinClasses.get(0).packageName.split("\\.");
    StringBuilder commonPackage = new StringBuilder();

    for (int i = 0; i < baseParts.length; i++) {
      String part = baseParts[i];
      for (MixinClass cls : this.mixinClasses) {
        String[] parts = cls.packageName.split("\\.");
        if (i >= parts.length || !parts[i].equals(part)) {
          return commonPackage.toString();
        }
      }
      if (commonPackage.length() > 0) {
        commonPackage.append(".");
      }

      commonPackage.append(part);
    }

    return commonPackage.toString();
  }

  private record MixinClass(String className, String qualifiedName, String packageName) {

  }
}
