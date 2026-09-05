# Pathfinding backoff

Milestone 5. A performance optimisation: it removes wasted A* pathfinder work
without changing where a mob can go.

## 1. Existing Paper behaviour

Paper already does real work here, and this builds on it rather than repeating
it. Two things matter:

- **Recompute throttle.** `PathNavigation.recomputePath()` will not rebuild a
  path more than once every 20 ticks (`timeLastRecompute`).
- **Failed-follow backoff.** `PathNavigation.moveTo(Entity target)` counts
  failures: after 10 consecutive failures to reach that entity, and while the
  path is still null, it skips pathfinding for 40 ticks (`pathfindFailures`,
  `lastFailure`). This is the exact idea below, but Paper applies it to **one**
  caller: following an entity.

## 2. Gap

The expensive operation is the A* search itself, `PathFinder.findPath`, inside
the terminal `createPath`. Every navigation funnels through it. Paper's backoff
guards only the follow-an-entity caller; every **positional** pathfind does not
get it:

- wander goals (`RandomStroll` and friends),
- move-to-block goals (work sites, beds, breeding, tempting),
- flee and move-towards-restriction goals.

A mob running one of these against a target it cannot reach keeps paying a full
A* search on its recompute throttle, indefinitely. The case that actually hurts
a server is a crowd: a wall of mobs at a base perimeter, none able to reach the
players inside. Paper's per-entity backoff helps each mob only after its own ten
failures and only for the entity-follow goal; the positional searches keep
running, once per mob per throttle, for as long as the crowd stands there.

## 3. Change

Apply Paper's proven rule at the one place every pathfind passes through, for
every caller.

- In the terminal `createPath`, just before building the search region and
  calling `findPath`, check a per-navigation backoff: if the last
  `failures-before-backoff` (default 8) searches to the **same target** all
  failed and fewer than `backoff-ticks` (default 40) have passed since the last
  failure, return null immediately and skip the A* search.
- After a search runs: a null result to the same target increments the failure
  count and stamps the tick; a different target, or any successful path, resets
  it at once.
- "Same target" is a coarse (4-block) key on one representative target position,
  so a mob genuinely re-heading somewhere new is never suppressed; only repeated
  attempts to essentially the same unreachable spot are.

The decision (`PathfindingBackoff.shouldSkip`) is a pure function; the
navigation holds three small fields (`emberFailures`, `emberLastFailTick`,
`emberLastTargetKey`) and nothing else.

## 4. Compatibility

Behaviour is preserved. A mob is never held off a target longer than
`backoff-ticks` (the same order as Paper's own 40), and only on a target it has
already failed to reach that many times in a row. The instant the target changes
or becomes reachable, the backoff clears, so a door opening or a block breaking
frees the mob within one window. It never suppresses a first attempt, never
touches a path that is succeeding, and changes no API. Paper's own entity-follow
backoff still runs ahead of this and is untouched. `entities.pathfinding.enabled:
false` removes the layer.

## 5. Thread safety

`createPath` runs on the server thread as part of entity ticking, where all
mob AI already runs; the per-navigation fields are touched only there. The one
shared piece is an `AtomicLong` skip counter for the metric.

## 6. Cost

When the backoff is not armed (the normal case): one coarse key computation over
the target set and a couple of integer comparisons, then the search runs exactly
as before. When it is armed: the same handful of comparisons and an early return,
in place of a full A* search. There is no allocation and no per-tick scan; the
work happens only when a pathfind was going to happen anyway.

## 7. Diagnostics

`/ember entities` shows "Pathfinds skipped" (A* searches avoided since start),
and `ember_pathfinds_skipped` is exported to the metrics endpoint. On an idle or
healthy server it stays near zero; it climbs only when mobs are actually stuck
against unreachable targets, which is exactly when it is saving work.

## 8. How it is measured

`PathfindingBackoffTest` pins the pure decision: no backoff before the failure
threshold, backoff active inside the window once armed, backoff expires after the
window, a negative elapsed is never treated as armed, and a zero/negative config
disables it. The live effect is read on the box: build a pen of mobs walled off
from a player, watch `/ember entities` show the skip counter climb and the entity
tick phase in `/ember profiler` drop, with the mobs still moving normally once a
route opens.

## 9. Measurements

_Unit tests pass. Live stuck-crowd measurement recorded in BENCHMARKS.md when
run on the box._
