package org.embermc.ember.config;

/**
 * The four starting points an administrator can pick from.
 *
 * <p>A preset is a set of <em>defaults</em>, not a lock: anything set explicitly
 * in a configuration file wins over what the preset would have chosen. Later
 * milestones consult the active preset when a value is left unset; this build
 * records the choice and shows it in {@code /ember config}.
 */
public enum Preset {
    /** EmberMC systems present but passive: observe and report, change nothing about gameplay timing. */
    VANILLA,
    /** Production default. Optimisations no player can notice; protections on with generous limits. */
    BALANCED,
    /** Larger activation ranges, longer inactive intervals, tighter limits. Test your farms. */
    PERFORMANCE,
    /** Lobbies, minigames, resource worlds. Distant entities barely tick. Not for a main survival world. */
    EXTREME
}
