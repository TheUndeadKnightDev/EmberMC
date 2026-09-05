# Configuration

**Status: not yet implemented — this is Milestone 2.** Nothing in this document
exists in the current build. It records the design the milestone will
implement so it can be reviewed before code is written.

## Where it lives

EmberMC's settings go in `config/ember-global.yml` and
`config/ember-world-defaults.yml`, next to Paper's own `paper-global.yml` and
`paper-world-defaults.yml`, with per-world overrides in each world's
`ember-world.yml`. This reuses Paper's Configurate-based configuration system —
its loader, its versioning, its migration hooks and its per-world override
resolution — rather than adding a second YAML stack.

## Shape

```yaml
_version: 1

# One of: vanilla, balanced, performance, extreme.
# Sets the default for every option below; anything you set explicitly wins.
profile: balanced

entities:
  adaptive-ticking:
    enabled: true
    # Blocks from the nearest active player at which an entity drops a tier.
    reduced-tick-range: 48
    minimal-tick-range: 96
    # Ticks between updates in each tier. FULL is always 1.
    reduced-interval: 4
    minimal-interval: 20
  optimize-pathfinding: true
  optimize-collisions: true
  optimize-item-merging: true
  optimize-xp-orbs: true

adaptive-engine:
  enabled: true
  # MSPT thresholds for each load level, and how long a level has to hold
  # before the engine moves — hysteresis, so it never oscillates.
  light-above: 35
  moderate-above: 45
  aggressive-above: 50
  hold-for-seconds: 10

packet-guard:
  enabled: true
  # ... per-category limits, each with an action: log | warn | throttle | drop | kick
```

Every option carries a comment in the generated file. The comments are the
documentation; this file only describes shape and rules.

## Rules

- **Versioned.** `_version` at the top; migrations are code, tested, and never
  lose a value the administrator set.
- **Validated on load.** An out-of-range value is reported with the path, the
  value, the allowed range and the default that was used instead. The server
  still starts.
- **Honest about reloading.** Each option is either reload-safe or restart-only,
  and the generated file says which. `/ember reload` applies the reload-safe
  set and lists what it did not touch. Nothing is claimed to hot-reload that
  cannot.
- **Presets are defaults, not locks.** Choosing `performance` changes what an
  unset option means. Anything set explicitly stays as set.
- **Per-world without sprawl.** Worlds inherit from `ember-world-defaults.yml`
  and override only the keys they name.

## Presets

| Preset | Intent |
| --- | --- |
| `vanilla` | EmberMC systems present but passive: observe and report, change nothing about gameplay timing |
| `balanced` | Production default. Optimisations that no player can notice; protections on with generous limits |
| `performance` | Larger activation ranges, longer inactive intervals, tighter limits. Acceptable on survival; test your farms |
| `extreme` | For lobbies, minigames and resource worlds. Distant entities barely tick. Not for a main survival world |
