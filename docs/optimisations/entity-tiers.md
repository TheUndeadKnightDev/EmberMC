# Entity tiers

Milestone 4, first layer. The plan below was written before the code, as
PERFORMANCE.md requires; the measurement section is filled in as runs happen.

## 1. Existing Paper behaviour

`io.papermc.paper.entity.activation.ActivationRange` (Paper's "EAR 2"). Once
per world tick, `activateEntities` walks every player, inflates one bounding box
per `ActivationType` (animals 32, monsters 32, raiders 64, misc 16, water 16,
villagers 32, flying 32 — `spigot.yml`), fetches every entity inside the largest
box, and stamps `entity.activatedTick = currentTick` on those inside their type's
box. In `ServerLevel.tickNonPassenger`, `checkIfActive` turns that into a boolean:
inside the range → `entity.tick()` (full); outside → `entity.inactiveTick()`
(goal selectors at a crawl, no movement), with a set of immunities
(`checkEntityImmunities`) that wake an inactive entity when it is on fire, in
water, falling, has a target, is hurt, is a baby, and so on.

The model is **binary**. Every entity inside the range costs a full tick every
tick, however far from the player it is; every entity outside it is frozen.

## 2. Bottleneck

On a populated survival server the entity phase is dominated by mobs that are
inside the activation range but far enough from any player that nobody is
looking at them closely: the back of a farm, the far edge of a village, mobs
across a lake. Administrators who want to save time shrink the ranges and mobs
then freeze visibly near players; administrators who want mobs to look alive
widen them and pay full price for everything in view. Paper's own inactive tick
is not a middle ground — it stops movement.

## 3. Proposed change

Split Paper's active range in two, per type, per player:

- **Full ring**: within `fullFraction × range` of a player. Unchanged: full
  tick every tick.
- **Outer ring**: between the full ring and the activation range. Full tick
  every `interval`-th tick, staggered by entity id; `inactiveTick()` on the
  ticks between.
- Beyond the activation range: unchanged (Paper inactive).

Presets: balanced 0.75 / 2, performance 0.5 / 2, extreme 0.4 / 4, chosen per
world through `entities.optimization` (inherit / vanilla / balanced /
performance / extreme) with global overrides `entities.tiers.full-range-fraction`
and `entities.tiers.reduced-interval`; `entities.tiers.enabled: false` removes
the feature entirely.

**Exempt, always full tick, anywhere:** players; vehicles and their riders;
projectiles; items and XP orbs; anything not on the ground, on fire, or being
pushed by water; any living entity with `hurtTime > 0` or jumping; any mob with
a target, a leash holder, or `isAggressive()`; baby or breeding animals; types
Paper marks `defaultActivationState`. The rule these implement: **nothing a
player is interacting with is ever throttled.**

Implementation: one `long emberFullTick` on `Entity`; one extra `AABB` per
`ActivationType` (`emberFullBox`); in `activateEntity`, one extra intersect
test that stamps `emberFullTick`; in `tickNonPassenger`, one call that reads two
fields and, only for outer-ring entities, runs the exemption checks. The world's
tier is resolved once per tick in `activateEntities` and cached on
`ServerLevel`.

## 4. Compatibility

Observable: a mob in the outer ring moves at `1/interval` of its normal speed
and reacts a little later to stimuli that are not on the exemption list
(e.g. a villager wandering, a cow grazing). Entities never freeze (that was
already possible on Paper outside the range). Bukkit events fire as before on
the ticks the entity does take. No API surface changes.

Toggles: `entities.tiers.enabled: false` (global), or `entities.optimization:
VANILLA` per world. Register row added in PLUGIN-COMPATIBILITY.md.

## 5. Thread safety

Main thread only. The fields written are written from the world tick and read
from the same world tick. No collections are introduced. No async.

## 6. Expected improvement

For entities uniformly spread inside a player's activation range, a full ring
at fraction *f* and interval *N* costs roughly `f² + (1 − f²)/N` of Paper's
entity-phase time: balanced ≈ 0.56 + 0.22 = **0.78**, performance
≈ 0.25 + 0.38 = **0.63**, extreme ≈ 0.16 + 0.21 = **0.37**. Real farms are
not uniform (they cluster where the player is not), so the real saving on the
entity phase is expected to be larger than the uniform estimate. The
exemptions reduce it where mobs are fighting or being farmed actively, which
is the point.

## 7. How it will be measured

`/ember profiler` entities-phase mean and p95 over 5 s, with one player standing
at the centre of a pen of 400–500 zombies (spawned via console, force-loaded,
`mobGriefing false`), first with `entities.tiers.enabled: false`, then
`balanced`, `performance`, `extreme`, applied with `/ember reload` on the same
running server. `/ember entities` reports full vs reduced counts per tick so
the split can be seen directly. A player is required: with no player online,
Paper deactivates everything and the tiers do nothing.

## 8. Measurements

Measured 2026-09-05 with a player and 2,037 mobs (BENCHMARKS.md R3): entity phase 14.76 ms (OFF/Paper) -> 10.43 (balanced) -> 9.19 (performance) -> 5.67 (extreme); MSPT 16.7 -> 6.5. Measured fractions 0.71/0.62/0.38 vs predicted 0.78/0.63/0.37. Mobs inside the player's full ring ticked fully at every tier.
