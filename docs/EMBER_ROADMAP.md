# EmberMC Roadmap

EmberMC is a production-grade Paper fork for large survival and economy
networks. Paper compatibility first; then measurable performance; then
aggressive exploit protection; then observability that lets an administrator
see *why* the server is doing what it is doing.

## Priorities, in order

1. Server stability
2. Correct gameplay behaviour
3. TPS / MSPT consistency
4. Plugin compatibility
5. Exploit prevention
6. Entity optimisation
7. Chunk optimisation
8. Network efficiency
9. Memory efficiency
10. Administrator control

A change lower on this list never wins against one higher on it. A benchmark
number is not a reason to remove vanilla behaviour.

## Every milestone follows the same loop

```
PLAN → IMPLEMENT → BUILD → TEST → PROFILE → BENCHMARK → DOCUMENT → COMMIT
```

Before any major optimisation is written, its plan records: the existing Paper
behaviour, the bottleneck, the proposed change, compatibility implications,
thread-safety implications, the expected improvement, and how it will be
measured. No milestone proceeds past a broken build.

---

## Milestone 1 - Fork infrastructure and branding ✅ (baseline)

- [x] Upstream Paper (`a2a42c5b`, MC 26.2) built unchanged
- [x] paperweight-patcher fork: `ember-api`, `ember-server`, `ember-checkstyle`
- [x] Upstream build scripts patched (brand manifest, source set wiring, Fill disabled)
- [x] Brand id `embermc:ember`, name `EmberMC`; reports Paper-compatible to plugins
- [x] `/ember status` - TPS, MSPT, players, entities, block entities, chunks, heap
- [x] `/ember version`
- [x] `ember.command.*` permissions
- [x] Startup banner (true-colour ANSI via Adventure; plain in latest.log)
- [x] Docs: README, BUILDING, UPDATING-UPSTREAM, LICENSE, DISTRIBUTION, this roadmap
- [x] Test server launched on a real box: Ember suite + floodgate + packetevents load, `/ember status` answers
- [ ] Full plugin-compatibility matrix recorded (see PLUGIN-COMPATIBILITY.md)

## Milestone 2 - Configuration framework ✅

- [x] `config/ember-global.yml`, `config/ember-world-defaults.yml`, `<world>/ember-world.yml`
  on Paper's Configurate stack (`EmberConfigurations extends Configurations<G, W>`), not a second YAML layer
- [x] Versioned (`_version`), constraint-validated, comment-persisting; transformations hook ready
- [x] Presets `vanilla · balanced (default) · performance · extreme` as the `profile` key
- [x] Every option's comment states reload-safe or restart-only; `/ember reload` reports both lists
- [x] `/ember config` shows effective values per world
- [x] Only options something reads are present; `entities.optimization` is the one marked
  "read now, applied from Milestone 4"
- [ ] Tests: parsing, validation, migration (with the first transformation)

## Milestone 3 - Profiler and instrumentation ✅

- [x] Always-on phase timing: scheduler, block ticks, chunks, block events, entities,
  block entities, connections, console commands, other - one `nanoTime` + one add per hook,
  ~40 hooks a tick with three worlds, per-world and per-tick 60 s rings, no per-tick allocation
- [x] Opt-in plugin attribution (event handlers + sync tasks) behind one volatile flag per site;
  sessions with auto-stop
- [x] `/ember profiler [start [s]|stop]`, `/ember plugins`, `/ember worlds`, `/ember entities`,
  `/ember chunks`, `/ember metrics` - every view says what it does not measure
- [x] Spike watchdog: threshold, cooldown, GC delta, per-phase and per-world breakdown of the
  offending tick, heaviest plugins when a session runs, report file + one console line
- [x] Metrics registry of `ember_*` gauges (suppliers, free when idle)
- [x] First measured win: Alternate Current redstone default (BENCHMARKS.md R1, 11× on block ticks)
- [x] `/ember tune show|apply|revert <preset>`: Paper/Spigot/Bukkit performance keys with backups,
  visible-effect notes and live safety guards
- [x] Opt-in Prometheus-style endpoint over the same gauges (`metrics.endpoint`, off by default, localhost)
- [x] Tests: `TimeRingTest` (statistics, window, wrap); phase accounting and watchdog cooldown still to cover

## Milestone 4 - Entity engine (first layer measured ✅)

- [x] Tiers on top of Paper's activation range: full ring / outer ring (every Nth tick, staggered) /
  Paper inactive beyond - `docs/optimisations/entity-tiers.md`
- [x] Exemptions so nothing a player interacts with is throttled: targets, leashes, damage, riders,
  projectiles, items, falling, swimming, burning, babies, breeding, Paper's always-active types
- [x] Per-world `entities.optimization` finally applied; global `entities.tiers.*` overrides and kill switch
- [x] `/ember entities` shows full vs reduced counts and each world's tier; `ember_entities_*` gauges
- [x] `autosave` profiler phase (the five-minute stall now has a name)
- [x] Measured with a player and 2,037 mobs (BENCHMARKS.md R3): entity phase -29% / -38% / -62% vs Paper behaviour
- [ ] Per-type tuning; villager hall and passive-mob benchmarks

## Milestone 5 - Pathfinding and collision

- [x] Reviewed: Paper already throttles recompute to once per 20 ticks and backs
  off failed follow-an-entity pathfinds (10 fails, 40-tick skip). Not reinvented.
