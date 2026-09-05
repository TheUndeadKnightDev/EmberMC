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

## Milestone 1 — Fork infrastructure and branding ✅ (baseline)

- [x] Upstream Paper (`a2a42c5b`, MC 26.2) built unchanged
- [x] paperweight-patcher fork: `ember-api`, `ember-server`, `ember-checkstyle`
- [x] Upstream build scripts patched (brand manifest, source set wiring, Fill disabled)
- [x] Brand id `embermc:ember`, name `EmberMC`; reports Paper-compatible to plugins
- [x] `/ember status` — TPS, MSPT, players, entities, block entities, chunks, heap
- [x] `/ember version`
- [x] `ember.command.*` permissions
- [x] Startup banner (true-colour ANSI via Adventure; plain in latest.log)
- [x] Docs: README, BUILDING, UPDATING-UPSTREAM, LICENSE, DISTRIBUTION, this roadmap
- [x] Test server launched on a real box: Ember suite + floodgate + packetevents load, `/ember status` answers
- [ ] Full plugin-compatibility matrix recorded (see PLUGIN-COMPATIBILITY.md)

## Milestone 2 — Configuration framework ✅

- [x] `config/ember-global.yml`, `config/ember-world-defaults.yml`, `<world>/ember-world.yml`
  on Paper's Configurate stack (`EmberConfigurations extends Configurations<G, W>`), not a second YAML layer
- [x] Versioned (`_version`), constraint-validated, comment-persisting; transformations hook ready
- [x] Presets `vanilla · balanced (default) · performance · extreme` as the `profile` key
- [x] Every option's comment states reload-safe or restart-only; `/ember reload` reports both lists
- [x] `/ember config` shows effective values per world
- [x] Only options something reads are present; `entities.optimization` is the one marked
  "read now, applied from Milestone 4"
- [ ] Tests: parsing, validation, migration (with the first transformation)

## Milestone 3 — Profiler and instrumentation ✅

- [x] Always-on phase timing: scheduler, block ticks, chunks, block events, entities,
  block entities, connections, console commands, other — one `nanoTime` + one add per hook,
  ~40 hooks a tick with three worlds, per-world and per-tick 60 s rings, no per-tick allocation
- [x] Opt-in plugin attribution (event handlers + sync tasks) behind one volatile flag per site;
  sessions with auto-stop
- [x] `/ember profiler [start [s]|stop]`, `/ember plugins`, `/ember worlds`, `/ember entities`,
  `/ember chunks`, `/ember metrics` — every view says what it does not measure
- [x] Spike watchdog: threshold, cooldown, GC delta, per-phase and per-world breakdown of the
  offending tick, heaviest plugins when a session runs, report file + one console line
- [x] Metrics registry of `ember_*` gauges (suppliers, free when idle)
- [x] First measured win: Alternate Current redstone default (BENCHMARKS.md R1, 11× on block ticks)
- [x] `/ember tune show|apply|revert <preset>`: Paper/Spigot/Bukkit performance keys with backups,
  visible-effect notes and live safety guards
- [x] Opt-in Prometheus-style endpoint over the same gauges (`metrics.endpoint`, off by default, localhost)
- [x] Tests: `TimeRingTest` (statistics, window, wrap); phase accounting and watchdog cooldown still to cover

## Milestone 4 — Entity engine (first layer measured ✅)

- [x] Tiers on top of Paper's activation range: full ring / outer ring (every Nth tick, staggered) /
  Paper inactive beyond - `docs/optimisations/entity-tiers.md`
- [x] Exemptions so nothing a player interacts with is throttled: targets, leashes, damage, riders,
  projectiles, items, falling, swimming, burning, babies, breeding, Paper's always-active types
- [x] Per-world `entities.optimization` finally applied; global `entities.tiers.*` overrides and kill switch
- [x] `/ember entities` shows full vs reduced counts and each world's tier; `ember_entities_*` gauges
- [x] `autosave` profiler phase (the five-minute stall now has a name)
- [x] Measured with a player and 2,037 mobs (BENCHMARKS.md R3): entity phase −29% / −38% / −62% vs Paper behaviour
- [ ] Per-type tuning; villager hall and passive-mob benchmarks

## Milestone 5 — Pathfinding and collision

- Short-lived failed-path cache keyed on (entity type, start region, goal,
  world revision); invalidated on block change in the affected region
- POI / target search throttling for distant mobs
- Collision search bounds for oversized entity groups; lag-machine safeguards
- Tests: cache invalidation; AI still reaches reachable targets

## Milestone 6 — Items and XP

- XP orb aggregation where semantics allow (same owner rules, mending untouched)
- Smarter dropped-item merging preserving metadata, enchantments, custom items,
  pickup restrictions; Bukkit `ItemMergeEvent` still fired
- Benchmarks: large XP farm, large item pile

## Milestone 7 — Chunk engine

- Chunk-load rate limiting per player with queueing, tuned so legitimate
  exploration and elytra travel are never blocked
- Teleport and portal chunk-load smoothing
- Diagnostics for unnecessary chunk retention
- Benchmarks: rapid elytra travel, mass teleport, generation stress

## Milestone 8 — Packet Guard and exploit protection (core shipped)

- [x] Per-connection, per-category token-bucket limiter after Paper's all-packets limiter —
  `docs/optimisations/packet-guard.md`
- [x] Categories: movement, arm-swing, interact, inventory, book/sign, chat, command,
  tab-complete, recipe, creative, other; classified by packet simple name (unit-tested)
- [x] Burst allowance; payload-size limit for book/sign; actions log / warn / throttle / drop / kick
- [x] `/ember security` (per-category limit, action, allowed, blocked); `ember_packets_blocked` gauge;
  console warns rate-limited; never logs contents or auth
- [x] Reload-safe, per-category configurable, kill switch `security.packet-guard.enabled`
- [x] Tests: `TokenBucketTest`, `PacketCategoryTest`
- [ ] Plugin-message and decompression-exhaustion categories; live flood benchmark on the box

## Milestone 9 — Adaptive Performance Engine (brought forward; first responder shipped)

- [x] `AdaptiveEngine` state machine: normal / light / moderate / aggressive from tick p95 with
  entry thresholds, exit margin, hold-up and hold-down, one step at a time, ceiling —
  `docs/optimisations/adaptive-engine.md`; `AdaptiveEngineTest` pins it
- [x] First responder: entity tiers scale full ring (floor 25%) and outer interval (cap 10)
- [x] Observable: console line per change with the p95, `/ember status` Load row, `ember_adaptive_*` gauges
- [x] Disableable and reload-safe (`adaptive.*`)
- [ ] Further responders: pathfinding frequency, spawning work, non-critical deferral (each bounded)
- [ ] Server-level measurement under induced load

## Milestone 10 — Memory and network

- Allocation profiling of hot paths; bounded caches with metrics
- Netty allocation review; entity-tracker and metadata packet de-duplication
- Every buffer-lifetime change documented and tested

## Milestone 11 — Compatibility testing

- Representative plugin matrix (see PLUGIN-COMPATIBILITY.md)
- Behaviour-change register with compatibility toggles

## Milestone 12 — Benchmarking and production hardening

- Repeatable scenarios: 10 / 50 / 100 / 200 players; the stress set in
  BENCHMARKS.md; Paper vs Purpur vs EmberMC on identical hardware
- No performance claim is published without the benchmark that supports it
