# Licence

EmberMC is published by **TheMeanOneDevelopments**. It is a fork of
[Paper](https://github.com/PaperMC/Paper) and is made of three kinds of code,
each keeping the licence it came with.

## EmberMC's own work - GPLv3

Everything authored for EmberMC - the patches under `ember-*/paper-patches`,
`ember-server/minecraft-patches`, `ember-checkstyle/config-patches`, the
`build.gradle.kts.patch` files, the sources under `ember-*/src`, and the
documentation - is licensed under the **GNU General Public License, version 3**,
unless a patch header states otherwise. The full text is in [`LICENSE`](LICENSE).

```
EmberMC - a high-performance Paper fork
Copyright (C) 2026 TheMeanOneDevelopments

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see <https://www.gnu.org/licenses/>.
```

**What GPLv3 means for EmberMC in plain terms.** Anyone may use, run, study,
modify and redistribute EmberMC, including commercially. The condition is
copyleft: **if you distribute EmberMC or a modified version of it, you must
release your changes under GPLv3 too, with source.** Nobody can take EmberMC,
close it, and sell it as a proprietary product - a fork stays as open as the
original. TheMeanOneDevelopments retains every right as the copyright holder,
including the ability to offer EmberMC under other terms.

The name **EmberMC**, its branding and logo, and the emberplugins.online sites
are not covered by this licence and remain TheMeanOneDevelopments' marks.

## Paper, Spigot, CraftBukkit and Bukkit

The upstream projects EmberMC is built on keep their own licences: Paper's
patches are MIT, the Bukkit API is GPLv3, and CraftBukkit and Spigot are
LGPLv3 - all compatible with EmberMC's GPLv3. Those files are not committed to
this repository - paperweight regenerates them from the Paper commit named in
`gradle.properties` - and their notices are preserved as-is in the generated
trees and the built jar. See `paper-server/LICENCE.txt`, `paper-server/LGPL.txt`
and `paper-api/LICENCE.txt` after `./gradlew applyAllPatches`.

## Minecraft

EmberMC does not redistribute Minecraft server code. Like Paper, the released
jar is a [Paperclip](https://github.com/PaperMC/Paperclip) launcher: on first
run it downloads the vanilla server from Mojang and applies EmberMC's patches to
it locally. Use of Minecraft is governed by the
[Minecraft EULA](https://www.minecraft.net/eula), which forbids selling modified
server software - another reason EmberMC is free.

## Studied projects

Other Paper forks were studied for their *repository structure* while setting up
EmberMC's build. No code was copied from them. EmberMC's optimisations are its
own; where an idea is shared with another project, the patch header says so.
