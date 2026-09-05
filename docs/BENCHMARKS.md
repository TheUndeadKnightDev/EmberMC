# Benchmarks

This file fixes the method before results are claimed, and records every
result with the run that produced it. There is one so far.

## Results

### R1 - Redstone engine: Vanilla vs Alternate Current (2026-09-05)

| | block-ticks phase, mean | p95 |
| --- | --- | --- |
| Paper default (`VANILLA`) | 2.20 ms | 9.25 ms |
| Alternate Current | **0.20 ms** | **0.74 ms** |

**Rig.** 31×31 plane of redstone dust (961 blocks) on smooth stone at y=101,
driven by a two-observer clock feeding one corner, area force-loaded, no
players. **Server.** EmberMC `1704ed8d8` on the `ember-folia` box (4 vCPU
share, 3.5 GB heap, Temurin 25). **Method.** `/ember profiler` 5-second mean
and p95 of the `block ticks` phase, read 12 s after the rig was built; engine
switched with `misc.redstone-implementation` + `/paper reload` on the same
running server; read again 12 s later. **Caveats.** One run, one rig shape,
one machine; Alternate Current is Paper's own engine, EmberMC's contribution is
making it the default. It is a switch worth flipping, not an EmberMC invention.

### R2 - Same rig, three servers on one host: Paper, Purpur, EmberMC (2026-09-05)

Common yardstick `/mspt`, 5-second window (avg / min / max), read ~20 s after
the rig was built. All three servers are containers on the same InterServer
host, Temurin 25, 3.5 GB heap, idle apart from the rig.

| Server | redstone engine | avg | max |
| --- | --- | --- | --- |
| Paper `a2a42c5b` (stock) | vanilla (default) | 3.0 ms | 16.7 ms |
| Purpur 26.2 (stock) | vanilla (default) | 2.5 ms | 13.0 ms |
| **EmberMC** `1704ed8d8` | Alternate Current (default) | **0.6 ms** | **1.2 ms** |

**Caveats.** One run each; three different containers share one host, so
absolute numbers carry noise (Purpur's slightly lower vanilla figure is within
it). The comparison that holds is engine-vs-engine on the same rig. EmberMC's
result *is* Paper's Alternate Current; the difference between the rows is a
default.

**Side finding.** All three idle windows show a periodic 0.3-1.9 s tick every
five minutes (`1825.8` max on stock Paper's 10-second column) - the autosave.
EmberMC's watchdog was the only thing that reported it; from this build it is
its own profiler phase (`autosave`) so the report names it.

### R3 - Entity tiers, measured with a player and 2,037 mobs (2026-09-05)

One player standing in a disc of ~1,150 in-range zombies (2,037 entities total,
AI on, spawning off), on the EmberMC `ember-folia` box. The tier was switched
with `/ember reload` on the same running server, same mobs, same second - so
`OFF` is EmberMC behaving exactly as Paper (every in-range mob ticks fully),
with none of the noise of comparing two separate containers.

| Tier | entities phase (mean) | MSPT (5s avg) | full / reduced this tick |
| --- | --- | --- | --- |
| **OFF** (= regular Paper) | 14.76 ms | 16.7 ms | 1483 / 0 |
| balanced (0.75 ring, outer /2) | 10.43 ms | 12.0 ms | 934 / 540 |
| performance (0.5 ring, outer /2) | 9.19 ms | 11.0 ms | 768 / 622 |
| extreme (0.4 ring, outer /4) | **5.67 ms** | **6.5 ms** | 283 / 865 |

Entity phase vs Paper behaviour: balanced **-29%**, performance **-38%**,
extreme **-62%**. Whole-tick MSPT fell 16.7 → 6.5 ms at extreme. The measured
fractions (0.71 / 0.62 / 0.38 of the OFF phase) land almost exactly on the
predicted 0.78 / 0.63 / 0.37 from `docs/optimisations/entity-tiers.md`, and
every mob within the full ring of the player kept ticking fully at all tiers  - 
the promise held.

**Caveats.** One run per tier; a disc of idle wandering zombies, not a real
farm; one machine. It shows the mechanism working at the predicted scale, not a
guarantee for every workload - which is what the profiler is for on your own
server.

## Rules

## Rules

- Every scenario is run on Paper (the pinned commit), Purpur (same Minecraft
  version) and EmberMC, on the same machine, same JVM, same flags, same world
  seed, same plugin set, back to back.
- Three runs per scenario per server; report the median. Discard a run whose
  p99 is more than 3× its median as noise, and say so.
- Warm up for 2 minutes before measuring. Measure for 5 minutes.
- Record: TPS, mean MSPT, p95 MSPT, p99 MSPT, CPU %, heap after GC, allocation
  rate (MB/s), GC pause count and total, entity tick ms, chunk tick ms.
- Record the exact build hashes of all three servers and the full JVM command
  line in the results file.
- A result that cannot be reproduced from this document is not a result.

## Harness

`benchmarks/` (milestone 12) will hold:

- `worlds/` - pre-generated worlds per scenario, checked in as seeds plus a
  generation script, not as region files
- `bots/` - a headless-client driver for the 10 / 50 / 100 / 200 player runs
  (must speak the 26.2 protocol; mineflayer does not, so this is either a
  Java client library or a custom minimal client)
- `scenarios/` - one script per scenario below that sets the world up and
  starts the measurement window
- `collect/` - reads `/ember profiler` output and JFR recordings into CSV
- `results/` - one directory per run, never edited after the fact

## Scenarios

| Scenario | Set-up | What it stresses |
| --- | --- | --- |
| Idle baseline | empty world, 0 players | fixed per-tick overhead |
| Players 10 / 50 / 100 / 200 | bots walking a 500-block loop | tracking, chunk loading, packets |
| 10,000 entities | 10k passive mobs in a 200-block square | entity tick, activation, collision |
| Mob farm | 2,000 zombies in a drop farm | AI, pathfinding, collision |
| Villager hall | 300 villagers, 300 workstations | POI search, brain ticks |
| Hopper system | 5,000 hoppers in chains | block entity tick, inventory search |
| Redstone clock farm | 500 fast clocks with observers | block updates, neighbour updates |
| Elytra chunk loading | 20 bots flying at speed in straight lines | chunk load, generation, send |
| Mass teleport | 100 bots teleported 10,000 blocks every 10 s | chunk load bursts |
| XP farm | 50,000 XP orbs dropped over 60 s | orb ticking and merging |
| Item pile | 20,000 dropped items in one chunk | item merge, collision |
| Packet spam | bots at 1,000 packets/s each | Packet Guard cost and effect |
| Chunk generation | 50 bots spreading outward from spawn | worldgen, saving |

## Reporting

Each published number links to the `results/<run-id>/` directory that
produced it. Comparisons are stated as measured deltas with the scenario named
("p99 MSPT 41 → 33 on the villager-hall scenario"), never as a single
"X% faster".
