# Distributing FlintMC

Server software is not a plugin, and the plugin marketplaces are the wrong
place for it. This is how every established Paper fork reaches its users, and
why.

## Where it cannot go

- **SpigotMC Resources** — plugins only. Server jars are not accepted.
- **BuiltByBit** — its server-jar rules exist to stop redistribution of modified
  Mojang code; a Paper fork is not a product you list there.
- **Modrinth / CurseForge / Hangar** — plugin and mod hosts. Hangar (PaperMC's
  own) does not host server software either.

## Where it goes

1. **GitHub Releases** — the canonical download. Tag each build; attach only the
   Paperclip jar (`flint-paperclip-<mc>-<build>.jar`). Release notes list the
   upstream Paper commit and every FlintMC change.
2. **Own site** — a downloads page with the latest build per Minecraft version
   and a JSON endpoint (`/api/v2/flint/<version>/latest`) so hosts and update
   checkers can find builds without scraping. The in-server version checker
   points here.
3. **Pterodactyl egg** — `eggs/egg-flintmc.json`. Hosting companies import
   eggs; players on those panels pick FlintMC from a dropdown. This is the
   single biggest source of adoption for Purpur and Pufferfish.
4. **Hosting providers' jar lists** — email the support desks of the big hosts
   with the egg and the JSON endpoint. They add forks that have a stable API.
5. **Discord** — support, announcements, and where the admins who try forks live.
6. **r/admincraft and the PaperMC Discord's `#forks` channel** — announce with
   benchmarks attached, never without.

## What keeps this legal

FlintMC never distributes Mojang code. The release is a
[Paperclip](https://github.com/PaperMC/Paperclip) launcher: on first run it
downloads the vanilla server from Mojang and applies FlintMC's patches locally.
That is the same mechanism Paper itself relies on.

The Minecraft EULA forbids selling modified server software. FlintMC is free and
MIT-licensed. Revenue around a fork comes from what runs *on* it — premium
plugins, hosting, support — not from the server itself.

## Release checklist

- [ ] `./gradlew applyAllPatches` clean from a fresh clone
- [ ] `./gradlew createPaperclipJar` succeeds; jar boots; `/flint version` right
- [ ] Plugin-compatibility matrix green (PLUGIN-COMPATIBILITY.md)
- [ ] Benchmarks for the build recorded (BENCHMARKS.md); no claim without a number
- [ ] Release notes: upstream commit, changes, known behaviour differences
- [ ] Tag, GitHub release, JSON endpoint updated, egg version bumped
