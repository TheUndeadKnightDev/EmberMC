package org.embermc.ember.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

/**
 * {@code config/ember-global.yml}.
 *
 * <p>Every option here is read by something in this build. Sections for the
 * entity engine, Packet Guard and the adaptive engine are added by the
 * milestones that implement them, not before, so the file never promises what
 * the server cannot yet do.
 *
 * <p>Each option's comment says whether {@code /ember reload} applies it or a
 * restart is needed. Nothing is described as reload-safe unless changing it at
 * runtime is actually safe.
 */
@SuppressWarnings({"CanBeFinal", "FieldMayBeFinal", "NotNullFieldNotInitialized"})
@ConfigSerializable
public class EmberGlobalConfiguration extends EmberConfigurationPart {

    /** Bump when a transformation is needed; see {@link EmberConfigurations}. */
    public static final int CURRENT_VERSION = 1;

    @Comment("""
        Which set of defaults this server starts from: vanilla, balanced, performance or extreme.
        A preset only decides what an unset option means; anything you set explicitly wins.
        Systems arriving in later milestones read this when their own options are left blank.
        Restart-only.""")
    public Preset profile = Preset.BALANCED;

    public Entities entities = new Entities();

    @ConfigSerializable
    public static class Entities extends EmberConfigurationPart {
        public Tiers tiers = new Tiers();

        @ConfigSerializable
        public static class Tiers extends EmberConfigurationPart {
            @Comment("""
                Split Paper's entity activation range into an inner ring that ticks fully every tick and an
                outer ring that ticks fully every Nth tick (staggered). Anything a player is interacting with -
                targets, leashes, damage, riders, projectiles, items, falling or swimming mobs - always ticks
                fully. How wide the inner ring is and what N is come from the server profile or the world's
                entities.optimization; the two overrides below force one value for every world. Reload-safe.""")
            public boolean enabled = true;

            @Comment("0 = from the profile (balanced 0.75, performance 0.5, extreme 0.4). Otherwise the fraction of each activation range that stays on full tick. Reload-safe.")
            @io.papermc.paper.configuration.constraint.Constraints.Min(0)
            public double fullRangeFraction = 0;

            @Comment("0 = from the profile (balanced 2, performance 2, extreme 4). Otherwise outer-ring entities tick fully every this many ticks. Reload-safe.")
            @io.papermc.paper.configuration.constraint.Constraints.Min(0)
            public int reducedInterval = 0;
        }
    }

    public Adaptive adaptive = new Adaptive();

    @ConfigSerializable
    public static class Adaptive extends EmberConfigurationPart {
        @Comment("""
            Respond to load automatically. Once a second the tick's p95 over the last 5 s is compared with the
            thresholds below; after hold-up-seconds above a threshold the level rises one step, after
            hold-down-seconds below it (minus exit-margin-ms) it falls one step. Each level asks the entity
            tiers to shrink the full ring and slow the outer ring, within the tiers' own limits; nothing else
            is touched, and nothing a player is interacting with is ever throttled. Reload-safe.""")
        public boolean enabled = true;
        @Comment("p95 above this (ms) for hold-up-seconds -> light. Reload-safe.") public double lightAboveMs = 35;
        @Comment("p95 above this (ms) -> moderate. Reload-safe.") public double moderateAboveMs = 45;
        @Comment("p95 above this (ms) -> aggressive. Reload-safe.") public double aggressiveAboveMs = 50;
        @Comment("A level is left only once p95 is this far (ms) below its entry threshold. The hysteresis. Reload-safe.") public double exitMarginMs = 5;
        @Comment("Seconds a threshold must be exceeded before rising a level. Reload-safe.") @io.papermc.paper.configuration.constraint.Constraints.Min(1) public int holdUpSeconds = 5;
        @Comment("Seconds below the exit threshold before falling a level. Longer than hold-up on purpose. Reload-safe.") @io.papermc.paper.configuration.constraint.Constraints.Min(1) public int holdDownSeconds = 20;
        @Comment("Highest level the engine may reach: light, moderate or aggressive. Reload-safe.") public String ceiling = "aggressive";
    }

