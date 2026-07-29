package com.example.examplemod.core;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ClientModInitializer {

  public static final Logger LOGGER = LoggerFactory.getLogger(ExampleMod.class);

  @Override
  public void onInitializeClient() {
    LOGGER.info("Hello Fabric world!");
  }
}
