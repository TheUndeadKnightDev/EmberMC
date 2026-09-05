package org.embermc.ember.security;

/**
 * What the Packet Guard does when a category is over its limit.
 *
 * <p>Chosen per category for a busy survival server: throttle where a burst is
 * plausibly legitimate (movement, interaction), drop where an occasional lost
 * packet is harmless (arm swings, tab completion), kick only for behaviour that
 * has no honest explanation at that rate.
 */
public enum GuardAction {
    /** Count it and let it through. The measurement mode. */
    LOG,
    /** Count it, let it through, and warn in the console (rate-limited). */
    WARN,
    /** Drop this packet but keep the connection; the client simply retries. */
    THROTTLE,
    /** Drop this packet silently. Same as THROTTLE for now; named for intent in config. */
    DROP,
    /** Disconnect the client. */
    KICK
}
