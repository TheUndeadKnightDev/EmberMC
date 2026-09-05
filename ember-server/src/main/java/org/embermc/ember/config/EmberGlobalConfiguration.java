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
        public ItemLimits itemLimits = new ItemLimits();
        public XpLimits xpLimits = new XpLimits();
        public Pathfinding pathfinding = new Pathfinding();

        @ConfigSerializable
        public static class Pathfinding extends EmberConfigurationPart {
            @Comment("""
                Stop mobs re-running the A* pathfinder against a target they have already proven they cannot
                reach. Paper backs off failed attempts to follow an entity; this extends the same bounded rule
                to every pathfind, including positional ones (wander, work-site and flee goals) that Paper
                leaves uncovered - the case that hurts is a crowd of mobs stuck at a base wall, each paying a
                real search on its throttle. After failures-before-backoff failed searches to the same target,
                further searches to that target are skipped for backoff-ticks; a different or newly reachable
                target resets it at once, so a mob is never held longer than the window and only on a target it
                has already failed to reach. Counted in ember_pathfinds_skipped. Reload-safe.""")
            public boolean enabled = true;

            @Comment("Consecutive failed searches to the same target before the backoff arms. Reload-safe.")
            @io.papermc.paper.configuration.constraint.Constraints.Min(1)
            public int failuresBeforeBackoff = 8;

            @Comment("How many ticks the backoff lasts once armed (20 ticks = 1 second). Reload-safe.")
            @io.papermc.paper.configuration.constraint.Constraints.Min(1)
            public int backoffTicks = 40;
        }

        @ConfigSerializable
        public static class ItemLimits extends EmberConfigurationPart {
            @Comment("""
                Cap dropped-item entities per loaded chunk, as an anti-abuse backstop against dupe machines and
                runaway farms that flood a chunk with items. Paper only trims items when a chunk unloads; this
                trims a loaded chunk within sweep-seconds. OFF by default because removing items changes
                gameplay - turn it on with a generous cap, not a farm nerf. The oldest items in an over-full
                chunk go first, so a player's fresh drop survives. Removals fire Bukkit's EntityRemoveEvent and
                show in /ember security. Reload-safe.""")
            public boolean enabled = false;
            @Comment("Most dropped-item entities allowed in one loaded chunk. Reload-safe.")
            @io.papermc.paper.configuration.constraint.Constraints.Min(0)
            public int maxPerChunk = 300;
            @Comment("How often to sweep, in seconds. Reload-safe.")
            @io.papermc.paper.configuration.constraint.Constraints.Min(1)
            public int sweepSeconds = 10;
        }

        @ConfigSerializable
        public static class XpLimits extends EmberConfigurationPart {
            @Comment("""
                Cap experience-orb entities per loaded chunk WITHOUT losing any experience: overflow orbs' XP is
                folded into the orbs that remain, then the emptied entities are removed. Paper merges orbs
                pairwise by value but does not bound the count per chunk, so a grinder or XP-dupe can still pile
                up thousands of orb entities. OFF by default. Fires EntityRemoveEvent; shown in /ember security.
                Reload-safe.""")
            public boolean enabled = false;
            @Comment("Most experience-orb entities allowed in one loaded chunk before overflow is merged into survivors. Reload-safe.")
            @io.papermc.paper.configuration.constraint.Constraints.Min(0)
            public int maxPerChunk = 200;
            @Comment("How often to sweep, in seconds. Reload-safe.")
            @io.papermc.paper.configuration.constraint.Constraints.Min(1)
            public int sweepSeconds = 10;
        }

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

    public Chunks chunks = new Chunks();

    @ConfigSerializable
    public static class Chunks extends EmberConfigurationPart {
        @Comment("""
            Report what is keeping chunks loaded, so unnecessary retention is visible. Paper's chunk system
            already loads, generates and saves chunks well (async saves, per-player concurrent load and
            generate limits, a delayed unload); what it does not surface is which plugin or force-load is
            holding chunks loaded, which is the usual cause of a world that never shrinks and idles heavy.
            /ember chunks shows loaded, ticking, force-loaded and plugin-held counts per world, and when a
            world's plugin-held chunks pass warn-threshold a single rate-limited line names the worst
            offenders. Read-only: EmberMC never force-unloads a chunk a plugin asked for. Reload-safe.""")
        public boolean retentionDiagnostics = true;

        @Comment("Log a warning when one world has more than this many plugin-ticket-held chunks. 0 disables the warning (the /ember chunks read-out stays). Reload-safe.")
        @io.papermc.paper.configuration.constraint.Constraints.Min(0)
        public int warnThreshold = 400;

        @Comment("At most one retention warning per this many seconds. Reload-safe.")
        @io.papermc.paper.configuration.constraint.Constraints.Min(1)
        public int warnCooldownSeconds = 300;
    }

    public Security security = new Security();

    @ConfigSerializable
    public static class Security extends EmberConfigurationPart {
        public PacketGuard packetGuard = new PacketGuard();

        @ConfigSerializable
        public static class PacketGuard extends EmberConfigurationPart {
            @Comment("""
                Per-category inbound packet limits, on top of Paper's own all-packets limiter. Each category
                is a token bucket (rate per second + a burst allowance, so a legitimate flurry is fine) with
                an optional max payload size and an action: log (count only), warn, throttle/drop (drop the
                packet, keep the player), or kick. Limits are per connection. The guard never logs packet
                contents or authentication data. Reload-safe.""")
            public boolean enabled = true;

            public Limit movement   = new Limit(200, 400, 0, GuardActionValue.THROTTLE);
            public Limit armSwing    = new Limit(60, 120, 0, GuardActionValue.DROP);
            public Limit interact    = new Limit(40, 80, 0, GuardActionValue.THROTTLE);
            public Limit inventory   = new Limit(40, 100, 0, GuardActionValue.THROTTLE);
            public Limit bookSign    = new Limit(4, 8, 12288, GuardActionValue.KICK);
            public Limit chat        = new Limit(8, 16, 0, GuardActionValue.THROTTLE);
            public Limit command     = new Limit(15, 30, 0, GuardActionValue.THROTTLE);
            public Limit tabComplete = new Limit(20, 40, 0, GuardActionValue.DROP);
            public Limit recipe      = new Limit(8, 16, 0, GuardActionValue.THROTTLE);
            public Limit creative    = new Limit(20, 60, 0, GuardActionValue.THROTTLE);
            public Limit pluginMessage = new Limit(20, 60, 32768, GuardActionValue.THROTTLE);
            public Limit other       = new Limit(500, 1000, 0, GuardActionValue.LOG);

            public enum GuardActionValue { LOG, WARN, THROTTLE, DROP, KICK }

            @ConfigSerializable
            public static class Limit extends EmberConfigurationPart {
                @Comment("Sustained packets per second allowed for this category.")
                public double perSecond = 100;
                @Comment("Extra packets a short burst may spend at once.")
                public double burst = 200;
                @Comment("Largest payload in bytes; 0 = no size check.")
                public int maxBytes = 0;
                @Comment("log | warn | throttle | drop | kick")
                public GuardActionValue action = GuardActionValue.THROTTLE;

                public Limit() { }
                public Limit(final double perSecond, final double burst, final int maxBytes, final GuardActionValue action) {
                    this.perSecond = perSecond; this.burst = burst; this.maxBytes = maxBytes; this.action = action;
                }
            }

            public Limit limitFor(final org.embermc.ember.security.PacketCategory c) {
                return switch (c) {
                    case MOVEMENT -> movement;
                    case ARM_SWING -> armSwing;
                    case INTERACT -> interact;
                    case INVENTORY -> inventory;
                    case BOOK_SIGN -> bookSign;
                    case CHAT -> chat;
                    case COMMAND -> command;
                    case TAB_COMPLETE -> tabComplete;
                    case RECIPE -> recipe;
                    case CREATIVE -> creative;
                    case PLUGIN_MESSAGE -> pluginMessage;
                    case OTHER -> other;
                };
            }
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

    public Memory memory = new Memory();

    @ConfigSerializable
    public static class Memory extends EmberConfigurationPart {
        public IdleTrim idleTrim = new IdleTrim();

        @ConfigSerializable
        public static class IdleTrim extends EmberConfigurationPart {
            @Comment("""
                Hand idle heap back to the operating system while the server is empty. After the server has
                had zero players online for after-minutes, EmberMC asks the JVM for one collection so the
                garbage collector can uncommit unused heap regions, shrinking the process's real footprint.
                It runs only with nobody online, so the collection pause affects no one, and at most once per
                idle stretch (it re-arms after a player has come and gone). This only lowers the footprint the
                OS sees if the JVM is allowed to uncommit: the classic Xms=Xmx + AlwaysPreTouch flag set pins
                the whole heap and the trim then returns nothing. See docs/optimisations/idle-memory.md for the
                flags that let it work. Reload-safe.""")
            public boolean enabled = true;

            @Comment("Minutes the server must be empty before a trim. Reload-safe.")
            @io.papermc.paper.configuration.constraint.Constraints.Min(1)
            public int afterMinutes = 5;
        }
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
