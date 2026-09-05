package org.embermc.ember.config;

import io.papermc.paper.configuration.Configurations;
import io.papermc.paper.configuration.PaperConfigurations;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import static io.leangen.geantyref.GenericTypeReflector.erase;

/**
 * EmberMC's configuration files, loaded through Paper's own Configurate stack.
 *
 * <p>Three files, mirroring Paper's layout so an administrator who knows one
 * knows the other:
 *
 * <ul>
 *   <li>{@code config/ember-global.yml} — the server</li>
 *   <li>{@code config/ember-world-defaults.yml} — every world unless overridden</li>
 *   <li>{@code <world>/ember-world.yml} — one world's overrides, only the keys it changes</li>
 * </ul>
 *
 * <p>Reusing {@link Configurations} rather than writing a second YAML layer buys
 * versioning, the defaults-plus-override merge, comment persistence, constraint
 * annotations and the same on-disk conventions for free. Paper's classes and
 * EmberMC's are kept apart by marker type, so neither loader will ever try to
 * read the other's file.
 *
 * <p>Initialised once at startup, before the banner and before any world loads,
 * so both can read it. {@link #reload} re-reads every file into the live
 * instances; individual options say in their comments whether that is enough
 * for them to take effect.
 */
@NullMarked
public final class EmberConfigurations extends Configurations<EmberGlobalConfiguration, EmberWorldConfiguration> {

    public static final String GLOBAL_FILE = "ember-global.yml";
    public static final String WORLD_DEFAULTS_FILE = "ember-world-defaults.yml";
    public static final String WORLD_FILE = "ember-world.yml";

    private static final String GLOBAL_HEADER = """
        EmberMC - server-wide settings.
        Ignite better performance. https://docs.emberplugins.online/embermc/

        Every option has a comment, and every comment says whether /ember reload applies it or a
        restart is needed. Per-world settings live in ember-world-defaults.yml and in each world's
        own ember-world.yml. Paper's settings are untouched and still live in paper-global.yml.""";

    private static final String WORLD_DEFAULTS_HEADER = """
        EmberMC - defaults for every world.

        Any world can override any key here in its own <world>/ember-world.yml. A world file only
        needs the keys it changes; everything else comes from this file.""";

    private static @Nullable EmberConfigurations instance;
    private static @Nullable EmberGlobalConfiguration global;

    private EmberConfigurations(final Path configDir) {
        super(configDir, EmberGlobalConfiguration.class, EmberWorldConfiguration.class,
            GLOBAL_FILE, WORLD_DEFAULTS_FILE, WORLD_FILE);
    }

    /** Loads (or creates) the global and world-defaults files. Call once, early. */
    public static void init(final Path configDir, final RegistryAccess registryAccess) {
        final EmberConfigurations configurations = new EmberConfigurations(configDir);
        try {
            if (!java.nio.file.Files.isDirectory(configDir)) {
                java.nio.file.Files.createDirectories(configDir);
            }
            global = configurations.initializeGlobalConfiguration(registryAccess);
            configurations.initializeWorldDefaultsConfiguration(registryAccess);
        } catch (final IOException ex) {
            throw new RuntimeException("Could not load EmberMC configuration from " + configDir, ex);
        }
        instance = configurations;
    }

    public static boolean isInitialized() {
        return instance != null && global != null;
    }

    public static EmberConfigurations get() {
        final EmberConfigurations i = instance;
        if (i == null) {
            throw new IllegalStateException("EmberMC configuration has not been initialised yet");
        }
        return i;
    }

    public static EmberGlobalConfiguration global() {
        final EmberGlobalConfiguration g = global;
        if (g == null) {
            throw new IllegalStateException("EmberMC configuration has not been initialised yet");
        }
        return g;
    }

