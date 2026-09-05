# FlintMC

A production-grade [Paper](https://github.com/PaperMC/Paper) fork for large
survival and economy networks.

Paper compatibility first. Then measurable performance, aggressive exploit
protection, intelligent entity and chunk handling, and the kind of
observability that tells an administrator *why* the server is doing what it is
doing.

| | |
| --- | --- |
| Minecraft | 26.2 |
| Upstream | Paper `a2a42c5b` |
| Java | 25 |
| Command | `/flint` |
| Permissions | `flint.command.*`, `flint.admin.*`, `flint.profiler.*`, `flint.security.*` |
| Brand | `flintmc:flint` — reports as Paper-compatible to plugins |

## Status

Milestone 1 — fork infrastructure, branding and `/flint status`. See
[docs/FLINT_ROADMAP.md](docs/FLINT_ROADMAP.md) for what comes next and in what
order, and why performance claims are never made without a benchmark.

## Building

```bash
git clone <this repo> FlintMC
cd FlintMC
./gradlew applyAllPatches
./gradlew createPaperclipJar
```

The runnable server is `flint-server/build/libs/flint-paperclip-*.jar`.
Windows notes and the full workflow are in [docs/BUILDING.md](docs/BUILDING.md).

## Documentation

- [BUILDING.md](docs/BUILDING.md) — prerequisites, build, run a test server
- [UPDATING-UPSTREAM.md](docs/UPDATING-UPSTREAM.md) — pulling new Paper commits
- [FLINT_ROADMAP.md](docs/FLINT_ROADMAP.md) — milestones and engineering rules
- [DISTRIBUTION.md](docs/DISTRIBUTION.md) — where server software is (and is not) distributed
- [LICENSE.md](LICENSE.md) — MIT for FlintMC's work; upstream licences preserved

## Licence

FlintMC's patches and sources are MIT. Paper, Spigot, CraftBukkit and Bukkit
keep their own licences, and the released jar never redistributes Mojang code —
it is a Paperclip launcher that patches the vanilla server locally. Details in
[LICENSE.md](LICENSE.md).
