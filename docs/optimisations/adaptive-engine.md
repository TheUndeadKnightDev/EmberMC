# Adaptive performance engine

Milestone 9, brought forward: it needs only the profiler (M3) and the entity
tiers (M4), both of which exist, and it is pure logic that can be tested
without a server.

## 1. Existing Paper behaviour

None. Paper has no load-responsive behaviour: every setting is what the
administrator typed, and a server that is falling behind falls behind until a
human notices. The nearest thing is the watchdog, which reports; it does not
act.

## 2. Bottleneck

Not a hot path but a gap: the settings that would keep a server at 20 TPS
through a spike (a raid on a farm, a hundred players logging in after a
restart) are too aggressive to run all the time, and the settings that feel
right at normal load are too generous for the spike. Administrators pick one
and live with the other.

## 3. Proposed change

A state machine, `AdaptiveEngine`, fed once a second with the tick's p95 over
the last five seconds, deciding one of four levels:

| Level | entry (p95) | full-ring scale | outer-ring interval scale |
| --- | --- | --- | --- |
| normal | — | 1.0 | 1 |
| light | > 35 ms | 0.9 | 1 |
| moderate | > 45 ms | 0.75 | 2 |
| aggressive | > 50 ms | 0.6 | 4 |

Hysteresis, explicitly: a level is entered after the p95 has been above its
threshold for `hold-up-seconds` (5), left after it has been below the
threshold minus `exit-margin-ms` (5) for `hold-down-seconds` (20). Levels move
one step at a time in both directions. A `ceiling` caps how far it may go.

The engine changes nothing itself. Its level is read by responders, each of
which applies the multipliers **inside its own bounds**. Today the one
responder is the entity tiers: full ring never below 25% of the activation
range, outer interval never above 10. So the worst the engine can do to a mob
is make one at mid-distance move at a tenth speed while the mobs around the
player keep ticking fully. No other system is touched; spawning, chunk loading
and redstone are untouched by design until each has its own bounded response.

Observability: `/ember status` shows the level and the number of changes since
start; a console line is written on every change (warn going up, info coming
down) with the p95 that caused it; `ember_adaptive_level` and
`ember_adaptive_changes` gauges. `adaptive.enabled: false` returns every
responder to its preset at the next second.

## 4. Compatibility

Observable only through the entity tiers' observable effect (see
`entity-tiers.md`), and only while the server is already over 35 ms p95 - that
is, already visibly lagging. No API change. Toggle: `adaptive.enabled`.

## 5. Thread safety

The engine runs on the main thread from the profiler's tick hook. The
multipliers it publishes are two volatile fields read on the main thread by
the tiers. Nothing else.

## 6. Expected improvement

Not a throughput gain in itself: the point is that the entity-tier saving (see
`entity-tiers.md`: roughly 0.78 / 0.63 / 0.37 of the entity phase at the three
presets) is applied *when it is needed* rather than always or never. Under a
spike, a `balanced` server behaves like `performance` after five seconds and
like `extreme` after fifteen, then walks back over about a minute once the
spike passes.

## 7. How it will be measured

Unit tests pin the state machine (`AdaptiveEngineTest`: rise after hold, blips
ignored, one step at a time, exit margin, ceiling). The server-level effect is
the entity-tier measurement under induced load: the zombie pen from
`entity-tiers.md` with enough mobs to push p95 past 50 ms, watching
`/ember status` climb and `/ember profiler` entities-phase fall.

## 8. Measurements

_Unit tests pass (see EmberTestSuite). Server measurement pending the same
player-at-the-pen run as the entity tiers._
