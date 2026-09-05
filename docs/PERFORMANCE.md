# Performance

**Status: EmberMC has made no performance changes yet.** The current build is
Paper `a2a42c5b` with EmberMC branding, configuration, and the profiler. It
performs as that Paper build performs, and nothing in this document is a claim
about a measured result. What it does have, since Milestone 3, is the
instrument the rest of this document depends on: always-on tick-phase timing
(about a microsecond a tick), opt-in per-plugin attribution, and a spike
watchdog that writes down what a bad tick was doing. See BENCHMARKS.md for how
results will be produced.

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

## What has shipped

**Alternate Current by default** (Milestone 3). Paper ships a redstone engine
that is 11× cheaper on a dust plane (BENCHMARKS.md R1) and leaves it off.
EmberMC turns it on for new installs; `misc.redstone-implementation: VANILLA`
restores upstream behaviour.

**The tuner** (`/ember tune`, Milestone 3). Paper, Spigot and Bukkit already
expose the levers every optimisation guide tells administrators to hand-edit:
activation and tracking ranges, villager tick rates, hopper checks, spawn
limits, collision caps, explosion optimisation. `/ember tune show <preset>`
lists what a preset would change and what a player could notice;
`/ember tune apply` writes it with backups; `/ember tune revert` writes upstream
defaults back. A change whose safety depends on the plugin set - disabling the
hopper move event - is refused while any plugin listens for it. These values
are settled practice, not EmberMC measurements; the profiler is there to
measure them on your server.

**Entity tiers** (Milestone 4, first layer; `docs/optimisations/entity-tiers.md`).
Paper's activation range is binary; EmberMC splits it into a full ring and an
outer ring that ticks fully every Nth tick, with everything a player interacts
with exempt. Expected 0.63–0.78× of the entity phase on uniform crowds at
balanced/performance; **not yet measured** - the run needs a player at the rig.

**Autosave as a phase** (Milestone 4). Paper saves players, level data and
chunks at the end of every tick, with a full save every `autosave` ticks
(6000 = five minutes). On the test host that full save stalls the tick for
0.3–1.9 s on Paper, Purpur and EmberMC alike; only EmberMC's watchdog reported
it. It now has its own phase so the report says "autosave" instead of "other".

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
  `/ember status` when it is doing something.

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
