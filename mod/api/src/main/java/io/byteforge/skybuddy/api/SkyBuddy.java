package io.byteforge.skybuddy.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public abstract class SkyBuddy {

  public static final Logger LOGGER = LoggerFactory.getLogger(SkyBuddy.class);

  public static boolean isDevelopmentEnvironment() {
    return Objects.equals(System.getProperty("io.byteforge.skybuddy.development"), "true");
  }
}
