package sh.creepercdn.dashloadingscreen.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sh.creepercdn.dashloadingscreen.DashScreen;

import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public abstract class LoadScreenMixin extends Overlay {
    protected DashScreen dash = new DashScreen();
    @Shadow
    private float progress;
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    @Final
    private ResourceReload reload;

    @Inject(at = @At("RETURN"), method = "<init>")
    protected void onInitClass(MinecraftClient client, ResourceReload monitor, Consumer exceptionHandler, boolean reloading, CallbackInfo ci) {
        dash.init(client, this.client.getWindow().getScaledWidth(), this.client.getWindow().getScaledHeight());
        dash.updateConfig();
    }

    @Inject(at = @At("HEAD"), method = "render", cancellable = true)
    protected void onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.progress = MathHelper.clamp(this.progress * 0.95F + this.reload.getProgress() * 0.05F, 0.0F, 1.0F);
        dash.progress = this.progress;
        if (this.reload.isComplete()) {
            this.client.setOverlay(null);
            dash.STATUS = DashScreen.Status.DONE;
        }
        dash.render(matrices, mouseX, mouseY, delta);
        ci.cancel();
    }
}