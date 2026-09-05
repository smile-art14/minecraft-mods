package com.shenmi.yoyo.combat;

import com.shenmi.yoyo.entity.StrangleTetherEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class StrangleManager {
    private static final int DURATION_TICKS = 24;
    private static final float LETHAL_CHANCE = 0.95F;
    private static final double MAX_KEEP_RANGE_SQR = 6.0D * 6.0D;
    private static final Map<UUID, Session> BY_ATTACKER = new HashMap<>();
    private static final Map<UUID, UUID> TARGET_TO_ATTACKER = new HashMap<>();
    private static boolean initialized;

    private StrangleManager() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerTickEvents.END_SERVER_TICK.register(server -> tickAll());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            for (Session session : BY_ATTACKER.values()) {
                cleanup(session);
            }
            BY_ATTACKER.clear();
            TARGET_TO_ATTACKER.clear();
        });
    }

    public static boolean begin(Player attacker, LivingEntity target) {
        if (attacker.level().isClientSide || target.level().isClientSide) {
            return false;
        }
        if (!attacker.isAlive() || !target.isAlive() || attacker == target) {
            return false;
        }
        if (BY_ATTACKER.containsKey(attacker.getUUID()) || TARGET_TO_ATTACKER.containsKey(target.getUUID())) {
            attacker.displayClientMessage(Component.translatable("message.shenmi_yoyo.strangle.busy"), true);
            return false;
        }

        target.stopRiding();
        boolean previousNoGravity = target.isNoGravity();
        target.setNoGravity(true);
        target.fallDistance = 0.0F;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, false, false));

        StrangleTetherEntity tether = new StrangleTetherEntity(attacker.level(), attacker, target);
        attacker.level().addFreshEntity(tether);

        Session session = new Session(attacker, target, previousNoGravity, tether);
        BY_ATTACKER.put(attacker.getUUID(), session);
        TARGET_TO_ATTACKER.put(target.getUUID(), attacker.getUUID());

        attacker.swing(InteractionHand.MAIN_HAND, true);
        attacker.level().playSound(
                null,
                attacker.getX(), attacker.getY(), attacker.getZ(),
                SoundEvents.LEASH_KNOT_PLACE,
                SoundSource.PLAYERS,
                0.8F,
                0.8F
        );
        attacker.displayClientMessage(
                Component.translatable("message.shenmi_yoyo.strangle.begin", target.getDisplayName()),
                true
        );
        if (target instanceof ServerPlayer serverTarget) {
            serverTarget.displayClientMessage(
                    Component.translatable("message.shenmi_yoyo.strangle.begin.victim", attacker.getDisplayName()),
                    true
            );
        }
        return true;
    }

    private static void tickAll() {
        Iterator<Map.Entry<UUID, Session>> iterator = BY_ATTACKER.entrySet().iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next().getValue();
            if (!tick(session)) {
                cleanup(session);
                TARGET_TO_ATTACKER.remove(session.target.getUUID());
                iterator.remove();
            }
        }
    }

    private static boolean tick(Session session) {
        Player attacker = session.attacker;
        LivingEntity target = session.target;
        Level level = attacker.level();

        if (!attacker.isAlive()
                || !target.isAlive()
                || target.level() != level
                || attacker.distanceToSqr(target) > MAX_KEEP_RANGE_SQR) {
            return false;
        }

        session.ticks++;
        target.setNoGravity(true);
        target.fallDistance = 0.0F;

        Vec3 look = attacker.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 0.0001D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalLook = horizontalLook.normalize();
        }

        Vec3 side = new Vec3(-horizontalLook.z, 0.0D, horizontalLook.x);
        double phase = session.ticks * 1.12D;
        double struggleAmount = session.ticks < 8 ? 0.06D : 0.14D;
        Vec3 struggle = side.scale(Math.sin(phase) * struggleAmount)
                .add(0.0D, Math.sin(phase * 1.7D) * 0.045D, 0.0D);

        Vec3 holdPoint = attacker.getEyePosition()
                .add(horizontalLook.scale(1.20D))
                .add(0.0D, 0.10D, 0.0D);
        Vec3 desiredFeet = holdPoint
                .add(struggle)
                .subtract(0.0D, target.getBbHeight() * 0.72D, 0.0D);
        Vec3 delta = desiredFeet.subtract(target.position());
        double pullStrength = session.ticks < 8 ? 0.42D : 0.28D;
        Vec3 velocity = delta.scale(pullStrength);
        if (velocity.lengthSqr() > 1.44D) {
            velocity = velocity.normalize().scale(1.20D);
        }

        target.setDeltaMovement(velocity);
        target.hasImpulse = true;

        if ((session.ticks % 5) == 0) {
            target.animateHurt(0.0F);
            attacker.swing(InteractionHand.MAIN_HAND, true);
        }

        if (level instanceof ServerLevel serverLevel && (session.ticks % 3) == 0) {
            Vec3 neck = target.position().add(0.0D, target.getBbHeight() * 0.78D, 0.0D);
            serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    neck.x, neck.y, neck.z,
                    3,
                    0.12D, 0.08D, 0.12D,
                    0.02D
            );
        }

        if (session.ticks < DURATION_TICKS) {
            return true;
        }

        resolve(session);
        return false;
    }

    private static void resolve(Session session) {
        Player attacker = session.attacker;
        LivingEntity target = session.target;
        Level level = attacker.level();

        if (level.random.nextFloat() < LETHAL_CHANCE) {
            boolean neckBreak = level.random.nextBoolean();
            boolean damaged = target.hurt(attacker.damageSources().playerAttack(attacker), 1024.0F);
            if (damaged) {
                attacker.displayClientMessage(
                        Component.translatable(neckBreak
                                ? "message.shenmi_yoyo.neck_break.success"
                                : "message.shenmi_yoyo.strangle.success", target.getDisplayName()),
                        true
                );
                if (target instanceof ServerPlayer serverTarget) {
                    serverTarget.displayClientMessage(
                            Component.translatable(neckBreak
                                    ? "message.shenmi_yoyo.neck_break.victim"
                                    : "message.shenmi_yoyo.strangle.victim", attacker.getDisplayName()),
                            true
                    );
                }
                level.playSound(
                        null,
                        target.getX(), target.getY(), target.getZ(),
                        neckBreak ? SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR : SoundEvents.PLAYER_ATTACK_CRIT,
                        SoundSource.PLAYERS,
                        neckBreak ? 0.55F : 0.75F,
                        neckBreak ? 1.65F : 0.75F
                );
            } else {
                attacker.displayClientMessage(Component.translatable("message.shenmi_yoyo.pvp_blocked"), true);
            }
        } else {
            target.hurt(attacker.damageSources().playerAttack(attacker), 6.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
            attacker.displayClientMessage(
                    Component.translatable("message.shenmi_yoyo.strangle.failed", target.getDisplayName()),
                    true
            );
        }
    }

    private static void cleanup(Session session) {
        if (session.target.isAlive()) {
            session.target.setNoGravity(session.previousNoGravity);
            session.target.fallDistance = 0.0F;
        }
        if (session.tether != null && session.tether.isAlive()) {
            session.tether.discard();
        }
    }

    private static final class Session {
        private final Player attacker;
        private final LivingEntity target;
        private final boolean previousNoGravity;
        private final StrangleTetherEntity tether;
        private int ticks;

        private Session(
                Player attacker,
                LivingEntity target,
                boolean previousNoGravity,
                StrangleTetherEntity tether
        ) {
            this.attacker = attacker;
            this.target = target;
            this.previousNoGravity = previousNoGravity;
            this.tether = tether;
        }
    }
}
