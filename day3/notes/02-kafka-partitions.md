# 02 — Kafka Partitions

## What is a partition? (one line)

> **A topic is split into smaller pieces called partitions,
> so that many consumers can work at the same time.**

---

## The bank counter analogy

Imagine a bank with ONE counter:

```
Customers:  😐 😐 😐 😐 😐 😐  ──>  [ Counter 1 ]     SLOW!
```

Now the same bank with THREE counters:

```
Customers:  😐 😐  ──>  [ Counter 1 ]
            😐 😐  ──>  [ Counter 2 ]     3x FASTER!
            😐 😐  ──>  [ Counter 3 ]
```

- Topic = the bank
- Partition = one counter
- More partitions = more work done in parallel

---

## What a partition looks like inside

Each partition is a simple **ordered list** — new messages are always
added at the END (like writing lines in a notebook):

```
Topic: "order-events"  (3 partitions)

Partition 0 : [msg0][msg1][msg2][msg3] <- new messages added here
Partition 1 : [msg0][msg1]             <- new messages added here
Partition 2 : [msg0][msg1][msg2]       <- new messages added here
```

Messages are never changed, never inserted in the middle.
**Append-only. Like a diary — you only write on the next empty line.**

---

## Which partition does a message go to?

Two cases:

### Case 1 — message has NO key
Kafka spreads messages across partitions (round-robin style).
Good for load balancing, but no ordering guarantee between them.

### Case 2 — message HAS a key (e.g. key = orderId)

> **Same key → always the SAME partition.**

```
key "order-A" ──> always Partition 1
key "order-B" ──> always Partition 0
key "order-A" ──> Partition 1 again (guaranteed!)
```

Why do we care? **ORDER.**

---

## The golden rule of ordering

> **Kafka guarantees order only INSIDE one partition —
> not across the whole topic.**

Example: an order goes through 3 events: `CREATED → PAID → SHIPPED`.

- If they land in DIFFERENT partitions → a consumer might see
  `SHIPPED` before `PAID`. 😱 Wrong!
- If we use **key = orderId** → all 3 events go to the SAME partition
  → they are always read in the correct sequence. ✅

**Rule of thumb:** related messages that must stay in order → give them
the same key.

---

## How many partitions should a topic have?

Simple thinking:

- Partitions = maximum number of consumers that can work in parallel.
- 3 partitions → at most 3 consumers (in one group) working at once.
- A 4th consumer in the same group would just sit **idle**.

For learning/demos: **3 partitions** is a nice number.
In production teams decide based on expected traffic.

```bash
# create a topic with 3 partitions (Kafka in docker)
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create --topic order-events --partitions 3 --replication-factor 1
```

---

## Key points (remember)

1. Partition = a slice of a topic = one "counter" for parallel work.
2. Each partition is an append-only, ordered list.
3. No key → messages spread out. With key → same key = same partition.
4. **Order is guaranteed ONLY within one partition.**
5. Number of partitions = maximum parallel consumers in a group.
6. More partitions = more speed, but you cannot easily reduce them later —
   plan a sensible number.
