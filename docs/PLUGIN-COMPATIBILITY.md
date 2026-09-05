# Plugin compatibility

FlintMC's contract with plugins is simple: **anything that runs on the pinned
Paper commit runs on FlintMC**, unless a change is listed in the behaviour
register below with a compatibility toggle.

## How FlintMC stays compatible

- The Bukkit, Spigot and Paper APIs are Paper's, untouched. FlintMC adds; it
  does not remove or rename.
- `ServerBuildInfo.isBrandCompatible(BRAND_PAPER_ID)` returns `true`, so
  plugins that gate on "is this Paper?" load normally. The server's own brand is
  `flintmc:flint` for plugins that want to know.
- `Bukkit.getName()` returns `FlintMC`. Plugins that compare the server name
  against the string `"Paper"` will see a difference; that has always been
  unsupported and every Paper fork has it.
- Every optimisation that changes something a plugin could observe ships with a
  toggle and an entry in the register below. No silent behaviour changes.

## Behaviour register

| Since | Change | Observable how | Toggle |
| --- | --- | --- | --- |
| M1 | Server brand and name are FlintMC | `Bukkit.getName()`, `/version`, manifest | none — this is the point |
| M1 | `/version` does not check PaperMC for updates | `/version` output | none |

Nothing else yet. Milestones 4–10 will add rows here as they land.

## Baseline matrix

Run on every release. A row passes when the plugin loads, its main commands
work, and the server shuts down cleanly with no plugin-attributed errors.

| Category | Plugin | Result | Notes |
| --- | --- | --- | --- |
| Permissions | LuckPerms | pending | |
| Economy | Vault + an economy provider | pending | |
| Claims | GriefPrevention or equivalent | pending | |
| World management | Multiverse-Core | pending | |
| World editing | FastAsyncWorldEdit | pending | |
| Protocol | packetevents / ProtocolLib | pending | |
| Placeholders | PlaceholderAPI | pending | |
| NPCs | Citizens | pending | |
| Anti-cheat | Grim | pending | |
| Cross-platform | Geyser + Floodgate | pending | |
| Custom items | ItemsAdder | pending | |
| Menus | a DeluxeMenus-class plugin | pending | |

The first pass of this matrix uses the Ember plugin suite already installed on
the test servers plus the third-party plugins present there (LuckPerms,
PlaceholderAPI, Vault, Floodgate, packetevents, ItemsAdder), because that is a
real production-shaped plugin set rather than a synthetic one.

## Reporting an incompatibility

A plugin that behaves differently on FlintMC than on the pinned Paper commit is
a FlintMC bug until proven otherwise. Reports need: FlintMC build, plugin and
version, the behaviour on Paper, the behaviour on FlintMC, and a way to
reproduce it.
