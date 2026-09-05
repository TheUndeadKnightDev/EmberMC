# Building EmberMC

## Prerequisites

- **Git** 2.31 or newer
- **JDK 25** (Gradle's toolchain support will provision one if you only have a
  newer JRE, but having it installed is faster)
- ~6 GB of free disk for the decompiled Minecraft sources and Gradle caches
- 8 GB of RAM available to the build

You do not need Paper checked out. paperweight fetches the exact Paper commit
named in `gradle.properties` and generates the upstream trees for you.

## Building

```bash
git clone <this repo> EmberMC
cd EmberMC
./gradlew applyAllPatches      # generate paper-*/ trees and apply Ember's patches
./gradlew createPaperclipJar   # build the runnable server
```

Output: `ember-server/build/libs/ember-paperclip-<mc>-<build>.jar`.

The first `applyAllPatches` decompiles Minecraft and takes several minutes.
Later runs are incremental.

## Running a test server

```bash
./gradlew runServer            # Mojang-mapped jar, working dir ./run
```

or copy the Paperclip jar into an empty directory and run it with
`java -jar ember-paperclip-*.jar --nogui`.

## Windows

Two things bite on Windows, both scoped to the build rather than your global
Git configuration:

1. **Line endings.** Git for Windows ships `core.autocrlf=true` in its system
   config, which converts the decompiled sources to CRLF and makes every
   `git am` fail. The repo sets `core.autocrlf=false` locally, but paperweight
   creates *new* Git repositories for the generated trees. Give Git the setting
   through the environment for the build shell:

   ```bash
   export GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=core.autocrlf GIT_CONFIG_VALUE_0=false
   ```

2. **Committer identity.** Feature patches are applied with `git am`, which
   needs an identity in the generated repositories. Again, through the
   environment rather than `--global`:

   ```bash
   export GIT_COMMITTER_NAME="EmberMC Build" GIT_COMMITTER_EMAIL="build@embermc.local"
   export GIT_AUTHOR_NAME="EmberMC Build"    GIT_AUTHOR_EMAIL="build@embermc.local"
   ```

3. **A git ceiling.** paperweight fetches Paper into
   `.gradle/caches/paperweight/upstreams/paper` by running git *inside that
   directory*. If it is not a repository yet and `git init` there does not take,
   git walks up to the nearest repository - this one - and runs
   `checkout -f FETCH_HEAD` on **your working tree**, replacing it with Paper's.
   `GIT_CEILING_DIRECTORIES=<repo root>` makes git refuse to look above the
   root from any subdirectory, so the worst case becomes an error instead of a
   rewrite. Never delete the root `.gradle` directory by hand; if you must
   start over, delete `.gradle/caches/paperweight/upstreams` only after
   re-cloning it:

   ```bash
   git clone --no-checkout https://github.com/PaperMC/Paper.git .gradle/caches/paperweight/upstreams/paper
   ```

`scripts/env.sh` sets all three. Source it before building. If the working
tree ever does get replaced, `main` is untouched: `git checkout -f main`,
`git clean -fd`, and restore `gradle/wrapper/gradle-wrapper.jar` from any
Paper checkout.

If a run of `applyAllPatches` fails part-way, paperweight may have already
written an *unpatched* `ember-*/build.gradle.kts`. Gradle configures those
before the patch task can fix them, so the next run fails on
`Project ':paper-checkstyle' could not be found`. Delete the three generated
build scripts and run again:

```bash
rm -f ember-api/build.gradle.kts ember-server/build.gradle.kts ember-checkstyle/build.gradle.kts
```

## Where things live

| Path | What | Committed? |
| --- | --- | --- |
| `ember-api/src`, `ember-server/src` | EmberMC's own classes | yes |
| `ember-api/paper-patches` | patches to Paper API classes | yes |
| `ember-server/paper-patches` | patches to Paper server classes | yes |
| `ember-server/minecraft-patches` | patches to Mojang classes | yes |
| `ember-*/build.gradle.kts.patch` | patches to Paper's build scripts | yes |
| `paper-api/`, `paper-server/`, `paper-checkstyle/`, `.checkstyle/` | generated upstream trees (each a Git repo) | **no** |
| `ember-server/src/minecraft` | decompiled Minecraft with patches applied (a Git repo) | **no** |
| `ember-*/build.gradle.kts` | generated from the `.patch` next to it | **no** |

## Making a change

**To EmberMC's own code**: edit under `ember-*/src` and build. No patch system
involved.

**To a Paper class**: edit the file inside `paper-server/` (or `paper-api/`),
then

```bash
./gradlew fixupServerFilePatches      # fold your edit into the file-patch commit
./gradlew rebuildServerFilePatches    # regenerate ember-server/paper-patches/files
```

(`Api` instead of `Server` for the API tree.) Mark every changed line with a
`// Ember - <reason>` comment so the patch is self-describing.

**To a Mojang class**: same, inside `ember-server/src/minecraft/java`, with
`fixupMinecraftFilePatches` and `rebuildMinecraftFilePatches`.

**A large feature spanning many files**: commit it as its own commit in the
relevant generated repo and run `rebuild<Project>FeaturePatches`. The commit
message becomes the patch header — explain intent there.

**To a build script**: edit the generated `ember-*/build.gradle.kts` and run
`./gradlew rebuildPaperSingleFilePatches`.

## Tests

```bash
./gradlew :ember-server:test
./gradlew :ember-api:test
```
