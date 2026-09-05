# Benchmark harness

Milestone 12. Not an optimisation: a way to prove the ones that are, on your own
server, without trusting a number in a listing.

## 1. The problem with fork benchmarks

Every fork claims to be faster, and almost none of them can be reproduced. The
number was measured on hardware you do not have, with a load you cannot recreate,
by a rig nobody publishes. EmberMC's headline claim is the entity tiers, so the
tiers are what has to be reproducible.

## 2. What it does

`/ember bench` measures the entity tick phase on the load your server is carrying
right now, at each tier, and prints the saving against vanilla.

- It holds a tier steady for a short settle window, then reads the entity phase
  mean over a fixed measure window, and cycles vanilla, balanced, performance and
  extreme.
- `vanilla` is the tiers switched off: that row is your Paper baseline, measured
  on the same box, same load, seconds apart from the others. That is the honest
  comparison, not one server versus a different one.
- It prints each tier's entity tick time and the percentage below the vanilla
  baseline.

```
/ember bench            measure window 6s per tier (about 36s total)
/ember bench 10         10s measure window per tier
```

Example, on a populated survival world:

```
Ember benchmark result (entity tick, vanilla = baseline):
  vanilla       14.76 ms   baseline
  balanced      10.43 ms   29% lower
  performance    9.19 ms   38% lower
  extreme        5.67 ms   62% lower
```

## 3. Why it is honest

- **Nothing is spawned and nothing in the world changes.** The tiers are only a
  decision about how often the mobs already loaded get their full tick, so
  switching tier and reading the profiler is the entire measurement. No test
  mobs, no world edits, no cleanup that could go wrong.
- **The baseline is real.** The `vanilla` row is this server with the tiers off,
  not a figure from elsewhere, so the percentages are the saving you would
  actually get by turning the tiers on here.
- **It needs real load to show anything.** On an empty server every row is near
  zero and it says so; the numbers only separate when there are mobs to tick,
  which is exactly when the tiers matter. It will not manufacture a difference.

## 4. Safety

Operator only. One run at a time. The adaptive engine is suspended for the run
so it cannot move the tier mid-measurement, and every piece of state (the forced
tier, the load response, the adaptive engine) is restored when the run finishes.
The measurement is passive: it reads the same profiler ring `/ember profiler`
reads, so it adds nothing to the tick beyond the tier switch itself.

## 5. How it is measured

The benchmark is the measurement, so what is pinned by tests is the machinery it
reads: the ring statistics (`TimeRingTest`) and the tier decision
(`AdaptiveEngineTest`, `TunePresetsTest`). The command is validated on the box by
running it under a known entity load and confirming the vanilla row matches a
plain Paper build's entity phase and the tier rows match `/ember profiler` read
by hand.

## 6. Measurements

_The R3 table in BENCHMARKS.md was produced exactly this way, by hand, before the
command existed; `/ember bench` is that procedure made repeatable. Live command
output recorded there when run._
