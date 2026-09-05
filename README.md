# EmberMC

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
| Command | `/ember` |
| Permissions | `ember.command.*`, `ember.admin.*`, `ember.profiler.*`, `ember.security.*` |
| Brand | `embermc:ember` — reports as Paper-compatible to plugins |

## Status

Milestones 1–3 — fork infrastructure and branding, the configuration framework,
and the profiler (always-on phase timing, opt-in plugin attribution, spike
watchdog). No performance changes yet, by design: the instrument comes first. See
[docs/EMBER_ROADMAP.md](docs/EMBER_ROADMAP.md) for what comes next and in what
order, and why performance claims are never made without a benchmark.

## Building

```bash
git clone <this repo> EmberMC
cd EmberMC
./gradlew applyAllPatches
./gradlew createPaperclipJar
```

The runnable server is `ember-server/build/libs/ember-paperclip-*.jar`.
Windows notes and the full workflow are in [docs/BUILDING.md](docs/BUILDING.md).

## Documentation

- [BUILDING.md](docs/BUILDING.md) — prerequisites, build, run a test server
- [UPDATING-UPSTREAM.md](docs/UPDATING-UPSTREAM.md) — pulling new Paper commits
- [EMBER_ROADMAP.md](docs/EMBER_ROADMAP.md) — milestones and engineering rules
- [DISTRIBUTION.md](docs/DISTRIBUTION.md) — where server software is (and is not) distributed
- [LICENSE.md](LICENSE.md) — GPLv3 (TheMeanOneDevelopments); upstream licences preserved

## Licence

EmberMC is published by **TheMeanOneDevelopments** under the **GNU GPL v3**: use,
run, study and modify it freely, including commercially, but any distributed
fork must stay open under GPLv3 too. Paper, Spigot, CraftBukkit and Bukkit keep
their own (compatible) licences, and the released jar never redistributes Mojang
code — it is a Paperclip launcher that patches the vanilla server locally.
Details in [LICENSE.md](LICENSE.md).
