package io.byteforge.skybuddy.api.util;

import io.byteforge.skybuddy.api.SkyBuddy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class JavaResourceLoader {

  private JavaResourceLoader() {
    // Private constructor to prevent instantiation
  }

  public static List<Path> listAllFilesFromResources(Object instance, String path)
      throws URISyntaxException,
      IOException {
    if (!SkyBuddy.isDevelopmentEnvironment()) {
      return List.of();
    }

    // Use getResources() to find all instances of the resource path on the classpath
    Enumeration<URL> dirURLs = instance.getClass().getClassLoader().getResources(path);

    List<Path> paths = new ArrayList<>();
    while (dirURLs.hasMoreElements()) {
      URL url = dirURLs.nextElement();
      if (url.getProtocol().equals("file")) {
        Path dirPath = Paths.get(url.toURI());
        try (Stream<Path> filesInDir = Files.list(dirPath)) {
          paths.addAll(filesInDir.filter(Files::isRegularFile).toList());
        }
      }
    }

    return paths;
  }

  public static List<String> readAllFilesFromResources(
      Object instance,
      String path
  ) throws URISyntaxException, IOException {
    // Load resources from files, used in dev environment
    if (SkyBuddy.isDevelopmentEnvironment()) {
      List<Path> paths = listAllFilesFromResources(instance, path);
      List<String> result = new ArrayList<>();
      for (Path referencesPath : paths) {
        result.add(Files.readString(referencesPath));
      }

      return result;
    }

    URL jarUrl = instance.getClass().getProtectionDomain().getCodeSource().getLocation();
    String jarPath = URLDecoder.decode(jarUrl.getFile(), StandardCharsets.UTF_8);
    JarFile jar = new JarFile(new File(jarPath));

    List<String> result = new ArrayList<>();
    for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
      JarEntry entry = entries.nextElement();
      String name = entry.getName();
      if (name.startsWith(path) && !entry.isDirectory()) {
        // load the file content
        try (InputStream inputStream = jar.getInputStream(entry)) {
          if (inputStream == null) {
            SkyBuddy.LOGGER.warn("Could not find resource: {}", name);
            continue;
          }

          String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
          result.add(content);
        } catch (IOException e) {
          SkyBuddy.LOGGER.error("Failed to read resource: {}", name, e);
        }
      }
    }

    jar.close();
    return result;
  }
}
