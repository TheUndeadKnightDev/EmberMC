# Updating upstream Paper

EmberMC tracks one Paper commit, named in `gradle.properties`:

```properties
paperCommit = a2a42c5b12249aaba42a347327fd930a1f94af06
```

Everything EmberMC changes is a patch on top of that commit. Updating means
moving the pointer and re-applying the patches, resolving whatever no longer
lines up.

## Routine update (same Minecraft version)

1. Pick the target commit from
   [PaperMC/Paper](https://github.com/PaperMC/Paper/commits/main) and read the
   commits between the current pointer and it. Note anything that touches a
   file EmberMC patches.
2. Change `paperCommit` in `gradle.properties`.
3. Source the environment and re-apply:

   ```bash
   source scripts/env.sh
   ./gradlew applyAllPatches
   ```

4. If it succeeds, build and run the smoke test, then commit
   `gradle.properties` with the message `Update upstream to <short-hash>`.
5. If it fails, paperweight stops at the first patch that no longer applies
   and leaves the generated repository mid-`git am`. Resolve it in place:

   ```bash
   cd paper-server            # or paper-api, or ember-server/src/minecraft/java
   git status                 # see the conflicted files
   # edit, keeping every "// Ember" line intact
   git add -A && git am --continue
   cd ../
   ./gradlew rebuildServerPatches   # or rebuildApiPatches / rebuildMinecraftPatches
   ```

   Repeat `applyAllPatches` until it is clean, then commit the updated patch
   files together with `gradle.properties`.

## Minecraft version update

Same loop, but expect every Mojang-class patch to need attention, and expect
Paper's own patch set to be in flux for the first days after a release. Wait
for Paper to have feature patches applied on the new version before moving.
Bump `mcVersion` and `apiVersion` in `gradle.properties` alongside `paperCommit`.

## Keeping conflicts rare

- Prefer EmberMC's own classes under `ember-*/src` over patching Paper. A
  one-line hook in an upstream file plus a whole class of our own conflicts far
  less than a large diff in the upstream file.
- Keep every changed upstream line marked `// Ember - <reason>` and every block
  wrapped in `// Ember start - <reason>` / `// Ember end - <reason>`. Conflicts
  then show *why* a line exists, not just that it does.
- Never reformat upstream code in a patch.
- Never patch a file to change only a comment or import order.
- Keep feature patches for genuinely multi-file features; use file patches for
  everything else, as Paper does.
