package io.byteforge.skybuddy.processor.util;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;

public class ProcessorUtil {

  public static FileObject createFile(
      ProcessingEnvironment processingEnv,
      String path
  ) throws IOException {
    return processingEnv.getFiler().createResource(
        StandardLocation.CLASS_OUTPUT,
        "",
        path
    );
  }

  public static FileObject writeFile(
      ProcessingEnvironment processingEnv,
      String path,
      String content
  ) throws IOException {
    FileObject file = createFile(processingEnv, path);
    try (Writer writer = file.openWriter()) {
      writer.write(content);
    }

    return file;
  }

  public static FileObject createEmptyFile(
      ProcessingEnvironment processingEnv,
      String path
  ) throws IOException {
    return writeFile(processingEnv, path, "");
  }
}
