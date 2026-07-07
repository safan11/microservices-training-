# 01 — Kafka Basics

## What is Kafka? (one line)

> **Kafka is a system where one application drops a message,
> and other applications pick it up — without knowing each other.**

Think of it as a **post office** between your applications.

---

## The problem Kafka solves

Imagine an e-commerce app. When a customer places an order:

- Stock must be reduced
- Email must be sent
- Payment must be recorded

**Without Kafka** — order-service calls everyone directly:

```
order-service ──> product-service   (wait...)
              ──> email-service     (wait...)
              ──> payment-service   (wait...)
```

Problems:
1. **Slow** — customer waits for ALL of them.
2. **Fragile** — email server down? Whole order FAILS. 😱
3. **Coupled** — new service tomorrow? Change order-service code again.

**With Kafka** — order-service drops ONE message and moves on:

```
order-service ──> [ KAFKA ] ──> product-service
                            ──> email-service
                            ──> payment-service
```

Benefits:
1. **Fast** — drop the message, reply to customer immediately.
2. **Safe** — email service down? The message WAITS in Kafka.
3. **Free** — new service just starts listening. Nobody's code changes.

---

## The post office analogy (remember this!)

| Real world | Kafka word |
|-----------|------------|
| Post office building | **Broker** (the Kafka server) |
| PO Box with a label ("orders") | **Topic** |
| Person dropping letters | **Producer** |
| Person collecting letters | **Consumer** |
| "Letters kept for 7 days" | **Retention** |

---

## The 4 words you must know

```
 PRODUCER  ── writes ──>  TOPIC (inside a BROKER)  ── reads ──>  CONSUMER
```

| Word | Simple meaning |
|------|----------------|
| **Producer** | The app that SENDS messages |
| **Topic** | The named channel messages go into (like `order-events`) |
| **Broker** | The Kafka server that stores everything |
| **Consumer** | The app that RECEIVES messages |

---

## The most special thing about Kafka

> **Kafka does NOT delete a message when someone reads it.**

A normal queue: read the letter → letter is gone.
Kafka: read the letter → **letter stays in the box** (until retention time, default 7 days).

Why is this great? Because **10 different teams can each read the SAME letter**:
- product team reads it → reduces stock
- email team reads it → sends email
- analytics team reads it → records the sale

One message, many readers. That is Kafka's superpower.

---

## Where is Kafka used in real life?

- **E-commerce** — order placed, stock changed, price changed
- **Banking** — every transaction is an event, fraud detection watches them
- **Uber/Ola** — driver location updates flow through Kafka
- **Netflix** — what you watch becomes events → recommendations

---

## Key points (remember)

1. Kafka = post office between applications.
2. Producer sends → Topic stores → Consumer reads.
3. Messages are NOT deleted after reading — they expire later (retention).
4. Sender and receiver don't know each other = **loose coupling**.
5. If a consumer is down, messages simply WAIT for it. Nothing is lost.
