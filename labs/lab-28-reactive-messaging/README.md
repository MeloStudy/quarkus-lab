# 📬 Lab 28 - Reactive messaging (in memory)

| ⏱️ Duration | 🚀 Quarkus | ☕ Java | 🧪 Type |
|---|---|---|---|
| 2 h | 3.40.1.1 LTS (SmallRye Reactive Messaging) | 21 | A (starter + solution) |

## 📍 Where to run commands
Open a terminal **inside this lab's `starter/` folder** and check where you are before running anything:

```powershell
Get-Location
```

```bash
pwd
```

> [!NOTE]
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead. No Docker and no broker are needed: every channel of this lab lives inside the application. Kafka comes in lab 29 and uses everything you learn here.

## 🎯 Why it matters
When a request becomes a **message**, the code that sends it and the code that handles it no longer share a call stack. Two questions that were free in a method call become your problem: **when does the sender find out that the message was handled** (or that it was not), and **what happens to the stream when one message cannot be handled**. Get the first one wrong and clients wait for answers that never come; get the second one wrong and a single bad order silently stops every order behind it, until somebody restarts the service. Both are the same in memory, on Kafka and on RabbitMQ: that is why this lab uses plain channels and no broker.

## 🧠 Concepts
| Idea | In one line |
|---|---|
| **Channel** | A named stream inside the application. `@Incoming("x")` consumes it, `@Outgoing("x")` produces into it, `@Channel("x") Emitter<T>` lets imperative code (an endpoint) push into it |
| **Processor** | A method with `@Incoming("a") @Outgoing("b")`: it sits between two channels |
| **Acknowledgement** | Every message ends in an **ack** (done) or a **nack** (failed, with the cause). `Emitter.send(x)` returns a `CompletionStage<Void>`: it completes on ack and completes **exceptionally** on nack |
| **Who acks** | A method that takes the **payload** is acknowledged **after it returns** (`POST_PROCESSING`): a return is an ack, an exception is a nack. A method that takes a `Message<T>` is **manual**: you must call `ack()` or `nack(...)`. `@Acknowledgment(PRE_PROCESSING)` acks before your code runs |
| **Dead letters** | Where a message that cannot be processed goes so that it is not lost. In memory it is just another channel; with a broker it is a topic or a queue (lab 29) |
| **Overflow** | An `Emitter` has a buffer (`@OnOverflow`). What happens when it is full is a choice: `BUFFER` (bounded), `DROP`, `FAIL`... |
| **Where it runs** | A consumer that is not `@Blocking` runs on an **event-loop** thread (lab 26). `@Blocking` moves it to a worker, one message at a time and in order |

```mermaid
flowchart LR
    C[client] -->|POST /orders| R[OrderResource]
    R -->|Emitter.send| O((orders))
    O --> I[OrderIntake<br/>processor]
    I --> F((fulfilment))
    F --> U[Fulfilment<br/>@Blocking]
    U -->|warehouse unavailable| D((dead-letters))
    D --> S[DeadLetterStore]
    U -.->|ack or nack travels back| R
```

