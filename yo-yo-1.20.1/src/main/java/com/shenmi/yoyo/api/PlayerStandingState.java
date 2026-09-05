package com.shenmi.yoyo.api;

import net.minecraft.world.entity.Entity;

/**
 * Network-synchronized marker used to distinguish the special "stand on a
 * player" posture from vanilla riding. Implemented directly on Player by a
 * common Mixin so every tracking client receives the same value.
 */
public interface PlayerStandingState {
    boolean shenmiYoyo$isStandingOnPlayer();

    void shenmiYoyo$setStandingOnPlayer(boolean standing);

    boolean shenmiYoyo$isSittingOnPlayer();

    void shenmiYoyo$setSittingOnPlayer(boolean sitting);

    int shenmiYoyo$getStandingTargetId();

    void shenmiYoyo$setStandingTargetId(int entityId);

    static boolean isStandingOnPlayer(Object entity) {
        return entity instanceof PlayerStandingState state
                && state.shenmiYoyo$isStandingOnPlayer();
    }

    static void setStandingOnPlayer(Object entity, boolean standing) {
        if (entity instanceof PlayerStandingState state) {
            state.shenmiYoyo$setStandingOnPlayer(standing);
            if (standing) {
                state.shenmiYoyo$setSittingOnPlayer(false);
            } else if (!state.shenmiYoyo$isSittingOnPlayer()) {
                state.shenmiYoyo$setStandingTargetId(-1);
            }
        }
    }

    static boolean isSittingOnPlayer(Object entity) {
        return entity instanceof PlayerStandingState state
                && state.shenmiYoyo$isSittingOnPlayer();
    }

    static void setSittingOnPlayer(Object entity, boolean sitting) {
        if (entity instanceof PlayerStandingState state) {
            state.shenmiYoyo$setSittingOnPlayer(sitting);
            if (sitting) {
                state.shenmiYoyo$setStandingOnPlayer(false);
            } else if (!state.shenmiYoyo$isStandingOnPlayer()) {
                state.shenmiYoyo$setStandingTargetId(-1);
            }
        }
    }

    static boolean isAttachedToPlayer(Object entity) {
        return isStandingOnPlayer(entity) || isSittingOnPlayer(entity);
    }

    static boolean isAttachedPair(Entity first, Entity second) {
        if (first == second) {
            return false;
        }
        return (isAttachedToPlayer(first) && getStandingTargetId(first) == second.getId())
                || (isAttachedToPlayer(second) && getStandingTargetId(second) == first.getId());
    }

    static int getStandingTargetId(Object entity) {
        return entity instanceof PlayerStandingState state
                ? state.shenmiYoyo$getStandingTargetId()
                : -1;
    }

    static void setStandingTargetId(Object entity, int entityId) {
        if (entity instanceof PlayerStandingState state) {
            state.shenmiYoyo$setStandingTargetId(entityId);
        }
    }

    static void startStanding(Object entity, int targetEntityId) {
        if (entity instanceof PlayerStandingState state) {
            state.shenmiYoyo$setStandingTargetId(targetEntityId);
            state.shenmiYoyo$setSittingOnPlayer(false);
            state.shenmiYoyo$setStandingOnPlayer(true);
        }
    }

    static void startSitting(Object entity, int targetEntityId) {
        if (entity instanceof PlayerStandingState state) {
            state.shenmiYoyo$setStandingTargetId(targetEntityId);
            state.shenmiYoyo$setStandingOnPlayer(false);
            state.shenmiYoyo$setSittingOnPlayer(true);
        }
    }

    static void stopStanding(Object entity) {
        if (entity instanceof PlayerStandingState state) {
            state.shenmiYoyo$setStandingOnPlayer(false);
            state.shenmiYoyo$setSittingOnPlayer(false);
            state.shenmiYoyo$setStandingTargetId(-1);
        }
    }
}
