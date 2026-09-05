package com.example.mixin;

import com.example.ExampleMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin {
    @Shadow
    private ItemStack tridentItem;

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void mcdemo$hookReturn(EntityHitResult hitResult, CallbackInfo ci) {
        if (EnchantmentHelper.getItemEnchantmentLevel(ExampleMod.HOOK_RETURN, tridentItem) <= 0) {
            return;
        }

        ThrownTrident trident = (ThrownTrident) (Object) this;
        Entity owner = trident.getOwner();
        Entity hit = hitResult.getEntity();
        if (owner instanceof LivingEntity livingOwner
                && hit instanceof LivingEntity livingTarget
                && livingOwner != livingTarget
                && !livingOwner.level().isClientSide) {
            ExampleMod.startHookPull(livingOwner, livingTarget);
        }
    }
}
