# 06 — Kafka Brokers

## What is a broker? (one line)

> **A broker is the Kafka SERVER — the machine that receives messages,
> stores them on disk, and gives them to consumers.**

It's the post office building itself.

---

## One broker vs a cluster

**For learning (our demo):** ONE broker in Docker is enough.

```
              +--------------------+
Producer ──>  |     BROKER 1       |  ──> Consumer
              |  (all partitions)  |
              +--------------------+
```

**In production:** several brokers work together = a **CLUSTER**
(usually 3 or more).

```
              +-----------+  +-----------+  +-----------+
Producer ──>  | BROKER 1  |  | BROKER 2  |  | BROKER 3  |  ──> Consumer
              +-----------+  +-----------+  +-----------+
                    \______________|______________/
                            one CLUSTER
```

Why more than one?
1. **More storage & speed** — partitions are spread across brokers.
2. **Safety** — one machine dies, the others keep working.

---

## How partitions live on brokers

A topic's partitions are DISTRIBUTED across the brokers:

```
Topic "order-events" (3 partitions), cluster of 3 brokers:

BROKER 1: Partition 0 (leader)   Partition 2 (copy)
BROKER 2: Partition 1 (leader)   Partition 0 (copy)
BROKER 3: Partition 2 (leader)   Partition 1 (copy)
```

Two words here:

| Word | Meaning |
|------|---------|
| **Leader** | The main copy of a partition. Producers/consumers talk to the leader. |
| **Replica (copy)** | Backup copy on another broker. Stays in sync silently. |

---

## Replication = photocopies for safety

`replication-factor = 3` means every partition exists on 3 brokers
(1 leader + 2 copies).

**If a broker dies:**

```
BROKER 1 (leader of P0) 💥 crashes...

Kafka instantly promotes the copy on BROKER 2 to be the new leader.
Producers and consumers switch automatically. NO data lost, NO downtime.
```

> Analogy: you photocopy an important document and keep copies in
> 3 different cupboards. One cupboard burns — you still have the document.

- Demo/learning: `replication-factor = 1` (we only have 1 broker).
- Production: `replication-factor = 3` (industry standard).

---

## Who manages the cluster? (ZooKeeper vs KRaft)

Somebody must keep track of: which brokers are alive, who is leader
of each partition, topic configurations...

| Era | Manager |
|-----|---------|
| Old Kafka (< 3.x) | A separate system called **ZooKeeper** (extra thing to install & maintain) |
| Modern Kafka | **KRaft mode** — Kafka manages ITSELF. No ZooKeeper needed. ✅ |

Our Docker setup uses KRaft — that's why the compose file has only ONE
container for Kafka and nothing else.

(If you see old tutorials with a `zookeeper:` service in docker-compose —
that's the old way. Not needed anymore.)

---

## bootstrap-servers — the front door

In every app config you write:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

**Why "bootstrap"?** Your app contacts this ONE address just to say hello.
The broker replies with the full map: *"here are all the brokers in the
cluster and who leads which partition."* After that, the app talks to
whichever broker it needs, directly.

> Like calling a company's reception once — the receptionist gives you
> everyone's direct numbers.

In production you list 2–3 brokers there (in case one is down when the
app starts):

```yaml
bootstrap-servers: broker1:9092,broker2:9092,broker3:9092
```

---

## Our demo broker (Docker)

```bash
docker compose up -d      # starts the broker (and kafka-ui)
docker ps                 # check "kafka" container is Up
docker logs kafka --tail 20
```

One broker in a container = a full Kafka post office on your laptop.

---

## Key points (remember)

1. Broker = the Kafka server. Cluster = several brokers together.
2. Partitions are spread across brokers; each has ONE leader + replicas.
3. Replication-factor 3 = every message stored on 3 machines. Broker
   dies → a copy is instantly promoted. No data loss.
4. Modern Kafka uses **KRaft** — no ZooKeeper anymore.
5. `bootstrap-servers` = the first address your app calls to discover
   the whole cluster.
6. Demo: 1 broker, replication 1. Production: 3+ brokers, replication 3.
