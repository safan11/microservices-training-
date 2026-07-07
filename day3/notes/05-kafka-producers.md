# 05 — Kafka Producers

## What is a producer? (one line)

> **A producer is the application that CREATES and SENDS messages
> to a Kafka topic.**

It's the person dropping letters at the post office.

---

## What is inside one message?

A Kafka message (also called a **record**) has these parts:

```
+---------------------------------------------+
|  KEY     : "order-123"        (optional)     |
|  VALUE   : { the actual data, e.g. JSON }    |
|  headers : extra info         (optional)     |
|  -> Kafka adds: topic, partition, offset,    |
|                 timestamp                    |
+---------------------------------------------+
```

- **VALUE** = your real data (an OrderEvent as JSON, for example).
- **KEY** = decides WHICH partition the message goes to.
  Same key → same partition → order guaranteed for that key.

---

## How does a producer decide the partition?

```
Has a key?
   YES ──> hash(key) % number of partitions  -> same key = same partition
   NO  ──> spread messages around (round-robin / sticky)
```

**Practical advice:** use a business id as the key —
`orderId` for order events, `userId` for user events.

---

## Producer in Spring Boot (all the code you need)

### Step 1 — dependency (pom.xml)

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### Step 2 — application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092    # address of the Kafka broker
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

- **Serializer** = "how to convert my Java object into bytes for the network".
- `JsonSerializer` → your object becomes JSON automatically. No manual code.

### Step 3 — send with KafkaTemplate

```java
@Service
public class OrderProducerService {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderProducerService(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;   // Spring auto-creates this bean
    }

    public void sendOrder(OrderEvent orderEvent) {
        //                 topic          key                      value
        kafkaTemplate.send("order-events", orderEvent.getOrderId(), orderEvent);
        System.out.println("Order event sent: " + orderEvent.getOrderId());
    }
}
```

> `KafkaTemplate` is to Kafka what `RestTemplate` is to REST —
> Spring's ready-made helper. `<String, OrderEvent>` = <key type, value type>.

---

## send() is ASYNCHRONOUS (important!)

`kafkaTemplate.send(...)` does **not wait**. It hands the message to a
background thread and returns immediately. That's why producers are fast.

Want to know if it really arrived? The result is a future — add a callback:

```java
kafkaTemplate.send("order-events", key, event)
    .whenComplete((result, ex) -> {
        if (ex == null) {
            System.out.println("Delivered to partition "
                + result.getRecordMetadata().partition()
                + " at offset " + result.getRecordMetadata().offset());
        } else {
            System.out.println("Failed: " + ex.getMessage());
        }
    });
```

---

## acks — how safe do you want delivery to be?

When the producer sends, how many confirmations does it wait for?

| Setting | Meaning | Speed | Safety |
|---------|---------|-------|--------|
| `acks=0` | Don't wait at all ("throw and run") | Fastest | Can lose messages |
| `acks=1` | Wait for the main broker only | Fast | Small risk |
| `acks=all` | Wait until all replica copies have it | Slower | Safest ✅ |

Modern Kafka defaults to `acks=all` — safe out of the box.
(Just know the concept — it's a favorite interview question.)

Also nice to know: the producer **retries automatically** on temporary
network errors, and **batches** messages together for efficiency.

---

## Key points (remember)

1. Producer = the sender. Message = key + value (+ headers).
2. Key decides the partition: same key → same partition → ordering.
3. Spring Boot: `KafkaTemplate.send(topic, key, value)` — one line.
4. Serializer converts object → bytes. `JsonSerializer` = automatic JSON.
5. `send()` is asynchronous — it does not block your API.
6. `acks=all` = safest delivery (wait for all copies). Default in new Kafka.
