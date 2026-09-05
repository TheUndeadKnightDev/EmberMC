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

## Milestone 3 — Profiler and instrumentation

- Tick-phase timing hooks (world tick, entity tick, block entity tick, chunk
  tick, scheduler, network flush) that cost nothing when the profiler is off
- `/ember profiler [start|stop]`, `/ember entities`, `/ember chunks`,
  `/ember worlds`, `/ember plugins` (tick cost attribution with honest caveats)
- Lag-spike watchdog: capture context automatically when a tick exceeds a
  threshold, write an administrator-readable report
- Internal metrics registry; optional Prometheus-style endpoint (opt-in)

## Milestone 4 — Entity engine

- Adaptive ticking tiers by distance to active players: FULL / REDUCED / MINIMAL,
  with overrides for gameplay-critical entities (targeting a player, in combat,
  holding a lead, carrying a passenger, projectiles in flight, items near players)
- Builds on Paper's activation range rather than replacing it
- Per-world tuning; per-type tuning; all bounded and observable
- Benchmarks: 10k passive mobs, mob farm, villager hall

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

## Milestone 8 — Packet Guard and exploit protection

- Centralised `Ember Packet Guard`: per-player, per-category rate tracking with
  burst detection; payload size limits; malformed-input detection
- Actions per rule: LOG / WARN / THROTTLE / DROP / KICK
- Covers movement, interaction, inventory, book/NBT/component payloads,
  commands, tab completion, recipes, creative inventory, plugin messages,
  vehicle movement, decompression exhaustion
- `/ember security`, `/ember network`; metrics for rates, drops, throttles
- Never logs authentication or session data
- Tests: normal play under limits; hostile input fuzzing

## Milestone 9 — Adaptive Performance Engine

- Load levels from MSPT with hysteresis: NORMAL / LIGHT / MODERATE / AGGRESSIVE
- Responses are bounded, configurable, observable, disableable
- Feeds entity tiers, pathfinding frequency, spawning work, non-critical deferral

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
