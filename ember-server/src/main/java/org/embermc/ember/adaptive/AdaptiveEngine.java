package org.embermc.ember.adaptive;

import org.jspecify.annotations.NullMarked;

/**
 * The Adaptive Performance Engine, as a state machine with no server in it.
 *
 * <p>Once a second the engine is given the tick's recent p95 and decides which
 * {@link LoadLevel} the server is in. Levels are entered quickly and left
 * slowly: a level is raised after the p95 has sat above its threshold for
 * {@code holdUpSeconds}, and lowered only after it has sat below the level's
 * exit threshold for {@code holdDownSeconds}. The exit threshold is below the
 * entry threshold. Together those two rules are the hysteresis that stops the
 * server flapping between levels on a noisy tick.
 *
 * <p>The engine itself changes nothing. It exposes a level; the systems that
 * respond to load — today the entity tiers — read the level's multipliers and
 * apply them within their own bounds. That keeps every response in one place,
 * with its own limit, and keeps this class small enough to test exhaustively
 * without a server.
 */
@NullMarked
public final class AdaptiveEngine {

    /** How hard the server is working, and what the responders may do about it. */
    public enum LoadLevel {
        /** Under the light threshold: nothing is changed. */
        NORMAL(1.0, 1),
        /** Outer-ring entities tick a little less often. */
        LIGHT(0.9, 1),
        /** Full ring shrinks; outer ring ticks half as often as its preset. */
        MODERATE(0.75, 2),
        /** Full ring shrinks further; outer ring at a quarter of its preset rate. Never below what a player can see. */
        AGGRESSIVE(0.6, 4);

        /** Multiplier on the entity tiers' full-ring fraction (1 = preset value). */
        public final double fullRingScale;
        /** Multiplier on the entity tiers' outer-ring interval (1 = preset value). */
        public final int intervalScale;

        LoadLevel(final double fullRingScale, final int intervalScale) {
            this.fullRingScale = fullRingScale;
            this.intervalScale = intervalScale;
        }

        LoadLevel up() {
            return this == AGGRESSIVE ? this : values()[ordinal() + 1];
        }

        LoadLevel down() {
            return this == NORMAL ? this : values()[ordinal() - 1];
        }
    }

    /** The thresholds and holds, so a test can supply its own. */
    public record Settings(double lightAboveMs, double moderateAboveMs, double aggressiveAboveMs,
                           double exitMarginMs, int holdUpSeconds, int holdDownSeconds, LoadLevel ceiling) {
        public static final Settings DEFAULT = new Settings(35, 45, 50, 5, 5, 20, LoadLevel.AGGRESSIVE);

        double entry(final LoadLevel level) {
            return switch (level) {
                case NORMAL -> Double.NEGATIVE_INFINITY;
                case LIGHT -> this.lightAboveMs;
                case MODERATE -> this.moderateAboveMs;
                case AGGRESSIVE -> this.aggressiveAboveMs;
            };
        }
    }

    private Settings settings;
    private LoadLevel level = LoadLevel.NORMAL;
    private int secondsAbove;
    private int secondsBelow;
    private long lastChangeSecond = -1;
    private long changes;

    public AdaptiveEngine(final Settings settings) {
        this.settings = settings;
    }

    public void settings(final Settings settings) {
        this.settings = settings;
        if (this.level.compareTo(settings.ceiling()) > 0) {
            this.level = settings.ceiling();
        }
    }

    public Settings settings() {
        return this.settings;
    }

    /**
     * Feed one second of observation. Returns the level in force afterwards.
     *
     * @param p95Ms the tick's p95 over the last few seconds
     * @param nowSecond a monotonically increasing second counter
     */
    public LoadLevel observe(final double p95Ms, final long nowSecond) {
        final LoadLevel next = this.level.up();
        final LoadLevel prev = this.level.down();

        // Rising: sustained above the NEXT level's entry threshold.
        if (next != this.level && next.compareTo(this.settings.ceiling()) <= 0 && p95Ms > this.settings.entry(next)) {
            this.secondsAbove++;
            this.secondsBelow = 0;
            if (this.secondsAbove >= this.settings.holdUpSeconds()) {
                this.change(next, nowSecond);
            }
            return this.level;
        }
        this.secondsAbove = 0;

        // Falling: sustained below THIS level's entry threshold minus the margin.
        if (prev != this.level && p95Ms < this.settings.entry(this.level) - this.settings.exitMarginMs()) {
            this.secondsBelow++;
            if (this.secondsBelow >= this.settings.holdDownSeconds()) {
                this.change(prev, nowSecond);
            }
            return this.level;
        }
        this.secondsBelow = 0;
        return this.level;
    }

    private void change(final LoadLevel to, final long nowSecond) {
        this.level = to;
        this.secondsAbove = 0;
        this.secondsBelow = 0;
        this.lastChangeSecond = nowSecond;
        this.changes++;
    }

    public LoadLevel level() {
        return this.level;
    }

    public long lastChangeSecond() {
        return this.lastChangeSecond;
    }

    public long changes() {
        return this.changes;
    }

    /** Forces a level (for an administrator's manual override or a test). */
    public void force(final LoadLevel level, final long nowSecond) {
        this.change(level, nowSecond);
    }
}
