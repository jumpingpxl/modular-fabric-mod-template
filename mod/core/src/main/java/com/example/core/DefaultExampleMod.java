package com.example.core;

import com.example.api.ExampleMod;
import net.fabricmc.api.ClientModInitializer;

public class DefaultExampleMod extends ExampleMod implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    LOGGER.info("Hello Fabric world!");
  }
}
