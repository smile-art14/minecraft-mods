package com.example.mixin;

import com.example.ExampleMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void mcdemo$useSwallowSword(Level level, Player player, InteractionHand hand,
                                        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> callback) {
        if (hand != InteractionHand.OFF_HAND) return;
        ItemStack stack = player.getItemInHand(hand);
        if (!ExampleMod.isSwallowSword(stack)) return;

        if (ExampleMod.isSwallowCompressed(stack)) {
            ExampleMod.popSpringKnife(player, stack);
            callback.setReturnValue(InteractionResultHolder.sidedSuccess(stack, level.isClientSide()));
            return;
        }

        ExampleMod.beginSwallow(stack);
        player.startUsingItem(hand);
        callback.setReturnValue(InteractionResultHolder.consume(stack));
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void mcdemo$swallowUseAnimation(ItemStack stack, CallbackInfoReturnable<UseAnim> callback) {
        if (ExampleMod.isSwallowSword(stack)) {
            // Keep the sword as a sword: no vanilla food-consumption animation/state.
            // Eating-style particles are emitted manually from onUseTick instead.
            callback.setReturnValue(UseAnim.NONE);
        }
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void mcdemo$swallowUseDuration(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (ExampleMod.isSwallowSword(stack)) {
            callback.setReturnValue(ExampleMod.getSwallowSwordUseDuration(stack));
        }
    }

    @Inject(method = "onUseTick", at = @At("HEAD"))
    private void mcdemo$updateSwallowProgress(Level level, LivingEntity user, ItemStack stack,
                                              int remainingTicks, CallbackInfo callback) {
        if (!(user instanceof Player player)) return;
        if (player.getUsedItemHand() != InteractionHand.OFF_HAND) return;
        if (!ExampleMod.isSwallowSword(stack)) return;
        ExampleMod.updateSwallowStage(stack, remainingTicks);
        ExampleMod.emitSwallowParticles(player, stack, remainingTicks);
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void mcdemo$finishSwallow(ItemStack stack, Level level, LivingEntity user,
                                      CallbackInfoReturnable<ItemStack> callback) {
        if (!ExampleMod.isSwallowSword(stack)) return;
        if (!stack.hasTag() || !stack.getTag().getBoolean(ExampleMod.SWALLOW_ACTIVE_TAG)) return;
        ExampleMod.finishSwallow(stack);
        callback.setReturnValue(stack);
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void mcdemo$cancelSwallow(ItemStack stack, Level level, LivingEntity user,
                                      int remainingTicks, CallbackInfo callback) {
        if (ExampleMod.isSwallowSword(stack)) {
            ExampleMod.cancelSwallow(stack);
        }
    }
}
