# Doctor

A diagnostics advisor. Not an optimisation itself: it reads the ones EmberMC
already measures and tells you which one matters right now and what to change.

## 1. The problem

By this point EmberMC measures a lot: the profiler breaks the tick into phases,
the entity engine reports full and reduced counts, the chunk diagnostics show
what is held loaded, the adaptive engine tracks load, `/ember status` shows the
heap. That is a lot of screens, and it still leaves the administrator to work out
which number is the problem and what to do about it. Most people never do.

## 2. What it does

`/ember doctor` reads all of it at once and prints a short, ranked list of
findings, each with a plain recommendation:

```
Doctor
  [CRITICAL] Tick is over budget: 63.2 ms (20 TPS needs under 50).
      Heaviest phase: entities (41.0 ms). Run /ember profiler for the full breakdown.
  [WARN] Plugins are holding 1,240 chunks loaded.
      See /ember chunks for which plugin. A chunk kept loaded ticks and costs memory.
  [NOTICE] Entities are the biggest tick cost and entity tiers are off.
      Turn on entities.tiers, or apply a performance profile with /ember tune.
```

Findings are sorted most severe first. When nothing needs attention it says so,
with the current MSPT and TPS.

## 3. What it looks at

- **Tick budget.** MSPT against 30 / 40 / 45 ms, with the heaviest tick phase
  named from the profiler.
- **Entities.** If the entity phase is the largest share of the tick and the
  tiers are off, it says to turn them on (and to run `/ember bench` first).
- **Chunk retention.** Plugin-held chunks over the configured threshold, and
  large force-load counts that persist in `level.dat`.
- **Heap.** Used-versus-max pressure, and an empty server holding heap with the
  idle trimmer off.
- **Adaptive engine.** Whether it is currently easing load, which means the
  server has been over its p95 threshold.

## 4. Honest by construction

Every recommendation points at an existing EmberMC control or a standard JVM
flag, never a vague "optimise". It advises and never acts: it will not change a
config, force a tier, or unload a chunk. And it only raises what the numbers
support, so on a healthy server it is one green line, not a wall of invented
problems.

## 5. How it is measured

The assessment is a pure function of a snapshot of the numbers
(`Doctor.assess(Snapshot)`), so `DoctorTest` pins every rule against fixed
inputs: a healthy server is one OK line, an over-budget tick is CRITICAL and
names the heaviest phase, entities-heavy with tiers off advises turning them on,
plugin-held chunks over threshold warns, high heap warns, an empty server without
the idle trimmer is flagged, and findings come out sorted most severe first. The
command only gathers the live snapshot and prints; the judgement is all tested.
