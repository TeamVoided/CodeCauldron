package org.teamvoided.template.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.teamvoided.template.Template.log;

@Mixin(Minecraft.class)
public class ExampleMixin {

    @Inject(method = "run", at = @At("HEAD"))
    private void run(CallbackInfo info) {
        log.info("Hello from Mixin");
    }
}
