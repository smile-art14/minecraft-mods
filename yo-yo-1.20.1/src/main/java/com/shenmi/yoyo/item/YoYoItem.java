package com.shenmi.yoyo.item;

import com.shenmi.yoyo.YoYoMod;
import com.shenmi.yoyo.api.PlayerStandingState;
import com.shenmi.yoyo.combat.StrangleManager;
import com.shenmi.yoyo.entity.YoYoProjectileEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class YoYoItem extends SwordItem {
    private static final double STRANGLE_RANGE_SQR = 3.25D * 3.25D;

    public YoYoItem(Properties properties) {
        super(Tiers.IRON, 3, -2.15F, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.shenmi_yoyo.melee").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.shenmi_yoyo.throw").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.shenmi_yoyo.strangle").withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.shenmi_yoyo.stand").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.shenmi_yoyo.mount").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            YoYoProjectileEntity projectile = new YoYoProjectileEntity(level, player);
            projectile.setItem(stack.copyWithCount(1));
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.55F, 0.05F);
            level.addFreshEntity(projectile);

            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_THROW,
                    SoundSource.PLAYERS,
                    0.65F,
                    1.35F
            );

            player.getCooldowns().addCooldown(this, 8);
            stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (target == player) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            return tryStrangle(stack, player, target, hand);
        }

        if (target instanceof Player otherPlayer) {
            return player.isSprinting()
                    ? tryMountPlayer(player, otherPlayer)
                    : tryStandOnPlayer(player, otherPlayer);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult tryStrangle(
            ItemStack stack,
            Player attacker,
            LivingEntity target,
            InteractionHand hand
    ) {
        Level level = attacker.level();
        if (attacker.distanceToSqr(target) > STRANGLE_RANGE_SQR) {
            if (!level.isClientSide) {
                attacker.displayClientMessage(
                        Component.translatable("message.shenmi_yoyo.too_far"),
                        true
                );
            }
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        boolean started = StrangleManager.begin(attacker, target);
        if (!started) {
            return InteractionResult.FAIL;
        }

        attacker.getCooldowns().addCooldown(this, 40);
        stack.hurtAndBreak(3, attacker, broken -> broken.broadcastBreakEvent(hand));
        return InteractionResult.CONSUME;
    }

    private InteractionResult tryStandOnPlayer(Player rider, Player target) {
        Level level = rider.level();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        int currentTargetId = PlayerStandingState.getStandingTargetId(rider);
        if (PlayerStandingState.isStandingOnPlayer(rider)
                && currentTargetId == target.getId()) {
            PlayerStandingState.stopStanding(rider);
            rider.displayClientMessage(
                    Component.translatable("message.shenmi_yoyo.stand.dismount"),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (rider.isPassenger()
                || target.isPassengerOfSameVehicle(rider)
                || rider.isPassengerOfSameVehicle(target)) {
            rider.displayClientMessage(
                    Component.translatable("message.shenmi_yoyo.stand.failed"),
                    true
            );
            return InteractionResult.FAIL;
        }

        PlayerStandingState.startStanding(rider, target.getId());
        rider.setDeltaMovement(0.0D, 0.0D, 0.0D);
        rider.setPos(
                target.getX(),
                target.getY() + target.getBbHeight() + 0.04D,
                target.getZ()
        );
        rider.fallDistance = 0.0F;
        rider.displayClientMessage(
                Component.translatable("message.shenmi_yoyo.stand.success", target.getDisplayName()),
                true
        );
        return InteractionResult.CONSUME;
    }

    private InteractionResult tryMountPlayer(Player rider, Player target) {
        Level level = rider.level();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        int currentTargetId = PlayerStandingState.getStandingTargetId(rider);
        if (PlayerStandingState.isSittingOnPlayer(rider)
                && currentTargetId == target.getId()) {
            PlayerStandingState.stopStanding(rider);
            rider.displayClientMessage(
                    Component.translatable("message.shenmi_yoyo.dismount"),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (rider.isPassenger()
                || target.isPassengerOfSameVehicle(rider)
                || rider.isPassengerOfSameVehicle(target)
                || (PlayerStandingState.isAttachedToPlayer(target)
                && PlayerStandingState.getStandingTargetId(target) == rider.getId())) {
            rider.displayClientMessage(
                    Component.translatable("message.shenmi_yoyo.mount.failed"),
                    true
            );
            return InteractionResult.FAIL;
        }

        PlayerStandingState.startSitting(rider, target.getId());
        rider.setDeltaMovement(0.0D, 0.0D, 0.0D);
        rider.setPos(
                target.getX(),
                target.getY() + target.getBbHeight() - 0.70D,
                target.getZ()
        );
        rider.fallDistance = 0.0F;
        rider.displayClientMessage(
                Component.translatable("message.shenmi_yoyo.mount.success", target.getDisplayName()),
                true
        );
        return InteractionResult.CONSUME;
    }
}
