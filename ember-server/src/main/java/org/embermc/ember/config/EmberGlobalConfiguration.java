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
