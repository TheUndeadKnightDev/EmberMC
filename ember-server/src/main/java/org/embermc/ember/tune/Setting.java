package org.embermc.ember.tune;

import java.util.Map;
import org.embermc.ember.config.Preset;
import org.jspecify.annotations.NullMarked;

/**
 * One tunable key in one of the server's configuration files, with the value
 * each preset wants for it.
 *
 * @param file    which file: {@link File#PAPER_WORLD_DEFAULTS}, {@link File#SPIGOT} or {@link File#BUKKIT}
 * @param path    dotted YAML path inside that file
 * @param values  target value per preset; {@link Preset#VANILLA} is always the
 *                upstream default, so applying it is the revert
 * @param visible what a player or plugin could notice, or null if nothing
 * @param why     the one-line reason this key matters for performance
 */
@NullMarked
public record Setting(File file, String path, Map<Preset, Object> values, @org.jspecify.annotations.Nullable String visible, String why) {

    public enum File {
        PAPER_WORLD_DEFAULTS("config/paper-world-defaults.yml", "applies on /paper reload"),
        SPIGOT("spigot.yml", "needs a restart"),
        BUKKIT("bukkit.yml", "needs a restart");

        public final String relativePath;
        public final String takesEffect;

        File(final String relativePath, final String takesEffect) {
            this.relativePath = relativePath;
            this.takesEffect = takesEffect;
        }
    }

    public Object value(final Preset preset) {
        Object v = this.values.get(preset);
        if (v == null) {
            // Presets fall back to the next gentler one, so a table entry only
            // has to name the presets where the value changes.
            v = switch (preset) {
                case EXTREME -> value(Preset.PERFORMANCE);
                case PERFORMANCE -> value(Preset.BALANCED);
                case BALANCED -> value(Preset.VANILLA);
                case VANILLA -> throw new IllegalStateException("no vanilla value for " + this.path);
            };
        }
        return v;
    }
}
