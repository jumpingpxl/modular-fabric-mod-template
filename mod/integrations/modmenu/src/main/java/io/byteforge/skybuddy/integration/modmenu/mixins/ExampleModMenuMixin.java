package io.byteforge.skybuddy.integration.modmenu.mixins;

import io.byteforge.skybuddy.api.SkyBuddy;
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
    SkyBuddy.LOGGER.info("ExampleModMenuMixin tick");
  }
}
