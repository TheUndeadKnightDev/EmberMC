# Plugin compatibility

EmberMC's contract with plugins is simple: **anything that runs on the pinned
Paper commit runs on EmberMC**, unless a change is listed in the behaviour
register below with a compatibility toggle.

## How EmberMC stays compatible

- The Bukkit, Spigot and Paper APIs are Paper's, untouched. EmberMC adds; it
  does not remove or rename.
- `ServerBuildInfo.isBrandCompatible(BRAND_PAPER_ID)` returns `true`, so
  plugins that gate on "is this Paper?" load normally. The server's own brand is
  `embermc:ember` for plugins that want to know.
- `Bukkit.getName()` returns `EmberMC`. Plugins that compare the server name
  against the string `"Paper"` will see a difference; that has always been
  unsupported and every Paper fork has it.
- Every optimisation that changes something a plugin could observe ships with a
  toggle and an entry in the register below. No silent behaviour changes.

## Behaviour register

| Since | Change | Observable how | Toggle |
| --- | --- | --- | --- |
| M1 | Server brand and name are EmberMC | `Bukkit.getName()`, `/version`, manifest | none - this is the point |
| M1 | `/version` does not check PaperMC for updates | `/version` output | none |
| M3 | Redstone engine defaults to Alternate Current on new installs | Update order in rare edge-case contraptions; measured 11x cheaper block-ticks phase | `misc.redstone-implementation: VANILLA` in `paper-world-defaults.yml` (Paper's own key). Existing installs keep whatever their file says |
| M8 | Packet Guard: per-category inbound rate/size limits per connection | A well-behaved client never hits them; a throttled packet is dropped and re-sent, invisible in play; abusive rates are throttled or kicked | `security.packet-guard.enabled: false`, or per-category action/limits in EmberMC's config |
| M4 | Entity tiers: mobs in the outer part of the activation range tick fully every Nth tick | A mob at mid distance moves at 1/N speed and reacts a little later to non-exempt stimuli; never frozen; Bukkit events fire on the ticks taken | `entities.tiers.enabled: false` (global) or `entities.optimization: VANILLA` (per world) in EmberMC's config |
| M3 | `/ember tune apply <preset>` writes Paper/Spigot/Bukkit performance keys | Only what the plan shows; each line names the visible effect; hopper move-event is refused while any plugin listens | `/ember tune revert` writes upstream defaults; every write is backed up to `ember-backups/` |

Nothing else yet. Milestones 4-10 will add rows here as they land.

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

A plugin that behaves differently on EmberMC than on the pinned Paper commit is
a EmberMC bug until proven otherwise. Reports need: EmberMC build, plugin and
version, the behaviour on Paper, the behaviour on EmberMC, and a way to
reproduce it.
