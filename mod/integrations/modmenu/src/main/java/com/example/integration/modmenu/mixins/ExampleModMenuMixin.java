package com.example.integration.modmenu.mixins;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ExampleModMenuMixin {

  @Inject(
      method = "tick",
      at = @At("HEAD")
  )
  public void exampleTick(CallbackInfo ci) {
    com.example.api.ExampleMod.LOGGER.info("ExampleModMenuMixin tick");
  }
}
