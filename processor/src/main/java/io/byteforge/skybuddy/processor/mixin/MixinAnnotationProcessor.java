package io.byteforge.skybuddy.processor.mixin;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.byteforge.skybuddy.processor.SingleAnnotationProcessor;
import io.byteforge.skybuddy.processor.util.ProcessorUtil;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
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
public class MixinAnnotationProcessor extends SingleAnnotationProcessor {

  private final List<MixinClass> mixinClasses = new ArrayList<>();

  public MixinAnnotationProcessor() {
    super(Mixin.class);
  }

  @Override
  protected void process(RoundEnvironment roundEnv, Set<? extends Element> annotatedElements) {
    // log info
    this.printInfo(
        "Mixin Annotation Processor is running for module " + this.getModuleName() + "..."
    );

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

  @Override
  protected void processingFinished(RoundEnvironment roundEnv) {
    String moduleName = this.getModuleName();
    if (this.mixinClasses.isEmpty()) {
      return;
    }

    String projectId = this.getProjectId();
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

      // search for *.refmap.json file in classesDir
      try (Stream<Path> stream = Files.list(classesDir)) {
        Optional<Path> first = stream.filter(
            file -> file.getFileName().toString().endsWith(".refmap.json")
        ).findFirst();

        String refmapName;
        if (first.isPresent()) {
          refmapName = first.get().getFileName().toString();
          this.printInfo("Found refmap file name: " + refmapName);
        } else {
          refmapName = projectId + "-" + moduleName + ".refmap.json";
          this.printWarning("No refmap file found, defaulting to: " + refmapName);
        }

        mixinConfig.addProperty("refmap", refmapName);
      }

      // Write the mixin config to a file
      ProcessorUtil.writeFile(
          this.processingEnv,
          fileName,
          gson.toJson(mixinConfig)
      );

      this.printInfo(
          "Wrote mixin file " + fileName + " with " + this.mixinClasses.size() + " mixins."
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

      if (!commonPackage.isEmpty()) {
        commonPackage.append(".");
      }

      commonPackage.append(part);
    }

    return commonPackage.toString();
  }

  private record MixinClass(String className, String qualifiedName, String packageName) {

  }
}
