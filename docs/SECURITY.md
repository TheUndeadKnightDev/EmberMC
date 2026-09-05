# Security

**Status: the Packet Guard core shipped in Milestone 8** (per-connection, per-category token-bucket limiter with actions log/warn/throttle/drop/kick, `/ember security`, `docs/optimisations/packet-guard.md`). The design rules below still govern what comes next. The current build has exactly
Paper's exploit mitigations — which are substantial — and nothing more. The
Ember Security Engine and Ember Packet Guard are Milestone 8. This document
fixes the design rules before that work begins.

## Reporting a vulnerability

Do not open a public issue for an exploit that works against live servers.
Email the maintainer (address in the repository profile) with the EmberMC
build, a description, and a way to reproduce. You will get an acknowledgement
within 48 hours and a fix or a workaround before any public disclosure.

## Design rules for Milestone 8

**One place.** All rate tracking, payload limits and malformed-input checks go
through the Ember Packet Guard, a single component on the Netty pipeline with
one configuration section and one metrics view (`/ember security`,
`/ember network`). No scattered checks.

**Actions, not just kicks.** Every rule carries an action — `log`, `warn`,
`throttle`, `drop`, `kick` — and a default chosen for a busy survival network:
throttle where a burst is plausibly legitimate (interaction, movement
correction), drop where it never is (oversized book pages, malformed
components), kick only for behaviour that has no honest explanation.

**Burst-aware limits.** Limits are token buckets with a burst allowance, not
fixed per-second counts. A player who opens a chest and shift-clicks a row of
items is not an attacker.

**Cheap.** Guard code runs on every inbound packet. It allocates nothing on the
hot path, uses primitive counters per player per category, and its own cost is
one of the metrics it reports. A security system that costs 5 ms a tick has
failed at its job.

**Diagnosable.** When the Guard acts, `/ember security` says which player,
which category, which rule, how far over, and what action was taken. An
administrator can always answer "why was this throttled?".

**No sensitive data.** The Guard never logs session tokens, authentication
payloads, chat content, or IP addresses beyond what Paper already logs. Player
identifiers in Guard output are names and UUIDs, nothing else.

**Tested hostile.** Every rule ships with a test that sends the abuse it
targets and asserts the action, and a test that sends heavy but legitimate play
and asserts *no* action. Parsers that touch player-controlled bytes get fuzzed.

## Categories the Guard will cover

Packet spam and malformed packets; oversized payloads; invalid movement and
vehicle movement; entity and block interaction spam; inventory packet abuse;
book, sign, NBT and component payload abuse; command and tab-completion spam;
recipe spam; creative inventory abuse; plugin-message traffic; decompression
and resource exhaustion; chunk-load and teleport abuse (with Milestone 7).
