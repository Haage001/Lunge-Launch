package haage.lungelaunch.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    
    @Inject(method = "attack(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
    private void onAttack(net.minecraft.entity.Entity target, CallbackInfo ci) {
        // This mixin is kept for compatibility but the main logic is in the tick event
        // to ensure proper timing with the handSwinging detection
    }
}
