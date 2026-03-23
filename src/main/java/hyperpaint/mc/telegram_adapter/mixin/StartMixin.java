package hyperpaint.mc.telegram_adapter.mixin;

import hyperpaint.mc.telegram_adapter.TelegramAdapter;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class StartMixin {
    @Inject(method = "main", at = @At("HEAD"))
    private static void initialize(String[] args, CallbackInfo callback) {
        TelegramAdapter.initialize();
    }
}