## 📚 Documentation
- [Quarkus: Messaging](https://quarkus.io/guides/messaging)
- [Quarkus: Messaging and virtual threads](https://quarkus.io/guides/messaging-virtual-threads)
- [SmallRye Reactive Messaging](https://smallrye.io/smallrye-reactive-messaging/latest/): [acknowledgement](https://smallrye.io/smallrye-reactive-messaging/latest/concepts/acknowledgement/), [emitters](https://smallrye.io/smallrye-reactive-messaging/latest/concepts/emitter/) and [testing](https://smallrye.io/smallrye-reactive-messaging/latest/concepts/testing/)
- Lab 23 (the retry on the warehouse call), lab 26 (event loop and workers), lab 27 (Mutiny: the endpoint returns a `Uni`)

## 🧭 Scenario
An **order desk**. A client posts an order; the order travels through two channels (a validation step and a fulfilment step that reserves stock in a slow, blocking warehouse); the client gets an answer only when the pipeline says what happened to its order.

| Answer | When |
|---|---|
| `201` + `Location` + `{id, status}` | the order was acknowledged (the stock is reserved) |
| `422 INVALID_ORDER` | the order itself is wrong: quantity not positive, unknown sku. The sender's fault |
| `409 OUT_OF_STOCK` | the warehouse has none (`SOLD-OUT` in the demo catalog). A business answer: never retried |
| `502 WAREHOUSE_UNAVAILABLE` | the warehouse did not answer, even after the retries (`OFFLINE` always fails). The order is kept in the dead letters |
| `503 BUSY` + `Retry-After: 1` | the channel has no room right now |
| `504 NO_ACK` | nobody acknowledged or rejected the order in time |

Also: `GET /orders/{id}` (what the ledger remembers, and on which thread the order was handled) and `GET /dead-letters`.

Already written: the model, the warehouse (with its `@Retry` from lab 23, only for the failure that can go away), the ledger, the error mapping, the dead-letter store and **all the tests**. Written for you in a first version that works when nothing goes wrong: `Fulfilment`. **Your job is the channels.**

| Task | What must be true |
|---|---|
| 1 | An order goes `orders` -> `fulfilment`; the client waits for the ack and a refused order comes out as a `422` |
| 2 | An order that fails reaches its sender as that failure, and the orders behind it still go through |
| 3 | A warehouse that is down is retried, then the order is kept in the dead letters; a refusal or an out-of-stock order is not |
| 4 | The warehouse call does not run on the event loop, and a full channel answers `503` instead of hanging or breaking |

> [!IMPORTANT]
> All the tests share **one running application**, and one of the traps of this lab (task 2) can cancel a channel **for the rest of the application's life**. The test classes are therefore ordered by task (`junit-platform.properties`). Work in order: until task 2 is solved, the classes after `FailureTest` are red because of the dead channel, not because of what they test.

## ✅ Tasks

### Task 1 - Wire the pipeline
**Goal:** the order travels through the channels and the answer depends on what comes back. **Validated by:** `PipelineTest`.

- [ ] `OrderIntake.validate`: make it the processor between `orders` and `fulfilment`, and refuse a bad order by throwing `InvalidOrderException` (quantity not positive; sku not in `Warehouse.CATALOG`).
- [ ] `OrderResource.place`: inject an `Emitter<Order>` for the `orders` channel, give the order an id, send it, and answer `201` with a `Location` header and a `Placed` body **when the channel acknowledges it**. A nack must reach the exception mappers as the exception the stage threw; nobody answering within `ackTimeoutMillis` is a `NoAckException`.

```powershell
.\mvnw.cmd test '-Dtest=PipelineTest'
```

```bash
./mvnw test -Dtest=PipelineTest
```

🔮 **Predict first:** (a) `Emitter.send(order)` returns before or after the order is handled? (b) the intake step throws: what does the `CompletionStage` do, and which exception does the endpoint see? (c) you misspell the channel name in `@Outgoing("fulfilments")`: does the application start?

> [!WARNING]
> A misspelled channel name is not an error at startup. Read the log of your application before looking at anything else.

### Task 2 - One bad order must not stop the others
**Goal:** a failure reaches its sender, and the channel keeps working. **Validated by:** `FailureTest`.

- [ ] Run `FailureTest` against the first version of `Fulfilment` and read **both** the test report and the application log. What happens to the sender of the `SOLD-OUT` order? And to the next order?
- [ ] Change `Fulfilment` so that an out-of-stock order reaches its sender as `409 OUT_OF_STOCK`, is `OUT_OF_STOCK` in the ledger, and does not stop the orders behind it.

```powershell
.\mvnw.cmd test '-Dtest=PipelineTest,FailureTest'
```

```bash
./mvnw test -Dtest=PipelineTest,FailureTest
```

🔮 **Predict first:** the first version takes a `Message<Order>`, calls `reserve` and then `message.ack()`. `reserve` throws. (a) What does the sender see? (b) What does the *next* order see? (c) Would `@Acknowledgment(PRE_PROCESSING)` have helped?

> [!WARNING]
> There are two ways to write a consumer that look equally natural and behave very differently when something throws. Find out which one is safe, and why, before choosing.

### Task 3 - Retry what can go away, keep what cannot be lost
**Goal:** failures are classified: the sender's fault, a business answer, and a failure that may be temporary. **Validated by:** `DeadLetterTest`.

- [ ] `DeadLetterStore.keep` is the end of the `dead-letters` channel: make it a consumer of that channel.
- [ ] In `Fulfilment`, when the warehouse is unavailable (after the retries of `Warehouse.reserve`), send a `DeadLetter` to the `dead-letters` channel (an `Emitter<DeadLetter>`), mark the order `DEAD` in the ledger and let the failure reach the sender (`502`).
- [ ] An out-of-stock order and an order refused by the intake step are **not** dead letters.

```powershell
.\mvnw.cmd test '-Dtest=PipelineTest,FailureTest,DeadLetterTest'
```

```bash
./mvnw test -Dtest=PipelineTest,FailureTest,DeadLetterTest
```

🔮 **Predict first:** the warehouse fails twice and then works: how many times is `reserve` called, and what does the client get? And if it fails forever?

> [!WARNING]
> The retry is on `Warehouse.reserve`, not on the message. Think about what the sender sees while the retries happen, and about what would change if you retried by sending the message again.

### Task 4 - Where it runs, and what happens when it is full
**Goal:** the blocking call stays off the event loop, and a full channel is a `503`. **Validated by:** `ExecutionTest` and `PressureTest`.

- [ ] `ExecutionTest` reads on which thread the order was handled (the ledger remembers it). Run it, read the thread name, and fix `Fulfilment`.
- [ ] Give the emitter in `OrderResource` an explicit bounded buffer of 10 (`@OnOverflow`). When the channel has no room, `send` throws: answer `503 BUSY` (`BusyException`) with `Retry-After`. A channel that was **cancelled** throws the same exception, and that one is not "busy": do not hide it behind a 503.

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

🔮 **Predict first:** (a) a consumer that is not `@Blocking` and sleeps: which thread runs it when the caller is a worker thread of an endpoint? (b) the emitter has a buffer of 10 and the consumer takes 80 ms per order: how many of 40 simultaneous orders are accepted? (c) what would `DROP` and `FAIL` do instead of `BUFFER`?

> [!WARNING]
> `@OnOverflow` has more than one answer to "what if it is full", and not all of them leave you with a working channel.

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green (22 tests).

## 👀 What you should see
The application log is where messaging failures speak. A nack from a payload method:

```text
ERROR [io.smallrye.reactive.messaging.provider] (executor-thread-1) SRMSG00200: The method lab.melostudy.Fulfilment#fulfil has thrown an exception: lab.melostudy.Exceptions$OutOfStockException: No stock of SOLD-OUT
```

A channel that has nobody on the other side (a misspelled name), at startup, and nothing more:

```text
WARN  [io.smallrye.reactive.messaging.provider] (main) SRMSG00207: Some components are not connected to either downstream consumers or upstream producers:
	- ProcessingMethod{method:'lab.melostudy.OrderIntake#validate', incoming:'orders', outgoing:'fulfilments'} has no downstream
	- SubscriberMethod{method:'lab.melostudy.Fulfilment#fulfil', incoming:'fulfilment'} has no upstream
```

A finished desk, through HTTP (see "Run the jar" below):

```text
POST sample order      ->  201 {"id":"...","status":"FULFILLED"}
POST SOLD-OUT          ->  409 {"code":"OUT_OF_STOCK",...}
POST OFFLINE           ->  502 {"code":"WAREHOUSE_UNAVAILABLE",...}   and the order is in GET /dead-letters
30 simultaneous orders ->  12 x 201 and 18 x 503 (300 ms per order)
```

## 🏃 Run the jar
Not a test: the same application as a packaged jar, with the sample orders of `samples/`. Open a terminal inside `solution/` (or your finished `starter/`) and build and start it:

```powershell
.\mvnw.cmd -q -DskipTests package
java -jar target\quarkus-app\quarkus-run.jar
```

In a **second terminal**, also inside `solution/`:

```powershell
curl.exe -s -i -H "Content-Type: application/json" --data-binary "@..\samples\order.json" http://localhost:8080/orders
curl.exe -s -i -H "Content-Type: application/json" --data-binary "@..\samples\sold-out.json" http://localhost:8080/orders
curl.exe -s -i -H "Content-Type: application/json" --data-binary "@..\samples\unknown-sku.json" http://localhost:8080/orders
curl.exe -s -i -H "Content-Type: application/json" --data-binary "@..\samples\offline.json" http://localhost:8080/orders
curl.exe -s http://localhost:8080/dead-letters
```

```bash
curl -s -i -H "Content-Type: application/json" --data-binary "@../samples/order.json" http://localhost:8080/orders
curl -s -i -H "Content-Type: application/json" --data-binary "@../samples/offline.json" http://localhost:8080/orders
curl -s http://localhost:8080/dead-letters
```

Now make the warehouse slow (300 ms per order; stop the application with `Ctrl+C` first) and send 30 orders at the same time:

```powershell
$env:APP_WAREHOUSE_DELAY_MILLIS = '300'
java -jar target\quarkus-app\quarkus-run.jar
```

```bash
APP_WAREHOUSE_DELAY_MILLIS=300 java -jar target/quarkus-app/quarkus-run.jar
```

```powershell
curl.exe -s --parallel --parallel-max 30 -o NUL -w "%{http_code}\n" -H "Content-Type: application/json" --data-binary "@..\samples\order.json" "http://localhost:8080/orders?n=[1-30]" | Group-Object | Select-Object Count, Name
```

```powershell
Remove-Item Env:\APP_WAREHOUSE_DELAY_MILLIS
```

Expected (in the run that wrote this lab): **12** answers `201` and **18** answers `503`. Your numbers differ by one or two: it depends on how many requests arrive before the first order is done.

## 💼 In real projects
- **Decide what the sender is told, on purpose.** "Fire and forget" (`send` and answer `202`) and "wait for the ack" (this lab) are both valid. The first is faster and hides failures from the client; the second ties the request to the slowest stage. Whatever you choose, a timeout is mandatory: a message that is never acknowledged is a request that never ends (`504 NO_ACK` here).
- **Ack means "the method finished", not "the business is done"** unless the message is passed along: a payload processor's ack/nack travels back through the chain here (a failing last stage nacked the original sender), but in an exploration with `@Broadcast` and two consumers the sender got an **ack** although one of the two consumers threw (see the stretch goal). Test your pipeline end to end for the case that matters.
- **Classify failures before choosing a policy.** Invalid input: reject, never retry, never dead-letter. A business answer: tell the sender, record it. A temporary failure: retry with a limit, then **keep the message** and make a human or a replay job able to find it. A pipeline with one policy for everything either retries forever or throws data away.
- **Retries belong to the call that fails** (`Warehouse.reserve` with `@Retry`), not to the message: re-sending the message re-runs every stage, including the ones that already worked.
- **One poison message must not be able to stop a channel.** The tests of this lab use a deliberately bad order for that reason. In production, add the alert: a channel that stops consuming is silent unless you watch for it (a counter of messages handled per minute, with the metrics of lab 24).
- **Bound everything.** An unbounded buffer is a memory leak waiting for a slow consumer; `@OnOverflow` with a size is a decision. And choose what "full" means for the caller: `503` and `Retry-After` is an honest answer.
- **Blocking code belongs on `@Blocking`** (or virtual threads: see the Quarkus guide above). The blocked-thread warning of lab 26 applies to consumers too.
- **Channels in memory are not durable.** An order in `orders` when the process dies is gone. In-memory channels are for decoupling stages **inside** one service; between services, or whenever losing a message matters, use a broker (lab 29) and, for "update the database and publish", the outbox idea.
- **Names are the contract, and the compiler does not check them.** A misspelled channel starts the application with a warning. A test that sends one message through the whole pipeline is the cheapest guard (task 1).

## 🚀 Stretch goal
1. **`@Broadcast`.** Add a second consumer of `fulfilment` (an audit log) with `@Broadcast` on the producing side. Predict: if the audit consumer throws, what does the sender get? Write the test.
2. **Fire and forget.** Add `POST /orders/async` that answers `202` as soon as the channel accepts the order, and a way to ask later (`GET /orders/{id}`). What happens to a failure now? Where does a client learn about it?
3. **Testing without the pipeline.** Read the [testing page](https://smallrye.io/smallrye-reactive-messaging/latest/concepts/testing/) and replace the `orders` channel's source with the in-memory connector in a test. When is that better than the HTTP tests of this lab?
4. **A real retry of the message.** Re-send dead letters when the warehouse is back (a `POST /dead-letters/replay`). What must be true so that replaying an order cannot reserve its stock twice? (Think of P2's idempotency key.)

## 💡 Hints
<details><summary>Task 1 (direction)</summary>

`@Incoming("orders") @Outgoing("fulfilment")` on a method that takes an `Order` and returns an `Order`: what it returns goes on to the next channel; what it throws is a nack. In the resource, `Uni.createFrom().completionStage(...)` turns the stage returned by `send` into something an endpoint can return, and `ifNoItem().after(...)` is the timeout of lab 27.
</details>

<details><summary>Task 2 (direction)</summary>

Compare what the sender sees, what the log says and what the *next* order sees with a `Message<Order>` method and with an `Order` method. Which of the two decides for you whether a thrown exception is a nack?
</details>

<details><summary>Task 3 (direction)</summary>

A `catch` for each kind of failure, in `Fulfilment`: the `OutOfStockException` is recorded and rethrown; the unavailable warehouse is sent to the dead letters, recorded and rethrown. Rethrow, or the sender is told `201`.
</details>

<details><summary>Task 3 (more concrete)</summary>

`@Inject @Channel("dead-letters") Emitter<DeadLetter>`; inject it by constructor (as `Warehouse` does with its configuration). `emitter.send(new DeadLetter(order, e.getMessage()))`. A channel with an emitter and no `@Incoming` fails when the emitter is injected; with the `@Incoming` on `DeadLetterStore.keep` it works.
</details>

<details><summary>Task 4 (direction)</summary>

`io.smallrye.common.annotation.Blocking` on the consumer. For the endpoint: `@Inject @Channel("orders") @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 10) Emitter<Order> orders;` and a `try/catch` around `send`. `Emitter` has `isCancelled()`.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1. Times are from one machine; the proportions are what matters.

**Task 1.** (a) `send` returns **at once** in an exploration where the consumer was `@Blocking` (0 ms) and the stage completes later (about 50 ms in the same run, the sleep of the consumer). It does **not** return at once when the consumer is not `@Blocking`: the consumer ran inside `send` (a 50 ms sleep made `send` itself take 51 ms). (b) The stage completes **exceptionally** with the exception the stage threw (`IllegalStateException: boom bad1` in the exploration), and `Uni.createFrom().completionStage(...)` fails with that same exception: that is why the mappers of `ErrorMappers` work unchanged. A payload processor that returns normally, followed by a consumer that throws, also nacked the original sender (a test sent `OK1`, `BAD1`, `OK2`: ack, nack, ack). (c) **Yes, it starts.** The log has one `WARN` (`SRMSG00207`, shown above). The failure arrives at the **first request** that touches the emitter, as a `500`: a `DefinitionException` whose message starts with `SRMSG00019: Unable to connect an emitter with the channel` and names the channel. All six `PipelineTest` tests were red with this mistake.

**Task 2.** With the first version (`Message<Order>` and `message.ack()` at the end): the sender of the `SOLD-OUT` order got **`504`** after the timeout (nobody acked and nobody nacked), the log had `SRMSG00200` (the method threw) and then `SRMSG00201` twice (error caught while processing a message), and **every later order, from any client, got a `500`**: `java.lang.IllegalStateException: SRMSG00028: The subscription to orders has been cancelled`. The exception thrown by a method that takes a `Message` and is acknowledged manually **cancels the subscription of the whole chain**, up to the emitter; nothing restarts it. `@Acknowledgment(PRE_PROCESSING)` is worse: the message is acked before the method runs, so the sender got **`201` for an order that failed** (the tests expected `409`), and the channel was cancelled as well (`SRMSG00201`, then `SRMSG00028` on the next send in the exploration). A method that takes the **payload** (`void fulfil(Order order)`) is acknowledged after it returns: an exception becomes a nack that reaches the sender, and the next order was processed normally (`FailureTest` sends three failing orders in a row and then two good ones). If you really need the `Message` (to read metadata), catch the exception yourself and call `message.nack(e)`.

**Task 3.** Two failures and a success: `reserve` was called **3** times (the call and two retries) and the client got `201`. A warehouse that never answers: **3** calls, then `502`, one dead letter, the ledger says `DEAD`. Out of stock: **1** call, `409`, no dead letter. Without `retryOn` the annotation retried the out-of-stock too (3 calls for a business answer). A channel with `Emitter<DeadLetter>` and no `@Incoming("dead-letters")` also gives `SRMSG00019` the first time a failure tries to use it, and every dead-letter test was red.

**Task 4.** (a) A consumer without `@Blocking` ran on `vert.x-eventloop-thread-N` **even when the caller was an endpoint worker** (`executor-thread-1` called `send`; the consumer ran on the event loop in the exploration). With `@Blocking` it ran on `executor-thread-N`, one order at a time and in order. (b) A buffer of 5 accepted **6** (one in process, five waiting); the default buffer of 128 accepted **129** and then `send` threw `java.lang.IllegalStateException: SRMSG00034: Insufficient downstream requests to emit item`; the channel kept working afterwards (the next order was acknowledged). With a buffer of 10, 40 simultaneous orders gave the accepted ones a `201` and everyone else a `503`. (c) `DROP`: the message is thrown away and **its stage never completes**, neither ack nor nack: the client got **`504`** after the timeout, mostly (`[504, 504, ..., 201, 504, ...]`). `FAIL`: the first send into a full channel fails with `io.smallrye.mutiny.subscription.BackPressureFailure: Could not emit value due to lack of requests`, and **the channel is cancelled for good** (`SRMSG00028` afterwards): the tests for "every accepted order was fulfilled" and "the channel recovers" were red in all runs. Without any `@OnOverflow` (the default buffer of 128) nobody was refused: the 40 orders were all accepted, the consumer needed 80 ms for each, and many clients got `504` (`[504, 504, 504, 504, 201, 201, 504, ...]`). Two injections of the same channel with different `@OnOverflow` settings are a deployment error (in the exploration: `DeploymentException: Emitter configuration for channel ... is different than previous configuration`): one channel, one emitter configuration.

**What each piece protects (mutation table).** Each row is the solution with one thing changed:

| Changed in the solution | What fails |
|---|---|
| `@Blocking` removed from `Fulfilment` | `theWarehouseIsNotCalledOnTheEventLoop`: `ran on vert.x-eventloop-thread-0` |
| `Fulfilment` back to the first version (`Message`, manual ack) | the four tests of `FailureTest` (`504` instead of `409`, then `500`) |
| `@PRE_PROCESSING` acknowledgement | the four tests of `FailureTest` (`201` instead of `409`) |
| the failure swallowed (no rethrow) | the four tests of `FailureTest` (`201` instead of `409`) |
| `@Retry` removed from the warehouse | `aTransientFailureIsRetriedAndTheOrderGoesThrough` and `aFailureThatNeverGoesAwayAnswers502` |
| `retryOn` removed | `anOutOfStockOrderIsNeitherRetriedNorKept` |
| the dead letter is not sent | the dead-letter tests that read the store or the endpoint |
| an out-of-stock order is sent to the dead letters | `anOutOfStockOrderIsNeitherRetriedNorKept` |
| `@Incoming("dead-letters")` removed | all of `DeadLetterTest` |
| no validation of the quantity | `aWrongQuantityIsRefusedBeforeTheWarehouse` (`201` instead of `422`) |
| `@Outgoing("fulfilments")` (typo) | all of `PipelineTest` |
| the `catch` around `send` removed | `aFullChannelAnswers503WithRetryAfter` (`500`) |
| `OnOverflow.Strategy.FAIL` | three tests of `PressureTest` |
| `OnOverflow.Strategy.DROP` | `aFullChannelAnswers503WithRetryAfter` (`504`) |
| no `@OnOverflow` (default buffer) | `aFullChannelAnswers503WithRetryAfter` (no `503`) |
</details>

## 🎤 Self-assessment
1. `Emitter.send(x)` returns a `CompletionStage`. When does it complete, and how do you tell an ack from a nack?
2. A consumer throws on one message. With a method that takes the payload and with a method that takes a `Message<T>` and acks by hand: what does the sender see, and what happens to the next messages?
3. Your pipeline starts fine and nothing arrives at the second stage. Where do you look first, and which kinds of mistake produce this?
4. An order fails because the warehouse is down, another because the stock is zero, another because the quantity is negative. Which are retried, which go to a dead-letter channel, and what does each sender get? Why?
5. The consumer is slow and the producers are fast. Compare `BUFFER` with a size, `DROP` and `FAIL` for an order desk and for a stream of sensor readings.

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
