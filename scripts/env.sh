#!/usr/bin/env bash
# Source this before building on Windows (Git Bash) or anywhere Git lacks an
# identity. Everything here is scoped to this shell; nothing touches your
# global Git configuration. See docs/BUILDING.md for why each line exists.

# Git for Windows defaults core.autocrlf=true system-wide, which converts the
# decompiled sources to CRLF and makes every `git am` fail.
export GIT_CONFIG_COUNT=1
export GIT_CONFIG_KEY_0=core.autocrlf
export GIT_CONFIG_VALUE_0=false

# Feature patches are applied with `git am`, which needs an identity in the
# repositories paperweight generates.
export GIT_COMMITTER_NAME="${GIT_COMMITTER_NAME:-FlintMC Build}"
export GIT_COMMITTER_EMAIL="${GIT_COMMITTER_EMAIL:-build@flintmc.local}"
export GIT_AUTHOR_NAME="${GIT_AUTHOR_NAME:-$GIT_COMMITTER_NAME}"
export GIT_AUTHOR_EMAIL="${GIT_AUTHOR_EMAIL:-$GIT_COMMITTER_EMAIL}"

# JDK 25 if it is where Microsoft's installer puts it and nothing else is set.
if [ -z "${JAVA_HOME:-}" ] && [ -d "/c/Program Files/Microsoft/jdk-25.0.4.101-hotspot" ]; then
  export JAVA_HOME="C:\\Program Files\\Microsoft\\jdk-25.0.4.101-hotspot"
fi

echo "FlintMC build environment set (autocrlf=false, identity=$GIT_COMMITTER_NAME, JAVA_HOME=${JAVA_HOME:-unset})"
