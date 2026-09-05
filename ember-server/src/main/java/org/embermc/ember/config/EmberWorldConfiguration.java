package org.embermc.ember.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

/**
 * {@code config/ember-world-defaults.yml}, overridden per world by
 * {@code <world>/ember-world.yml}.
 *
 * <p>A world file only needs the keys it changes; everything else comes from
 * the defaults file. That is Paper's own override model, reused so an
 * administrator maintaining thirty worlds maintains one file and thirty
 * three-line exceptions, not thirty copies.
 */
@SuppressWarnings({"CanBeFinal", "FieldMayBeFinal", "NotNullFieldNotInitialized"})
@ConfigSerializable
public class EmberWorldConfiguration extends EmberConfigurationPart {

    public static final int CURRENT_VERSION = 1;

    public Entities entities = new Entities();

    @ConfigSerializable
    public static class Entities extends EmberConfigurationPart {
        @Comment("""
            How hard the entity engine may work this world: inherit (use the server profile),
            vanilla, balanced, performance or extreme. A lobby or resource world can run extreme
            while the main survival world stays balanced.
            NOT YET APPLIED: the entity engine arrives in Milestone 4. The value is read, validated
            and shown in /ember config today so world files can be prepared ahead of it.""")
        public Optimization optimization = Optimization.INHERIT;

        public enum Optimization { INHERIT, VANILLA, BALANCED, PERFORMANCE, EXTREME }
    }
}
