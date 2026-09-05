# Packet Guard

Milestone 8. Exploit mitigation, not a throughput optimisation, but it follows
the same rules: measured cost, bounded behaviour, honest diagnostics.

## 1. Existing Paper behaviour

Paper has two things. A **connection throttle** (`ServerHandshakePacketListenerImpl`)
that rejects repeated logins from one address within a window. A **packet
limiter** (`Connection.channelRead0`) with an all-packets rate and optional
per-exact-type overrides, action KICK or DROP. Both are blunt: the all-packets
limit is one number for every packet type, and per-type overrides are keyed on
exact classes.

## 2. Gap

The rate a movement packet arrives at is nothing like the rate a book edit or a
command should, and one all-packets number cannot express that. Set it low
enough to stop command spam and you kick a player sprinting; set it high enough
for movement and command spam sails through. And there is no view of what is
being limited: an administrator sees a kick, not why.

## 3. Change

A per-connection, per-category token-bucket limiter sitting just after Paper's
limiter in `channelRead0`.

- **Categories** (`PacketCategory`): movement, arm-swing, interact, inventory,
  book/sign, chat, command, tab-complete, recipe, creative, plugin-message, other. Classified
  by the packet class's simple name, so no Minecraft type is needed to test it.
- **Token bucket** (`TokenBucket`) per category: a sustained `per-second` rate
  and a `burst` allowance, so a legitimate flurry (a row of shift-clicks, a
  sprint) is fine and a sustained flood is not.
- **Payload size**: categories that carry big text (book, sign) have a
  `max-bytes`; over it is a violation regardless of rate. Measured without
  reading contents beyond length.
- **Action** per category: `log` (count only), `warn`, `throttle`/`drop` (drop
  the packet, keep the player - the client just retries), `kick`.
- **Diagnostics**: `/ember security` shows every category's limit, action,
  allowed and blocked counts; `ember_packets_blocked` gauge. Console warns are
  rate-limited to one per five seconds.

Defaults: movement 200/s (throttle), arm-swing 60/s (drop), book/sign 4/s +
12 KB (kick), command 15/s (throttle), and so on - a busy survival server's
headroom, not a tight cap.

Decompression-exhaustion (a packet that inflates to a huge size) is not a
category here because Paper already caps it: the compression decoder rejects any
packet claiming to decompress past 8 MB before it inflates. Not reinvented.

## 4. Compatibility

A well-behaved client never hits these limits, so nothing changes for players.
A throttled packet is dropped and re-sent by the client, invisible in normal
play. `security.packet-guard.enabled: false` removes the layer; every limit and
action is per-category configurable. No API change. Register row added.

## 5. Thread safety

The guard runs on the Netty read thread for the connection, where Paper's own
limiter already runs, and each `Session` belongs to one connection, so the
buckets are touched by one thread. The server-wide diagnostic counters are
`AtomicLongArray`. Nothing touches world or entity state.

## 6. Cost

Per packet: one enum lookup by simple name (a handful of `contains` on a short
string), one array index, one bucket refill (two longs and a double), and only
on a violation an atomic increment. No allocation after the first packet of each
category on a connection. Categorisation by simple name is the one thing worth
watching; if it ever shows up in a profile it becomes an identity-map cache
keyed on the packet class.

## 7. How it is measured

Unit tests pin the two pure pieces: `TokenBucketTest` (burst then stop, refill
at rate, never above burst, sustained-at-limit passes, reconfigure caps down)
and `PacketCategoryTest` (Mojang names classify correctly, signed-command is a
command not chat, unknown is other). The end-to-end path is checked on the box
by flooding a category from a socket and reading `/ember security`.

## 8. Measurements

_Unit tests pass. Live flood check recorded in BENCHMARKS.md when run on the box._