    public Metrics metrics = new Metrics();

    @ConfigSerializable
    public static class Metrics extends EmberConfigurationPart {
        public Endpoint endpoint = new Endpoint();

        @ConfigSerializable
        public static class Endpoint extends EmberConfigurationPart {
            @Comment("""
                Serve every ember_* gauge at http://<bind>:<port>/metrics in Prometheus text format. Off by default.
                Binds to localhost by default; put a reverse proxy or firewall in front before exposing it wider.
                Restart-only.""")
            public boolean enabled = false;
            @Comment("Address to bind. Restart-only.") public String bind = "127.0.0.1";
            @Comment("Port to bind. Restart-only.") @io.papermc.paper.configuration.constraint.Constraints.Min(1) public int port = 9464;
        }
    }

    public Console console = new Console();

    @ConfigSerializable
    public static class Console extends EmberConfigurationPart {
        @Comment("Print the EmberMC box at startup. Restart-only, since it is printed once at boot.")
        public boolean banner = true;

        @Comment("""
            How much colour to use in the console: truecolor, indexed-256, indexed-16 or none.
            Panel consoles (Pterodactyl and friends) render truecolor; a plain terminal that shows
            garbage wants indexed-16 or none. Log files never contain colour either way.
            Restart-only.""")
        public ColorLevel colorLevel = ColorLevel.TRUECOLOR;

        public enum ColorLevel { TRUECOLOR, INDEXED_256, INDEXED_16, NONE }
    }

    public Status status = new Status();

    @ConfigSerializable
    public static class Status extends EmberConfigurationPart {
        @Comment("""
            Show "live heap after last GC" in /ember status next to used heap.
            Used heap counts garbage not yet collected and says little about footprint on a server
            started with AlwaysPreTouch; live heap is the number that means something. Reload-safe.""")
        public boolean showLiveHeap = true;
    }

    public Profiler profiler = new Profiler();

    @ConfigSerializable
    public static class Profiler extends EmberConfigurationPart {
        @Comment("""
            A tick longer than this, in milliseconds, is a spike: the watchdog writes a report of what
            that tick was doing (phases, worlds, GC, and plugins if a session is running) and logs one
            line. A normal tick is 50 ms; 100 is a clear spike without being noisy. Reload-safe.""")
        @io.papermc.paper.configuration.constraint.Constraints.Min(20)
        public int spikeThresholdMs = 100;

        @Comment("At most one spike report per this many seconds, so a sustained overload produces a few files, not thousands. Reload-safe.")
        @io.papermc.paper.configuration.constraint.Constraints.Min(1)
        public int spikeReportCooldownSeconds = 30;

        @Comment("How many spike reports to keep on disk; the oldest are deleted. Reload-safe.")
        @io.papermc.paper.configuration.constraint.Constraints.Min(1)
        public int keepSpikeReports = 50;

        @Comment("Where spike reports are written, relative to the server directory. Reload-safe.")
        public String spikeReportsDir = "ember-reports";

        @Comment("How long /ember profiler start runs when no duration is given, in seconds. 0 means until stopped. Reload-safe.")
        @io.papermc.paper.configuration.constraint.Constraints.Min(0)
        public int defaultSessionSeconds = 60;
    }

    public UpdateChecker updateChecker = new UpdateChecker();

    @ConfigSerializable
    public static class UpdateChecker extends EmberConfigurationPart {
        @Comment("""
            Print one line at startup naming the EmberMC build. EmberMC has no update endpoint yet,
            so this never contacts the network; it exists so a pasted log identifies the build.
            Reload-safe (takes effect at the next start).""")
        public boolean startupMessage = true;
    }
}
