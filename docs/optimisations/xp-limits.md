# XP-orb limits

Milestone 6, the experience half. A live per-chunk cap on experience-orb
entities that never throws away a single point of experience.

## 1. Existing Paper behaviour

Paper already merges nearby experience orbs: `experience-merge-max-value` lets
two orbs within range combine into one whose value is the sum, up to a cap. That
keeps the orb count down in normal play, and it is why mending still works after
you turn it up. What Paper does not do is bound the number of orb entities a
single loaded chunk may hold. Merging is pairwise and opportunistic; a grinder
that drops experience faster than the merges resolve, or an XP-dupe, can still
pile thousands of orb entities into one chunk. Each one is an entity that ticks
and is tracked to every player in range.

## 2. Gap

A hard ceiling on orb count per loaded chunk, without the obvious cost. The
naive way to cap orbs is to delete the overflow, and that is unacceptable:
experience is a player's, earned or paid for, and deleting it is a bug, not an
optimisation. So the cap has to preserve the total.

## 3. Change

`entities.xp-limits`. When a loaded chunk holds more than `max-per-chunk` orbs,
the overflow orbs' experience is folded into the orbs that remain and the
now-empty overflow entities are removed. The number of entities drops; the total
experience on the ground is exactly the same.

- The youngest orbs are kept (a player's freshest drops survive intact); the
  oldest overflow is what gets folded in, so nothing a player just earned
  vanishes from under them.
- A survivor's value is summed and clamped to `Integer.MAX_VALUE`, so even a
  pathological pile collapses to a handful of high-value orbs a player can still
  pick up.
- Off by default. It changes entity count, not gameplay, but it is an anti-abuse
  backstop, so an administrator turns it on deliberately with a generous cap.
- Removals fire Bukkit's `EntityRemoveEvent`; the collapsed count shows in
  `/ember security` and is exported as `ember_xp_orbs_merged`.

This is the difference from [item limits](item-limits.md), which discards
overflow items: items are fungible and a dupe flood of them is just lag, but
experience is owned, so it is merged, never deleted.

## 4. Compatibility

Total experience is conserved, so a farm's output is unchanged; only the number
of entities carrying it drops. Mending and pickup behave exactly as before. With
the feature off (the default) nothing happens at all. `entities.xp-limits.enabled:
false` removes it; the cap and sweep interval are configurable.

## 5. Cost

One pass over each world's experience-orb entities every `sweep-seconds`,
bucketed by chunk; nothing per tick and nothing per orb outside the sweep. Main
thread, same shape as the item sweep.

## 6. How it is measured

The overflow arithmetic is the same pure function the item cap uses
(`ItemLimits.overflow`, unit-tested). The value-preserving fold is confirmed on
the box by spawning a dense field of orbs, turning the cap on, and checking that
`/ember security` shows the collapse while a test player's total pickup is
unchanged.

## 7. Measurements

_Unit tests pass. Live XP-flood conservation check recorded in BENCHMARKS.md when
run on the box._
