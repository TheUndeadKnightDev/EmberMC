# Performance

**Status: FlintMC has made no performance changes yet.** The current build is
Paper `a2a42c5b` with FlintMC branding and the `/flint` command. It performs
exactly as that Paper build performs, and nothing in this document is a claim
about a measured result. See BENCHMARKS.md for how results will be produced.

This document is the standing rule set for every optimisation that follows.

## How an optimisation gets in

Every major optimisation is written down before it is written in code, under
`docs/optimisations/<name>.md`, with these headings:

1. **Existing Paper behaviour** — what the code does today, with file and
   method names from the pinned commit.
2. **Bottleneck** — the profile that shows it matters. A hunch is not a
   bottleneck.
3. **Proposed change** — what will be different, and what will not.
4. **Compatibility** — every Bukkit/Paper-observable difference, with the
   toggle that restores upstream behaviour and the register entry in
   PLUGIN-COMPATIBILITY.md.
5. **Thread safety** — which thread runs what, and why that is safe.
6. **Expected improvement** — a number, with the scenario it applies to.
7. **How it will be measured** — the BENCHMARKS.md scenario, or a new one.

Then: implement → build → test → profile → benchmark → document → commit. A
change whose benchmark does not show the expected improvement is reverted, not
kept because it "should" help.

## Standing rules

- Never remove vanilla behaviour for a benchmark number. Change *when* and
  *how often* work happens, not *whether* it happens — and put the "whether"
  behind a default-off toggle if it must exist at all.
- No mob freezes in front of a player. Distance tiers use the nearest *active*
  player and are overridden by anything gameplay-relevant: targeting, combat,
  leads, passengers, projectiles in flight, items near players.
- Every cache is bounded, has an invalidation story, and exposes a hit/miss
  metric. A cache that can grow without limit is a memory leak with a nicer
  name.
- Nothing moves off the main thread unless Paper's architecture already
  guarantees the access is safe there. "It seemed to work" is not a guarantee.
- Profiling is opt-in. The instrumentation that stays on all the time costs
  nothing measurable when the profiler is off.
- Adaptive behaviour has hysteresis, limits, an off switch, and shows up in
  `/flint status` when it is doing something.

## Where the work is expected to be

In rough order of expected payoff on a large survival network, from Paper's own
profiles and from what the other forks found worth doing:

1. Entity ticking tiers and activation (M4)
2. Pathfinding — failed-path caching, POI search throttling (M5)
3. Collision search bounds in dense entity groups (M5)
4. Hopper inventory search short-circuits (M6)
5. XP orb and item aggregation (M6)
6. Chunk-load smoothing on teleport and fast travel (M7)
7. Entity-tracker and metadata packet de-duplication (M10)

Each will get its own `docs/optimisations/` entry before work starts, and its
own row in BENCHMARKS.md results after.
