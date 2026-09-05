package org.embermc.ember.config;

/**
 * Marker for every section of an EmberMC configuration file.
 *
 * <p>The loader registers one type serializer for "anything assignable to this",
 * which is how a nested section is recognised as a section rather than as an
 * opaque object. Paper does the same with its own {@code ConfigurationPart};
 * EmberMC keeps a separate marker so the two trees never serialise each other.
 */
public abstract class EmberConfigurationPart {
}
