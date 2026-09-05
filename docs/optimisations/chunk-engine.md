# Chunk engine

Milestone 7. The honest version: Paper's chunk system (the Moonrise rewrite,
upstream now) is very good, and the right thing to do is use it and tune it, not
reimplement it. What it does not give you is visibility into retention, so that
is what EmberMC adds here.

## 1. Existing Paper behaviour

Already handled upstream, and not reinvented:

- **Async saves.** Chunk data is written off the main thread during unload
  processing; the main-thread autosave no longer serialises chunk NBT.
- **Per-player concurrency limits.** `chunk-system.player-max-concurrent-chunk-
  loads` and `player-max-concurrent-chunk-generates` bound how much one player
  can have in flight, auto-tuned per player by default. This is what keeps rapid
  elytra travel and mass teleport from swamping the loader.
- **Autosave spread.** `chunks.max-auto-save-chunks-per-tick` spreads the save
  budget across ticks.
- **Delayed unload.** `chunks.delay-chunk-unloads-by` (default 10s) stops a
  player pacing a border from thrashing load/unload.
- **Save-limit trims.** `chunks.entity-per-chunk-save-limit` caps projectile and
  orb piles written to disk.

The Tuner (`/ember tune`) already sets the ones worth changing
(`max-auto-save-chunks-per-tick`, `prevent-moving-into-unloaded-chunks`, the
per-chunk save limits) as part of its presets.

## 2. Gap

None of that answers the question an admin actually asks when a world idles heavy
or never shrinks: **what is keeping these chunks loaded?** The usual answer is a
plugin holding chunk tickets - a claim protector, a spawner or shop manager, a
minimap backend - or a forgotten `/forceload`. Bukkit exposes the data
(`World.getPluginChunkTickets()`, `World.getForceLoadedChunks()`) but nothing
puts it in front of you, so a chunk leak is usually found only after it has
already cost memory and tick time for weeks.

## 3. Change

A read-only retention diagnostic. No change to how chunks load, generate or save.

- **`/ember chunks`** now shows, per world: loaded (all in memory), ticking
  (fully ticking), forced (`/forceload`), and plugin (chunks held by plugin
  tickets), alongside the chunk tick-phase time. Below the table it names the
  plugins holding chunks in each world, most first.
- **A leak warning.** Every 20 ticks EmberMC reads the per-world plugin-ticket
  totals; when one world passes `chunks.warn-threshold` (default 400) it logs a
  single line, rate-limited to `chunks.warn-cooldown-seconds` (default 300),
  naming the top offenders. It is a heads-up, nothing more.
- **Metrics.** `ember_chunks_plugin_held` and `ember_chunks_force_loaded` are
  exported for dashboards and alerts.

EmberMC never force-unloads a chunk a plugin asked to keep. A ticket is a
contract; breaking it silently would corrupt the very plugins this is meant to
help you diagnose. The value is knowing, not meddling.

## 4. Compatibility

Read-only. No chunk behaviour changes, no API changes, nothing is unloaded.
`chunks.retention-diagnostics: false` removes the warning and the extra
`/ember chunks` detail; the counts are read from public Bukkit API either way.
The ticking-chunk count is read through the server handle inside a try/catch, so
a future mapping change can only blank that one column, never break the command.

## 5. Cost

The `/ember chunks` read-out and the metric suppliers walk the per-world ticket
map on demand only (a command, or a metrics scrape). The background warning check
runs once per 20 ticks and only sums map sizes; it allocates nothing and
formats a string only on the tick it actually warns.

## 6. How it is measured

`ChunkInsightsTest` pins the pure warning decision: warns only above the
threshold, respects the cooldown, and a zero threshold disables it. The read-out
is confirmed on the box by force-loading a block of chunks and by loading a
plugin that holds tickets, then checking the counts and the named holders in
`/ember chunks`.

## 7. Measurements

_Unit tests pass. Live retention read-out confirmed on the box; recorded in
BENCHMARKS.md when run._
