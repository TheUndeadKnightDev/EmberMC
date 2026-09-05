# Item limits

Milestone 6. Paper already merges dropped items and XP orbs well, so EmberMC
does not reinvent that. What Paper does not do is bound a **loaded** chunk's
item count - and that is the shape a dupe machine, a broken farm, or a deliberate
lag machine takes.

## 1. Existing Paper behaviour

Items merge by radius (`spigot.yml merge-radius.item`); orbs merge by value.
`entity-per-chunk-save-limit` trims item entities, but **only when the chunk is
written to disk**. While a chunk stays loaded it can hold tens of thousands of
item entities, each ticking gravity, fire and merge scans.

## 2. Gap

On an economy server, an item dupe or a runaway grinder fills a loaded chunk
with items faster than they despawn, and the server ticks every one until the
chunk unloads - which, around active players, is never. That is real, sustained
lag with no live ceiling.

## 3. Change

`ItemLimits`: every `sweep-seconds` (10 by default), each world's item entities
are bucketed by chunk; any chunk over `max-per-chunk` has its oldest excess
items removed. Oldest-first means a player's fresh drop is the last to go and
only long-settled overflow is culled. Removals fire Bukkit's
`EntityRemoveEvent`. **Off by default** - it removes items, which is a gameplay
change - and meant to be enabled with a generous cap as a backstop, not a farm
nerf.

## 4. Compatibility

With the feature off (default), nothing changes. On, the only observable effect
is that a chunk cannot hold more than the cap in dropped items; the excess that
would have sat there until unload is removed, with an event plugins can see.
Toggle `entities.item-limits.enabled`; register row added.

## 5. Thread safety

The sweep runs on the main thread from the profiler's per-tick hook, at most
once per `sweep-seconds`. It reads and removes through the Bukkit API on the
main thread. Nothing async.

## 6. Cost

One pass over each world's item entities per sweep (default every 10 s),
bucketed into a fastutil long-keyed map; nothing per tick, nothing per item
between sweeps. The selection of what to remove is a pure function
(`overflow`), unit-tested.

## 7. Measurement

`ItemLimitsTest` pins the pure overflow calculation. Live: drop several thousand
items in one chunk with the cap on and watch the count fall back to the cap
within a sweep while MSPT stays flat; `/ember security` and `ember_items_removed`
report the cull.
