# Licence

FlintMC is a fork of [Paper](https://github.com/PaperMC/Paper). It is made of
three kinds of code, and each keeps the licence it came with.

## FlintMC's own work

Everything authored for FlintMC — the patches under `flint-*/paper-patches`,
`flint-server/minecraft-patches`, `flint-checkstyle/config-patches`, the
`build.gradle.kts.patch` files, the sources under `flint-*/src`, and the
documentation — is released under the MIT licence unless a patch header says
otherwise.

```
MIT License

Copyright (c) 2026 Caleab Harless and FlintMC contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Paper, Spigot, CraftBukkit and Bukkit

The upstream projects FlintMC is built on keep their own licences. Paper's
patches are MIT; the Bukkit API is GPLv3; CraftBukkit and Spigot are LGPLv3.
Those files are not committed to this repository — paperweight regenerates
them from the Paper commit named in `gradle.properties` — and their notices are
preserved as-is in the generated trees and the built jar. See
`paper-server/LICENCE.txt`, `paper-server/LGPL.txt` and `paper-api/LICENCE.txt`
after `./gradlew applyAllPatches`.

## Minecraft

FlintMC does not redistribute Minecraft server code. Like Paper, the released
jar is a [Paperclip](https://github.com/PaperMC/Paperclip) launcher: on first
run it downloads the vanilla server from Mojang and applies FlintMC's patches to
it locally. Use of Minecraft is governed by the
[Minecraft EULA](https://www.minecraft.net/eula).

## Studied projects

Other Paper forks were studied for their *repository structure* while setting
up FlintMC's build. No code was copied from them. FlintMC's optimisations are
its own; where an idea is shared with another project, the patch header says so.
