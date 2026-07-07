# 04 — Kafka Offsets

## What is an offset? (one line)

> **An offset is the position number of a message inside a partition —
> and a consumer's BOOKMARK saying "I have read up to here."**

---

## The book + bookmark analogy

A partition is like a book. Every message is a page with a page number:

```
Partition 0:
+--------+--------+--------+--------+--------+
| msg    | msg    | msg    | msg    | msg    |
| offset | offset | offset | offset | offset |
|   0    |   1    |   2    |   3    |   4    |
+--------+--------+--------+--------+--------+
                        ^
                 consumer's bookmark: "next read = offset 3"
```

- The pages (messages) never move or change.
- The consumer just moves its **bookmark** forward as it reads.
- Close the book (consumer stops) → the bookmark STAYS.
- Open it again (consumer restarts) → continue from the bookmark.
  **No message missed, nothing read twice.**

---

## Where is the bookmark stored?

Kafka stores the bookmark ITSELF, in an internal topic called
`__consumer_offsets`.

It is saved **per group, per partition**:

```
"product-group" + order-events partition 0  ->  offset 42
"product-group" + order-events partition 1  ->  offset 17
"email-group"   + order-events partition 0  ->  offset 40   (own bookmark!)
```

> Each GROUP has its own bookmark. product-group being at offset 42
> doesn't affect email-group at 40. Everyone reads at their own speed.

This is why a consumer that was DOWN for an hour catches up perfectly:
its bookmark waited, the messages waited, it just continues reading.

---

## Committing = saving the bookmark

"Committing an offset" simply means: **telling Kafka to save the bookmark.**

In Spring Boot this happens **automatically** after your
`@KafkaListener` method finishes successfully. You normally write zero
code for this.

```
message arrives -> your method runs -> success -> offset committed
                                    -> exception -> not committed
                                       (message can be retried)
```

---

## auto-offset-reset: earliest vs latest

One question Kafka must answer: *"A brand-NEW group connects — there is
no bookmark yet. Where should it start reading?"*

```yaml
spring:
  kafka:
    consumer:
      auto-offset-reset: earliest   # or latest
```

| Value | Meaning | Analogy |
|-------|---------|---------|
| `earliest` | Start from the beginning of the partition | Read the book from page 0 |
| `latest` (default) | Start from NOW — only new messages | Start reading from today's newspaper |

> **Important:** this setting is used ONLY when the group has NO saved
> bookmark (first time ever, or bookmark expired). Once a bookmark
> exists, Kafka always continues from it — this setting is ignored.

Common classroom confusion: *"I set earliest but old messages don't come
again!"* → Because the group already has a bookmark. Use a NEW group id
to re-read everything from the start.

---

## Consumer LAG (the #1 production metric)

```
LAG = (last offset written by producer) - (consumer's bookmark)
```

```
Partition 0: producer wrote up to offset 100
             consumer bookmark is at offset 90
             LAG = 10 messages behind
```

- LAG near 0 → consumer is keeping up. Healthy. ✅
- LAG growing and growing → consumer is too slow / stuck. Alarm! 🚨
  (Fix: speed it up, or add more consumers to the group.)

See it live:

```bash
docker exec -it kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group product-group
```

---

## Key points (remember)

1. Offset = message's position number in a partition (0, 1, 2, ...).
2. Consumer's offset = its **bookmark** — saved by Kafka per GROUP per partition.
3. Restarted consumer continues exactly where it stopped. Nothing lost.
4. Commit = save the bookmark. Spring Boot does it automatically after
   your listener method succeeds.
5. `earliest` / `latest` matter only for a group with NO bookmark yet.
6. **LAG** = how far behind a consumer is — the number to watch in production.
