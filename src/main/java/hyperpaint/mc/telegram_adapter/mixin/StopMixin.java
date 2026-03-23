package hyperpaint.mc.telegram_adapter.mixin;

import hyperpaint.mc.telegram_adapter.TelegramAdapter;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class StopMixin {
    @Inject(method = "stop", at = @At("TAIL"))
    private void terminate(CallbackInfo callback) {
        TelegramAdapter.terminate();
    }
}
