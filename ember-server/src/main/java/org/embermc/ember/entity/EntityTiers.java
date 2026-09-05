package org.embermc.ember.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.embermc.ember.config.EmberConfigurations;
import org.embermc.ember.config.EmberWorldConfiguration;
import org.embermc.ember.config.Preset;
import org.jspecify.annotations.NullMarked;

/**
 * The Ember entity engine, first layer: a middle tier between Paper's "active"
 * and "inactive".
 *
 * <p>Paper's activation range is binary. Inside the range an entity ticks fully
 * every tick; outside it, it gets {@code inactiveTick()} — goals at a crawl, no
 * movement. Servers wanting to save time shrink the range, and mobs then freeze
 * visibly close to players. Servers wanting mobs to look alive widen it and pay
 * full price for everything in view.
 *
 * <p>This splits Paper's active range in two. An inner ring — a fraction of each
 * type's activation range — stays exactly as Paper has it: full tick, every
 * tick. The outer ring, between that fraction and the activation range, ticks
 * fully every {@code interval}-th tick and inactive-ticks in between, staggered
 * by entity id so the work spreads evenly. Beyond the activation range nothing
 * changes. The saving is real (a mob in the outer ring costs 1/interval), and
 * what a player sees is a mob at mid distance moving a little slower rather
 * than a mob frozen at close range.
 *
 * <p>Anything gameplay-relevant is exempt and stays on full tick wherever it is:
 * players, vehicles and riders, projectiles, items and orbs, anything falling,
 * swimming or burning, a mob with a target or a leash or fresh damage, babies
 * and breeding animals, and types Paper itself never deactivates. The intent
 * behind the rule is a promise: <b>nothing a player is interacting with is
 * ever throttled.</b>
 *
 * <p>Cost: one extra AABB intersect per entity per nearby player in Paper's
 * existing activation loop, and a handful of field reads per entity tick. No
 * searches, no allocation.
 */
@NullMarked
public final class EntityTiers {

    /** The three numbers a preset decides. */
    public enum Level {
        VANILLA(1.0, 1),
        BALANCED(0.75, 2),
        PERFORMANCE(0.5, 2),
        EXTREME(0.4, 4);

        /** Fraction of the activation range that stays on full tick. */
        public final double fullFraction;
        /** Outer-ring entities tick fully every this many ticks. */
        public final int interval;

        Level(final double fullFraction, final int interval) {
            this.fullFraction = fullFraction;
            this.interval = interval;
        }
    }

    /** What the adaptive engine is currently asking for: multipliers on the preset, clamped in the getters. */
    private static volatile double loadFullScale = 1.0;
    private static volatile int loadIntervalScale = 1;

    /** Called by the adaptive engine. Bounds are applied where the values are read, never here. */
    public static void setLoadResponse(final double fullRingScale, final int intervalScale) {
        loadFullScale = fullRingScale;
        loadIntervalScale = intervalScale;
    }

    private static long fullThisTick;
    private static long reducedThisTick;
    private static long fullLastTick;
    private static long reducedLastTick;

    private EntityTiers() {
    }

    /** Resolves the level a world runs at, from its own setting or the server profile. Cheap; called once per world per tick. */
    public static Level levelFor(final ServerLevel level) {
        final var global = EmberConfigurations.global();
        if (!global.entities.tiers.enabled) {
            return Level.VANILLA;
        }
        final EmberWorldConfiguration.Entities.Optimization world = level.emberConfig().entities.optimization;
        final Preset preset = switch (world) {
            case INHERIT -> global.profile;
            case VANILLA -> Preset.VANILLA;
            case BALANCED -> Preset.BALANCED;
            case PERFORMANCE -> Preset.PERFORMANCE;
            case EXTREME -> Preset.EXTREME;
        };
        return switch (preset) {
            case VANILLA -> Level.VANILLA;
            case BALANCED -> Level.BALANCED;
            case PERFORMANCE -> Level.PERFORMANCE;
            case EXTREME -> Level.EXTREME;
        };
    }

    /** Effective full-ring fraction for a world, honouring the global override if set. */
    public static double fullFraction(final Level level) {
        final double override = EmberConfigurations.global().entities.tiers.fullRangeFraction;
        final double base = override > 0 ? override : level.fullFraction;
        // Under load the full ring shrinks, but never below a quarter of the range: a player always has
        // a ring of fully ticking mobs around them wide enough that nothing near them looks throttled.
        return level == Level.VANILLA ? base : Math.max(0.25, base * loadFullScale);
    }

    /** Effective outer-ring interval for a world, honouring the global override if set. */
    public static int interval(final Level level) {
        final int override = EmberConfigurations.global().entities.tiers.reducedInterval;
        final int base = override > 0 ? override : level.interval;
        // Under load the outer ring slows, but never past one full tick in ten: at 1/10 a mob still
        // visibly moves, and Paper's own inactive tick is the floor below that.
        return level == Level.VANILLA ? base : Math.min(10, base * loadIntervalScale);
    }

    /**
     * Decides, for an entity Paper has already judged active, whether this tick
     * should be a reduced one. Main thread only.
     */
    public static boolean reduceThisTick(final Entity entity, final ServerLevel level, final long currentTick) {
        final Level tier = level.emberTierLevel;
        if (tier == Level.VANILLA || entity.emberFullTick >= currentTick) {
            fullThisTick++;
            return false;
        }
        if ((currentTick + entity.getId()) % level.emberTierInterval == 0 || mustTickFully(entity)) {
            fullThisTick++;
            return false;
        }
        reducedThisTick++;
        return true;
    }

    /** The promise: nothing a player is interacting with is throttled. */
    private static boolean mustTickFully(final Entity entity) {
        if (entity instanceof Player || entity.defaultActivationState) {
            return true;
        }
        if (entity.isPassenger() || entity.isVehicle()) {
            return true;
        }
        if (entity instanceof Projectile || entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
            return true;
        }
        if (!entity.onGround() || entity.getRemainingFireTicks() > 0 || (entity.isInWater() && entity.isPushedByFluid())) {
            return true;
        }
        if (entity instanceof final LivingEntity living) {
            if (living.hurtTime > 0 || living.isJumping()) {
                return true;
            }
            if (entity instanceof final Mob mob && (mob.getTarget() != null || mob.getLeashHolder() != null || mob.isAggressive())) {
                return true;
            }
            if (entity instanceof final Animal animal && (animal.isBaby() || animal.isInLove())) {
                return true;
            }
        }
        return false;
    }

    /** Called once per server tick, after entities have ticked. */
    public static void endTick() {
        fullLastTick = fullThisTick;
        reducedLastTick = reducedThisTick;
        fullThisTick = 0;
        reducedThisTick = 0;
    }

    public static long fullLastTick() {
        return fullLastTick;
    }

    public static long reducedLastTick() {
        return reducedLastTick;
    }
}
