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

A row passes when the plugin loads and enables, its main features work, and the
log shows no plugin-attributed errors. Verified live on the `ember-folia` test
box, **EmberMC `26.2-DEV-3a6a3de`** (Minecraft 26.2), on 2026-09-05.

### Verified on the box (this run)

Twelve plugins enabled with zero errors, exceptions or version/brand complaints.
The set is production-shaped, not synthetic.

| Category | Plugin | Version | Result | What it exercises on EmberMC |
| --- | --- | --- | --- | --- |
| Protocol library | packetevents | 2.13.0 | pass | The `Connection` packet path and the Packet Guard hook; server-version and platform detection. Deep packet interception, enabled clean. |
| Cross-platform | Floodgate | 2.2.5-b140 | pass | The login and connection flow behind a proxy; Bedrock auth. Enabled clean; real players logged in end to end. |
| Profiler (internals) | spark | bundled | pass | Deep server internals and async sampling; a strong signal that NMS access is intact. |
| GUI menus (DeluxeMenus-class) | EmberMenus | 1.2.0 | pass | Inventory GUIs, click handling, dynamic command registration (8 menus, 7 commands). |
| Economy | EmberCoins | 1.1.0 | pass | Economy provider and API; balances read correctly in play. |
| Economy core / commands | EmberEssentials | 1.0.0 | pass | Large command set, events, economy hook. |
| Player market | EmberAuctions | 1.0.0 | pass | Bidding, GUIs, cross-plugin economy. |
| Shop | EmberShop | 1.1.1 | pass | 1,022 items across 16 categories; item-provider integration with EmberEnchants. |
| Custom enchants / items | EmberEnchants | 1.2.0 | pass | Item metadata, anvil/table hooks, provider API. |
| Effects engine | EmberFxCore | 1.0.0 | pass | Triggers/conditions/effects engine (libreforge-class), heavy API surface. |
| Chunk management | EmberHorizon | 1.0.0 | pass | View/simulation-distance control and chunk-ticket reads. |
| Packet spoofing | EmberSeedGuard | 2.0.0 | pass | packetevents-based hashed-seed spoofing over the Packet Guard path. |

**Risk points confirmed clear this run:**

- **Server brand / version detection** - packetevents and Floodgate both detect
  the platform and version themselves and enabled without warning, so
  `isBrandCompatible` and the version manifest read correctly.
- **Event delivery** - every plugin's listeners fired (economy, menus, seed
  spoof, shop) with the profiler's `PaperEventManager` wrapper in place.
- **Scheduler** - repeating tasks (Horizon's distance loop, spark sampling,
  menu timers) ran; the `CraftScheduler` attribution wrapper did not drop tasks.
- **Packet path + login** - real players connected through the proxy and played
  with the Packet Guard active; no packets were wrongly dropped or kicks issued.
- **Entity tiers** - during the R4 benchmark ~8,000 tiered mobs still fired
  Bukkit events and responded to commands; nothing a plugin touched froze.
- **Commands** - `/ember` coexists with every plugin's commands; the only
  collisions logged are between plugins (EmberMenus vs others over `/rtp` etc.),
  not with EmberMC.

### Still to verify (not present on this box)

These are on other Ember boxes or not yet installed here; marked pending honestly
rather than claimed.

| Category | Plugin | Result |
| --- | --- | --- |
| Permissions | LuckPerms | pending |
| Economy bridge | Vault | pending |
| Claims | GriefPrevention | pending |
| World management | Multiverse-Core | pending |
| World editing | FastAsyncWorldEdit | pending |
| Placeholders | PlaceholderAPI | pending |
| NPCs | Citizens | pending |
| Anti-cheat | Grim | pending |
| Custom items | ItemsAdder | pending |

## Reporting an incompatibility

A plugin that behaves differently on EmberMC than on the pinned Paper commit is
a EmberMC bug until proven otherwise. Reports need: EmberMC build, plugin and
version, the behaviour on Paper, the behaviour on EmberMC, and a way to
reproduce it.
