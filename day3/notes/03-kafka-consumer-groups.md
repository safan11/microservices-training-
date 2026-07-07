# 03 — Kafka Consumer Groups

## What is a consumer group? (one line)

> **A consumer group is a TEAM of consumers that share the work
> of reading one topic.**

Every consumer belongs to a group — you set it with `group.id`
(in Spring Boot: `spring.kafka.consumer.group-id`).

---

## The pizza delivery analogy

A pizza shop (topic) has orders coming in on 3 counters (partitions).

**One delivery boy (1 consumer):** he handles ALL 3 counters alone. Slow.

```
Partition 0 ──┐
Partition 1 ──┼──> 🛵 consumer-1          (does everything)
Partition 2 ──┘
```

**Three delivery boys in the SAME team (same group):** work is SPLIT.

```
Partition 0 ──> 🛵 consumer-1
Partition 1 ──> 🛵 consumer-2             (each takes a share)
Partition 2 ──> 🛵 consumer-3
```

Each order is delivered by exactly ONE boy. Nobody delivers the
same pizza twice. **This is load balancing.**

---

## THE two rules (most important slide of the day)

### Rule 1 — SAME group = messages are SHARED

```
                          group: "product-group"
Topic ── each message ──> goes to only ONE member
```

Use case: run 3 instances of product-service (same group id) to handle
high traffic. Each order is processed ONCE.

### Rule 2 — DIFFERENT groups = messages are COPIED

```
Topic ── every message ──> "product-group"  (gets ALL messages)
      └─ every message ──> "email-group"    (ALSO gets ALL messages)
```

Use case: product-service reduces stock AND email-service sends mail —
both need EVERY order. Different group ids → both get everything.

> **Memory trick:**
> Same team → divide the work.
> Different teams → everyone gets a full copy.

---

## Partition assignment (who gets what?)

Kafka gives each partition of the topic to exactly ONE consumer in the group:

| Partitions | Consumers in group | Result |
|-----------|--------------------|--------|
| 3 | 1 | that consumer reads all 3 partitions |
| 3 | 2 | one reads 2 partitions, other reads 1 |
| 3 | 3 | perfect: one partition each |
| 3 | 4 | 3 work, **1 sits IDLE** (nothing left to give) |

> More consumers than partitions = wasted consumers.
> Partitions are the upper limit of parallelism.

---

## Rebalancing (the group reorganizes itself)

When a consumer **joins** or **leaves/crashes**, Kafka automatically
redistributes the partitions among the remaining members.

```
Before:  P0 -> C1,  P1 -> C2,  P2 -> C2
C1 crashes...
After:   P0 -> C2,  P1 -> C2,  P2 -> C2    (C2 took over everything)
```

Nobody restarts anything manually. This is **automatic failover** — a big
reason companies love Kafka.

You can SEE this live: start a second instance of your consumer service
and watch the console print `partitions assigned: [...]`.

---

## In Spring Boot

```yaml
spring:
  kafka:
    consumer:
      group-id: product-group        # <- the team name
```

```java
@KafkaListener(topics = "order-events", groupId = "product-group")
public void consume(OrderEvent event) {
    // called automatically for each message this member receives
}
```

Scaling = run the same app again (different port). Same group id
→ Kafka splits the partitions between them. No code change.

---

## Key points (remember)

1. Consumer group = team of consumers with the same `group.id`.
2. **Same group → share the messages** (each message read once). Load balancing.
3. **Different groups → each group gets ALL messages.** Broadcast.
4. One partition is read by only ONE consumer of a group at a time.
5. Max useful consumers in a group = number of partitions.
6. Consumer joins/dies → **rebalancing** happens automatically.
