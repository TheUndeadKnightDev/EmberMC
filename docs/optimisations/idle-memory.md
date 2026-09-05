# Idle memory trim

An optional milestone extra. Not a tick optimisation: it lowers the memory a
server holds while nobody is playing, so an idle instance on a shared host stops
sitting on hundreds of megabytes it is not using.

## 1. The problem

A JVM commits heap as the server warms up and, by default, keeps it. Peak load
happens with players online; the committed heap it needed for that peak stays
committed at 3am with the server empty. On a box packed with instances, or a VPS
sized to a budget, that idle footprint is pure waste: RAM the OS accounts to the
process and cannot lend elsewhere.

Paper does not address this. It is a JVM behaviour, not a server one.

## 2. What EmberMC does

`memory.idle-trim`. When the server has had **zero players for `after-minutes`**
(default 5), EmberMC asks the JVM for one collection. Under G1 a collection is
where unused heap regions are uncommitted, so the committed heap, and the real
process footprint, drop to what the empty server actually needs.

- It runs **only with nobody online**, so the collection pause has no one to
  affect.
- It runs **at most once per idle stretch**. After it trims it waits until a
  player has joined and left again before it will trim a second time, so an
  always-empty server does not GC on a loop.
- The check is one player count and a subtraction per tick. When anyone is
  online it does nothing.

`/ember status` shows the trim state and how much the last trim returned; the
trim also logs one line with the before and after numbers.

## 3. The catch, stated plainly

The trim only lowers the footprint the OS sees **if the JVM is allowed to give
heap back**. The popular "Aikar" flag set pins the whole heap on purpose:

    -Xms<N>G -Xmx<N>G -XX:+AlwaysPreTouch

`Xms` equal to `Xmx` commits the maximum up front; `AlwaysPreTouch` faults every
page in at boot so it is all resident. That is deliberate: it trades idle
footprint for steady latency, and pre-touched pages never cause a commit stall
mid-tick. With those flags the trim runs, collects, and returns **0 MB**, which
is exactly what `/ember status` will show. The feature is honest about this
rather than pretending a GC beats the flags.

## 4. Flags that let it work

To let idle heap actually come back, size the floor low and let the ceiling
float, and drop pre-touch:

    -Xms512M -Xmx8G -XX:+UseG1GC -XX:G1PeriodicGCInterval=300000 -XX:+G1PeriodicGCInvokesConcurrent

- `Xms512M` (not equal to `Xmx`): the heap can shrink back toward the floor.
- no `AlwaysPreTouch`: pages are freed as regions uncommit.
- `G1PeriodicGCInterval=300000`: G1 itself runs a periodic collection every 5
  minutes when the app is idle, uncommitting on its own; EmberMC's trim is the
  event-driven complement that fires as soon as the empty threshold is reached
  rather than on the periodic clock.

The trade is the opposite one: a returning player may hit a short warm-up as the
heap re-commits and re-touches. For a server that spends real time empty, that is
usually the right trade. For a full server that is never idle, keep the pinned
flags; the trim will simply never have anything to return, and costs nothing.

The bundled Pterodactyl egg uses `-XX:MaxRAMPercentage` with a low `-Xms`, which
is uncommit-friendly out of the box.

## 5. Cost

Per tick: `Bukkit.getOnlinePlayers().isEmpty()` and an integer compare. The
`System.gc()` fires only on the single tick a trim is due, only when empty. No
allocation, no background thread, no world or entity access.

## 6. Configuration

    memory:
      idle-trim:
        enabled: true       # reload-safe
        after-minutes: 5    # reload-safe

## 7. How it is measured

`IdleMemoryTest` pins the pure threshold logic (not due before the minute
threshold, due at and past it, threshold scales with `after-minutes`). The
return-to-OS effect is confirmed on the box by starting with uncommit-friendly
flags, emptying the server, and reading committed heap before and after in
`/ember status` and the trim log line. With pinned flags the same procedure
confirms it correctly reports 0 MB returned.