    /**
     * Builds a world's configuration: defaults merged with its own file.
     *
     * <p>Called from inside {@code ServerLevel}'s constructor, where
     * {@code level.getServer()} is still null, so the server and game rules are
     * passed in rather than read back off the level.
     */
    public EmberWorldConfiguration createWorldConfig(final ServerLevel level, final MinecraftServer server, final net.minecraft.world.level.gamerules.GameRules gameRules) {
        try {
            return this.createWorldConfig(PaperConfigurations.createWorldContextMap(
                server.storageSource.getDimensionPath(level.dimension()),
                level.dimension().identifier(),
                level.spigotConfig,
                server.registryAccess(),
                gameRules
            ));
        } catch (final IOException ex) {
            throw new RuntimeException("Could not create EmberMC world config for " + level.dimension().identifier(), ex);
        }
    }

    /**
     * Re-reads every file into the live objects. Values that are not reload-safe
     * are re-read too, but the code that consumed them at startup has already
     * run; their comments say so.
     */
    public void reload(final MinecraftServer server) {
        try {
            this.initializeGlobalConfiguration(server.registryAccess(), reloader(EmberGlobalConfiguration.class, global()));
            this.initializeWorldDefaultsConfiguration(server.registryAccess());
            for (final ServerLevel level : server.getAllLevels()) {
                this.createWorldConfig(contextFor(level), reloader(EmberWorldConfiguration.class, level.emberConfig()));
            }
        } catch (final IOException ex) {
            throw new RuntimeException("Could not reload EmberMC configuration", ex);
        }
    }

    /**
     * Loads a node into an existing instance instead of making a new one, so
     * every holder of the old reference sees the new values. Paper has the same
     * helper but keeps it package-private.
     */
    @SuppressWarnings("unchecked")
    private static <T> org.spongepowered.configurate.util.CheckedFunction<org.spongepowered.configurate.ConfigurationNode, T, org.spongepowered.configurate.serialize.SerializationException> reloader(final Class<T> type, final T instance) {
        return node -> {
            final org.spongepowered.configurate.objectmapping.ObjectMapper.Factory factory =
                (org.spongepowered.configurate.objectmapping.ObjectMapper.Factory) java.util.Objects.requireNonNull(node.options().serializers().get(type));
            final org.spongepowered.configurate.objectmapping.ObjectMapper.Mutable<T> mutable =
                (org.spongepowered.configurate.objectmapping.ObjectMapper.Mutable<T>) factory.get(type);
            mutable.load(instance, node);
            return instance;
        };
    }

    private static ContextMap contextFor(final ServerLevel level) {
        return PaperConfigurations.createWorldContextMap(
            level.getServer().storageSource.getDimensionPath(level.dimension()),
            level.dimension().identifier(),
            level.spigotConfig,
            level.registryAccess(),
            level.getGameRules()
        );
    }

    /* ---- Configurations contract ---------------------------------------- */

    @Override
    protected boolean isConfigType(final Type type) {
        return EmberConfigurationPart.class.isAssignableFrom(erase(type));
    }

    @Override
    protected int globalConfigVersion() {
        return EmberGlobalConfiguration.CURRENT_VERSION;
    }

    @Override
    protected int worldConfigVersion() {
        return EmberWorldConfiguration.CURRENT_VERSION;
    }

    @Override
    protected YamlConfigurationLoader.Builder createGlobalLoaderBuilder(final RegistryAccess registryAccess) {
        return super.createGlobalLoaderBuilder(registryAccess)
            .defaultOptions(options -> options
                .header(GLOBAL_HEADER)
                .serializers(s -> s.register(new io.papermc.paper.configuration.serializer.EnumValueSerializer()))); // lower-case enums, as Paper writes them
    }

    @Override
    protected YamlConfigurationLoader.Builder createWorldConfigLoaderBuilder(final ContextMap contextMap) {
        final boolean defaults = contextMap.require(WORLD_KEY).equals(WORLD_DEFAULTS_KEY);
        return super.createWorldConfigLoaderBuilder(contextMap)
            .defaultOptions(options -> options
                .header(defaults
                    ? WORLD_DEFAULTS_HEADER
                    : "EmberMC - settings for this world only. Keys left out come from config/ember-world-defaults.yml.")
                .serializers(s -> s.register(new io.papermc.paper.configuration.serializer.EnumValueSerializer())));
    }
}
