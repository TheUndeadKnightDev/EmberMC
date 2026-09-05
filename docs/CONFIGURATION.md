# Configuration

EmberMC's settings live next to Paper's, use the same file conventions, and
are loaded by the same machinery — Paper's Configurate-based system, reused
rather than duplicated. An administrator who knows `paper-global.yml` already
knows how these work.

| File | Scope |
| --- | --- |
| `config/ember-global.yml` | the server |
| `config/ember-world-defaults.yml` | every world, unless a world overrides a key |
| `<world>/ember-world.yml` | one world's overrides — only the keys it changes. Lives next to Paper's `paper-world.yml`, e.g. `world/dimensions/minecraft/overworld/ember-world.yml` on current world layouts |

Paper's own files are untouched. EmberMC never reads or writes
`paper-global.yml`.

## The rules

**Every option is real.** An option appears in the file only when something in
the build reads it. Sections for the entity engine, Packet Guard and the
adaptive engine arrive with the milestones that implement them. The one
exception is marked as such in its own comment (`entities.optimization`, read
and shown today, applied from Milestone 4) so world files can be prepared ahead
of it.

**Every option is documented here, with its reload semantics.** The YAML
loader Paper ships writes a header comment per file but not per-key comments
(Paper's own `paper-global.yml` is the same), so the explanations below are the
reference. Each option is either
*reload-safe* — `/ember reload` applies it — or *restart-only*. Nothing is
called reload-safe unless changing it at runtime is actually safe.
`/ember reload` re-reads every file, then tells you which values it applied and
which need a restart.

**Versioned.** `_version` at the top of each file. When a key moves or changes
meaning, a transformation migrates old files in code; a value you set is never
silently dropped.

**Validated on load.** A value outside its range is reported with the path and
the default that was used instead. The server still starts.

**Presets are defaults, not locks.** `profile` decides what an *unset* option
means. Anything you set explicitly wins over the preset.

## `ember-global.yml`

```yaml
_version: 1

# Which set of defaults this server starts from: VANILLA, BALANCED, PERFORMANCE or EXTREME (case-insensitive).
# A preset only decides what an unset option means; anything you set explicitly wins.
# Systems arriving in later milestones read this when their own options are left blank.
# Restart-only.
profile: BALANCED

console:
  # Print the EmberMC box at startup. Restart-only, since it is printed once at boot.
  banner: true
  # How much colour to use in the console: truecolor, indexed-256, indexed-16 or none.
  # Panel consoles (Pterodactyl and friends) render truecolor; a plain terminal that shows
  # garbage wants indexed-16 or none. Log files never contain colour either way.
  # Restart-only.
  color-level: TRUECOLOR

status:
  # Show "live heap after last GC" in /ember status next to used heap.
  # Used heap counts garbage not yet collected and says little about footprint on a server
  # started with AlwaysPreTouch; live heap is the number that means something. Reload-safe.
  show-live-heap: true

update-checker:
  # Print one line at startup naming the EmberMC build. EmberMC has no update endpoint yet,
  # so this never contacts the network; it exists so a pasted log identifies the build.
  # Reload-safe (takes effect at the next start).
  startup-message: true
```

## `ember-world-defaults.yml` and `<world>/ember-world.yml`

```yaml
_version: 1

entities:
  # How hard the entity engine may work this world: inherit (use the server profile),
  # vanilla, balanced, performance or extreme. A lobby or resource world can run extreme
  # while the main survival world stays balanced.
  # NOT YET APPLIED: the entity engine arrives in Milestone 4. The value is read, validated
  # and shown in /ember config today so world files can be prepared ahead of it.
  optimization: INHERIT
```

A world file starts empty apart from `_version`. Add only what that world
changes:

```yaml
# world_resource/dimensions/minecraft/overworld/ember-world.yml
_version: 1
entities:
  optimization: EXTREME
```

## Presets

| Preset | Intent |
| --- | --- |
| `vanilla` | EmberMC systems present but passive: observe and report, change nothing about gameplay timing |
| `balanced` | Production default. Optimisations no player can notice; protections on with generous limits |
| `performance` | Larger activation ranges, longer inactive intervals, tighter limits. Test your farms |
| `extreme` | Lobbies, minigames, resource worlds. Distant entities barely tick. Not for a main survival world |

## Commands

| Command | Permission | Does |
| --- | --- | --- |
| `/ember config` | `ember.command.config` | Shows the active profile, console settings, file locations, and each world's effective values |
| `/ember reload` | `ember.command.reload` | Re-reads all three kinds of file and reports what applied and what needs a restart |

## For developers

`org.embermc.ember.config.EmberConfigurations` extends Paper's
`Configurations<G, W>`. Global settings are `EmberConfigurations.global()`;
a world's are `((ServerLevel) level).emberConfig()`. Sections are plain
`@ConfigSerializable` classes extending `EmberConfigurationPart`, with
Configurate's `@Comment` on every field. Adding an option is adding a field with
a comment that states its reload semantics; the file, the defaults merge and
`/ember reload` follow from that.
