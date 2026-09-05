# Benchmarks

This file fixes the method before results are claimed, and records every
result with the run that produced it. There is one so far.

## Results

### R1 — Redstone engine: Vanilla vs Alternate Current (2026-09-05)

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

- `worlds/` — pre-generated worlds per scenario, checked in as seeds plus a
  generation script, not as region files
- `bots/` — a headless-client driver for the 10 / 50 / 100 / 200 player runs
  (must speak the 26.2 protocol; mineflayer does not, so this is either a
  Java client library or a custom minimal client)
- `scenarios/` — one script per scenario below that sets the world up and
  starts the measurement window
- `collect/` — reads `/ember profiler` output and JFR recordings into CSV
- `results/` — one directory per run, never edited after the fact

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
