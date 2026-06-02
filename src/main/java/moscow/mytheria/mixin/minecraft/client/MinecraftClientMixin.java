/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_542
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package moscow.mytheria.mixin.minecraft.client;

import moscow.mytheria.Mytheria;
import moscow.mytheria.protection.client.MinecraftClientMixinProtection;
import moscow.mytheria.systems.event.impl.game.GameTickEvent;
import moscow.mytheria.ui.mainmenu.CustomTitleScreen;
import moscow.mytheria.utility.render.penis.PenisAtlas;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_542;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_310.class})
public class MinecraftClientMixin {
    @Shadow
    private int field_1752;

    @Inject(method={"method_1574()V"}, at={@At(value="HEAD")})
    public void tick(CallbackInfo ci) {
        Mytheria.getInstance().getEventManager().triggerEvent(new GameTickEvent());
    }

    @Inject(method={"method_1507(Lnet/minecraft/class_437;)V"}, at={@At(value="HEAD")}, cancellable=true)
    public void replaceVanillaTitleScreen(class_437 screen, CallbackInfo ci) {
        if (screen instanceof class_442 && !(screen instanceof CustomTitleScreen) && !Mytheria.INSTANCE.isPanic()) {
            ci.cancel();
            ((class_310)(Object)this).method_1507((class_437)new CustomTitleScreen());
        }
    }

    @Inject(method={"<init>(Lnet/minecraft/class_542;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_310;method_15993()V")})
    public void initializeClient(class_542 args, CallbackInfo ci) {
        MinecraftClientMixinProtection.init();
    }

    @Inject(method={"<init>(Lnet/minecraft/class_542;)V"}, at={@At(value="RETURN")})
    public void endInitialize(class_542 args, CallbackInfo ci) {
        try {
            PenisAtlas atlas = PenisAtlas.getOrCreateAtlasFor(16, 16);
            atlas.registerAnimationFromPenisFile(Mytheria.id("penises/combat.penis"));
            atlas.registerAnimationFromPenisFile(Mytheria.id("penises/movement.penis"));
            atlas.registerAnimationFromPenisFile(Mytheria.id("penises/visuals.penis"));
            atlas.registerAnimationFromPenisFile(Mytheria.id("penises/player.penis"));
            atlas.registerAnimationFromPenisFile(Mytheria.id("penises/other.penis"));
            atlas.registerAnimationFromPenisFile(Mytheria.id("penises/search.penis"));
            atlas.buildAtlas();
            PenisAtlas atlas12 = PenisAtlas.getOrCreateAtlasFor(12, 12);
            atlas12.registerAnimationFromPenisFile(Mytheria.id("penises/check_enable.penis"));
            atlas12.registerAnimationFromPenisFile(Mytheria.id("penises/check_disable.penis"));
            atlas12.buildAtlas();
        }
        catch (Exception var5) {
            System.err.println("Ошибка при загрузке анимаций: " + var5.getMessage());
            var5.printStackTrace();
        }
    }

    @Inject(method={"method_1490()V"}, at={@At(value="INVOKE", target="Lnet/minecraft/class_310;close()V", shift=At.Shift.AFTER)})
    public void shutdownClient(CallbackInfo ci) {
        MinecraftClientMixinProtection.shutdown();
    }

    @Inject(method={"method_24287()Ljava/lang/String;"}, at={@At(value="HEAD")}, cancellable=true)
    public void changeWindowTitle(CallbackInfoReturnable<String> cir) {
        MinecraftClientMixinProtection.updateTitle(cir);
    }
}
