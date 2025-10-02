package io.byteforge.skybuddy.core;

import io.byteforge.skybuddy.api.SkyBuddy;
import net.fabricmc.api.ClientModInitializer;

public class DefaultSkyBuddy extends SkyBuddy implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    LOGGER.info("Initializing SkyBuddy Core Module");
  }
}
