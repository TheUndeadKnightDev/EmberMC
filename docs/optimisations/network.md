# Network visibility

Milestone 10 (network half). The honest version, the same as the chunk engine:
the heavy lifting is already done upstream, so this adds sight, not a rewrite.

## 1. Existing Paper behaviour

Vanilla and Paper already avoid redundant entity packets. Entity data is
dirty-tracked: `ServerEntity.sendDirtyEntityData` sends only the values that
changed, `getNonDefaultValues` seeds a new tracker with only what differs from
default, movement packets are sent on an interval and skipped when nothing moved
enough, and Paper adds allocation and item-frame-visibility optimisations on top.
There is no pile of duplicate metadata to remove; "packet de-duplication" would
be reimplementing what the dirty-tracking already does. Not done.

## 2. Gap

What is missing is not an optimisation but a measurement. When a server's
downstream bandwidth is high, or a particular area lags only for the players in
it, there is no built-in way to see the shape of the outbound traffic: how much
is the entity tracker (moves, metadata), how much is chunk and light data, how
much is sound and particle spam from a plugin. The profiler shows where tick time
goes; nothing showed where the bytes go.

## 3. Change

A time-boxed outbound sample, off by default.

- **`/ember netstat start [seconds]`** turns the sampler on for a window
  (default 15s, capped at 600). While it runs, each clientbound packet is
  classified by type and its estimated size added to a per-category tally.
- **`/ember netstat`** prints the result: packets and bytes per category, the
  per-second rates, and each category's share, sorted by bytes, with a total.
  Categories are the ones that matter for diagnosis: entity move, metadata,
  velocity, equipment, spawn; chunk, light, block; sound, particle, player-info.
- **`/ember netstat stop`** ends the window early. The counts stay readable until
  the next start.

## 4. Zero cost when off

The send path checks a single `volatile boolean`. When no sample is running that
read returns immediately and nothing else happens: no classification, no size
estimate, no allocation. The packet's class name and size estimate are only
touched while a sample is active, which is a deliberate, bounded diagnostic
window an operator chose to open. Nothing about what is sent is changed; the
sampler only counts.

## 5. Thread safety

Packets are sent from more than one thread (the main thread and Netty threads),
so the tallies are `AtomicLongArray` and the on/off flag is `volatile`. The
sampler touches no world or entity state.

## 6. Diagnostics and honesty

The read-out ends with the plain statement that metadata is already deduplicated
upstream and nothing is being dropped. This is a window into traffic the server
was always sending, not a claim to have reduced it. If a category looks
surprisingly large (a plugin flooding particles, an entity farm's metadata), that
is a lead for the operator to act on, at the plugin or design level.

## 7. How it is measured

`OutboundCategoryTest` pins the classifier: entity move vs metadata vs velocity
vs equipment are distinct, spawns classify before the generic entity bucket,
chunk/light/block are separated, and the noisy extras and unknowns land where
expected. The tallies and the window timing are read on the box with
`/ember netstat` during normal play and under a spawned entity load.

## 8. Measurements

_Unit tests pass. Live outbound sample recorded in BENCHMARKS.md when run on the
box._