- [x] Generalised that backoff to every pathfind, including the positional goals
  (wander, work-site, flee) Paper leaves uncovered: after failures-before-backoff
  failed A* searches to the same coarse target, skip it for backoff-ticks; any
  new or reachable target resets at once. `entities.pathfinding`, reload-safe.
  Pure `PathfindingBackoff.shouldSkip`, `PathfindingBackoffTest` (5).
  `/ember entities` "Pathfinds skipped", `ember_pathfinds_skipped` gauge.
  `docs/optimisations/pathfinding.md`
- [x] Reviewed: collision is already Paper-bounded (max-entity-collisions);
  oversized-group lag is handled there and by the item cap. Not reinvented.
- [ ] Optional: POI/target search throttling for distant mobs; live stuck-crowd
  benchmark on the box

## Milestone 6 - Items and XP

- [x] Reviewed: Paper already merges dropped items (radius) and XP orbs (value-grouped) well;
  not reinvented, to avoid a fake win
- [x] Live per-chunk item-entity cap (`entities.item-limits`, OFF by default): trims a loaded chunk
  within sweep-seconds instead of only at unload, oldest-first, EntityRemoveEvent fired - an
  anti-dupe-flood / lag-machine backstop Paper lacks live. `docs/optimisations/item-limits.md`
- [x] `/ember security` line; `ember_items_removed` gauge; `ItemLimitsTest`
- [ ] Optional: XP-orb per-area cap; live flood benchmark on the box

## Milestone 7 - Chunk engine

- [x] Reviewed: Paper's Moonrise chunk system already does async chunk saves,
  per-player concurrent load/generate limits (auto-tuned), autosave spread, and
  delayed unload. Load rate-limiting and save-smoothing are NOT reinvented - that
  would be a fake win. The Tuner already sets the knobs worth changing.
- [x] Chunk retention diagnostics (`chunks.retention-diagnostics`): `/ember chunks`
  now shows loaded / ticking / forced / plugin-held per world and names the
  plugins holding tickets; a rate-limited warning fires when one world passes
  `warn-threshold`. Read-only - never force-unloads a plugin's chunk. Gauges
  `ember_chunks_plugin_held`, `ember_chunks_force_loaded`. Pure
  `ChunkInsights.shouldWarn`, `ChunkInsightsTest` (3). `docs/optimisations/chunk-engine.md`
- [ ] Optional: portal/teleport pre-load hint; live elytra/mass-teleport read-out

## Milestone 8 - Packet Guard and exploit protection (core shipped)

- [x] Per-connection, per-category token-bucket limiter after Paper's all-packets limiter  - 
  `docs/optimisations/packet-guard.md`
- [x] Categories: movement, arm-swing, interact, inventory, book/sign, chat, command,
  tab-complete, recipe, creative, other; classified by packet simple name (unit-tested)
- [x] Burst allowance; payload-size limit for book/sign; actions log / warn / throttle / drop / kick
- [x] `/ember security` (per-category limit, action, allowed, blocked); `ember_packets_blocked` gauge;
  console warns rate-limited; never logs contents or auth
- [x] Reload-safe, per-category configurable, kill switch `security.packet-guard.enabled`
- [x] Tests: `TokenBucketTest`, `PacketCategoryTest`
- [ ] Plugin-message and decompression-exhaustion categories; live flood benchmark on the box

## Milestone 9 - Adaptive Performance Engine (brought forward; first responder shipped)

- [x] `AdaptiveEngine` state machine: normal / light / moderate / aggressive from tick p95 with
  entry thresholds, exit margin, hold-up and hold-down, one step at a time, ceiling  - 
  `docs/optimisations/adaptive-engine.md`; `AdaptiveEngineTest` pins it
- [x] First responder: entity tiers scale full ring (floor 25%) and outer interval (cap 10)
- [x] Observable: console line per change with the p95, `/ember status` Load row, `ember_adaptive_*` gauges
- [x] Disableable and reload-safe (`adaptive.*`)
- [ ] Further responders: pathfinding frequency, spawning work, non-critical deferral (each bounded)
- [ ] Server-level measurement under induced load

## Milestone 10 - Memory and network

- [x] Idle-RAM trimmer (`memory.idle-trim`, reload-safe): after after-minutes with
  zero players, one collection lets G1 uncommit unused heap so the process
  footprint drops. Runs only when empty, at most once per idle stretch. Honest
  about the AlwaysPreTouch/Xms=Xmx flag set pinning the heap (returns 0 then);
  uncommit-friendly flags documented. Pure `IdleMemory.dueForTrim`,
  `IdleMemoryTest` (3). `/ember status` line. `docs/optimisations/idle-memory.md`
- [x] Reviewed: vanilla + Paper already dirty-track entity data (sendDirtyEntityData,
  getNonDefaultValues) so metadata is already deduped - a "packet dedup" would be a
  fake win. Not reinvented.
- [x] Outbound network visibility (`/ember netstat start [sec] | stop`): time-boxed
  sample of clientbound traffic by category (entity move/metadata/velocity/equipment/
  spawn, chunk, light, block, sound, particle, player-info) with packets, bytes,
  per-second rates and share. Zero cost when off (one volatile read on the send
  path). NetSampler + OutboundCategory, OutboundCategoryTest (5). Connection.java
  patch. `docs/optimisations/network.md`
- [ ] Allocation profiling of hot paths; bounded caches with metrics

## Milestone 11 - Compatibility testing

- Representative plugin matrix (see PLUGIN-COMPATIBILITY.md)
- Behaviour-change register with compatibility toggles

## Milestone 12 - Benchmarking and production hardening

- Repeatable scenarios: 10 / 50 / 100 / 200 players; the stress set in
  BENCHMARKS.md; Paper vs Purpur vs EmberMC on identical hardware
- No performance claim is published without the benchmark that supports it
