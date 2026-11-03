# 📈 Lab 24 - Health and metrics

| ⏱️ Duration | 🚀 Quarkus | ☕ Java | 🧪 Type |
|---|---|---|---|
| 1-2 h | 3.40.1.1 LTS | 21 | A (starter + solution) |

## 📍 Where to run commands
Open a terminal **inside this lab's `starter/` folder** and check where you are before running anything:

```powershell
Get-Location
```

```bash
pwd
```

> [!NOTE]
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead. No Docker is needed.

## 🎯 Why it matters
Lab 23 made the application survive its dependencies. This lab lets the **platform and the people** see how it is doing. A platform (Kubernetes, a load balancer) asks "are you alive? may I send you traffic? have you finished starting?" and acts on the answer: restart you, stop routing to you, wait for you. People ask "how many, how long, how much waiting?" and want a graph, an alert and a trend. Health checks and metrics are how a service answers. A wrong answer is expensive: a liveness check that looks at the database restarts every pod when the database blinks.

## 🧠 Concepts
| Probe | The platform's question | The platform's reaction when DOWN | What the check may look at |
|---|---|---|---|
| **Liveness** `/q/health/live` | Is this process broken in a way only a restart fixes? | **Restart it** | Only the process itself (a stuck thread, a deadlock). **Never a dependency** |
| **Readiness** `/q/health/ready` | Should I send traffic to this instance now? | **Stop routing** to it, do not restart | Whatever it needs in order to serve: its dependencies, warm-up, "draining" |
| **Startup** `/q/health/started` | Has the slow initialization finished? | Keep waiting; other probes wait too | One-time initialization |

- A check answers with `UP` or `DOWN` and optional data; the HTTP status is **200 when UP and 503 when DOWN**. `/q/health` combines all of them.
- **Metrics** are numbers with names and **tags** (labels) that a server (Prometheus) collects at intervals. Micrometer is the facade the code uses; the Prometheus registry renders the text format at `/q/metrics`. Three shapes cover most needs: a **counter** (only goes up: orders created), a **timer** (how long, how many) and a **gauge** (a value that goes up and down: queue size).
- Every distinct combination of a metric name and tag values is a **separate time series** that the monitoring system stores forever. Tags with unbounded values (an order id, a user id, a raw URL) create millions of series and take the monitoring down. This is called **cardinality**.
- The **management interface** is a second HTTP server inside the application, on its own port (default 9000), for operations endpoints (`/q/health`, `/q/metrics`). The public port then carries only the business API.

## 📚 Documentation
- [SmallRye Health](https://quarkus.io/guides/smallrye-health)
- [Micrometer metrics](https://quarkus.io/guides/telemetry-micrometer)
- [Management interface reference](https://quarkus.io/guides/management-interface-reference)
- [Deploying to Kubernetes](https://quarkus.io/guides/deploying-to-kubernetes) (how the health endpoints become probes)

## 🧭 Scenario
A small **orders service**: `POST /api/orders` reserves stock in an inventory (a stand-in for a remote service that the tests can switch off), stores the order and hands it to a **background worker** that processes it. `GET /api/orders/{id}` reads it.

Already written: the business code, a worker that writes a **heartbeat** every time around its loop (and can be stalled by the tests), an inventory that can be switched off, and the dependencies in the `pom.xml`. **Your job is everything about observing it.**

| Task | What the application must report |
|---|---|
| 1 | Operations endpoints on the management port only |
| 2 | Liveness: the worker is alive (a silence longer than the configured limit = stuck). Readiness: the inventory is available. Startup: the worker has started |
| 3 | Counter `orders.created` (tag `type`), timer `orders.processing`, gauge `orders.queue.size` |
| 4 | Every metric carries `application="orders"`; no identifiers as tags |

In the tests the management interface answers on port **9001** (the tests' own port, as 8081 is for the public one); in dev and in the jar it is **9000**.

## ✅ Tasks

### Task 1 - Operations on their own port
**Goal:** `/q/health` and `/q/metrics` answer on the management interface and are gone from the public port. **Validated by:** `ManagementInterfaceTest`.

- [ ] Enable the management interface in `application.properties`.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=ManagementInterfaceTest'
```

```bash
./mvnw test -Dtest=ManagementInterfaceTest
```

🔮 **Predict first:** after you enable it, does `GET /q/health` still answer on the public port? What does it answer there?

### Task 2 - Three questions, three checks
**Goal:** liveness, readiness and startup each answer their own question. **Validated by:** `HealthTest`.

- [ ] A liveness check named `order-worker`: UP while `processor.millisSinceHeartbeat()` is below the limit `app.worker.max-silence-millis` (already in the properties; the test profile uses 1 s). Add the silence as data.
- [ ] A readiness check named `inventory`: UP exactly when the inventory is available.
- [ ] A startup check named `worker-started`: UP when the worker is running.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=HealthTest'
```

```bash
./mvnw test -Dtest=HealthTest
```

> [!WARNING]
> Look at `withoutTheInventoryTheApplicationIsStillAlive`. Which of the three checks would be tempting to make depend on the inventory, and what would the platform do?

🔮 **Predict first:** the inventory goes down. What do `/q/health/live` and `/q/health/ready` answer (status code each)? And a platform that restarts on a failed liveness: what would it do if the liveness check also looked at the inventory?

### Task 3 - Numbers for the dashboard
**Goal:** the application counts, times and measures. **Validated by:** `MetricsTest`.

- [ ] In `OrderService.create`: a counter `orders.created` with a tag `type` (standard or express), counted only for orders that were created.
- [ ] In `OrderProcessor`: a timer `orders.processing` around the processing of each order, and a gauge `orders.queue.size` for the queue.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=MetricsTest'
```

```bash
./mvnw test -Dtest=MetricsTest
```

🔮 **Predict first:** the code says `orders.created`. How will the name look in `/q/metrics`? And the timer `orders.processing`: which lines does it produce?

### Task 4 - Metrics that stay useful
**Goal:** every metric carries `application="orders"`; the metrics of the application have no identifiers as tags, and HTTP metrics are per path template. **Validated by:** `MetricHygieneTest`.

- [ ] Add a `MeterFilter` bean that adds the common tag to every meter.
- [ ] Run the test class. Two of its three tests are guards that should already pass; read them.

```powershell
.\mvnw.cmd test '-Dtest=MetricHygieneTest'
```

```bash
./mvnw test -Dtest=MetricHygieneTest
```

🔮 **Predict first:** a request to `GET /api/orders/3` and another to `GET /api/orders/4`: how many series does the HTTP metric get? And a request for a path that matches nothing, `GET /nothing/here/123`?

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green.

## 👀 What you should see
Run the finished project (in `solution/`) and look at it as an operator:

```powershell
.\mvnw.cmd quarkus:dev
```

```bash
./mvnw quarkus:dev
```

In another terminal:

```powershell
curl.exe -s http://localhost:9000/q/health
curl.exe -s -w " [%{http_code}]`n" http://localhost:8080/q/health
curl.exe -s -X POST -H "Content-Type: application/json" -d '{\"sku\":\"A-100\",\"quantity\":2,\"type\":\"express\"}' http://localhost:8080/api/orders
curl.exe -s http://localhost:9000/q/metrics | Select-String '^orders_|^http_server_requests_seconds_count'
```

```bash
curl -s http://localhost:9000/q/health
curl -s -w " [%{http_code}]\n" http://localhost:8080/q/health
curl -s -X POST -H "Content-Type: application/json" -d '{"sku":"A-100","quantity":2,"type":"express"}' http://localhost:8080/api/orders
curl -s http://localhost:9000/q/metrics | grep -E '^orders_|^http_server_requests_seconds_count'
```

```text
{ "status": "UP", "checks": [ { "name": "order-worker", "status": "UP", "data": { "silenceMillis": 37 } },
                              { "name": "inventory", "status": "UP" }, { "name": "worker-started", "status": "UP" } ] }
 [404]
orders_created_total{application="orders",type="express"} 1.0
orders_processing_seconds_count{application="orders"} 1.0
orders_queue_size{application="orders"} 0.0
http_server_requests_seconds_count{application="orders",method="POST",outcome="SUCCESS",status="201",uri="/api/orders"} 1.0
```

The jar behaves the same (`.\mvnw.cmd package '-DskipTests'`, then `java -jar target\quarkus-app\quarkus-run.jar`; the startup log says `Management interface listening on http://0.0.0.0:9000`).

## 💼 In real projects
- **Kubernetes mapping.** The three endpoints become the `livenessProbe`, `readinessProbe` and `startupProbe` of the container (the Kubernetes guide generates them when the health extension is present). Point the probes at the management port.
- **A liveness check that touches a dependency is a classic outage multiplier.** The database slows down, liveness fails, every pod restarts, the restarted pods hammer the database while it recovers. Liveness is for "this process is beyond help".
- **Readiness flapping.** A readiness check that goes DOWN on every blip pulls instances out and puts the remaining ones under more load. Keep checks cheap (a cached result or a small timeout), and prefer degrading (lab 23's fallback) to going out of rotation when the application can still answer something useful.
- **Health data is read by machines and sometimes by attackers.** Put no secrets and no internal detail in it; the management interface should be reachable only from the platform (network policy, firewall), not from the internet. In this lab it listens on all interfaces: closing it is a deployment job.
- **Cardinality is the budget.** Before adding a tag, ask: how many different values can it take over a year? `type` has 2; an order id has millions. Per-tenant tags are acceptable only if the number of tenants is small.
- **Name and unit your metrics** with the conventions of your monitoring system. Micrometer names with dots (`orders.created`) turn into Prometheus names (`orders_created_total`, `orders_processing_seconds_count`); dashboards are written against the *exported* names, so renaming a metric breaks them.
- **Measure what users feel, alert on symptoms.** Rate, errors and duration of requests (RED), and saturation of queues and pools. The `http_server_requests_*` family comes for free; add business metrics (orders, payments) on top, and add the metrics of lab 23's breakers and fallbacks to see degradation before users do.
- **Common tags** (`application`, and in a real fleet `environment`, `version`) are what lets one Prometheus serve many services.

## 🚀 Stretch goal
Add a second readiness check for **overload**: DOWN when the queue holds more than N orders. Write the test with the stalled worker. Then decide, and write down, whether an overloaded instance should leave rotation or answer `503` itself, and what each does to the others.

## 💡 Hints
<details><summary>Hint 1 (direction)</summary>

Task 1 is one property. Task 2: three small `@ApplicationScoped` classes that implement `HealthCheck`, each with its qualifier annotation. Task 3: inject the `MeterRegistry`; the counter, the timer and the gauge are each one call. Task 4: a CDI producer method that returns a `MeterFilter`.
</details>

<details><summary>Hint 2 (more concrete)</summary>

`quarkus.management.enabled=true`. `@Liveness` / `@Readiness` / `@Startup` come from `org.eclipse.microprofile.health` (not `io.quarkus.runtime.Startup`). `HealthCheckResponse.builder().name("...").status(boolean).withData("key", number).build()`. `registry.counter("orders.created", "type", type).increment()`. `Timer.Sample sample = Timer.start(registry); ...; sample.stop(registry.timer("orders.processing"))`. `registry.gauge("orders.queue.size", queue, BlockingQueue::size)`. `@Produces @Singleton MeterFilter commonTags() { return MeterFilter.commonTags(Tags.of("application", "orders")); }`.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1. "Removing X" means: in a scratch copy of the solution, remove X, run the suite, restore.

**Task 1.** After enabling the management interface, `/q/health`, `/q/health/live`, `/q/health/ready`, `/q/health/started` and `/q/metrics` answer **404** on the public port and **200** on the management port (9000 in dev and jar, 9001 in the tests), in dev mode and in the jar. The Dev UI stays on the public port in dev (`/q/dev-ui` answers 200). Without the property, all 14 tests that call the operations port fail with `Connection refused`.

**Task 2.**
- With the inventory down: `/q/health/live` answers **200** and `/q/health/ready` answers **503** (`status: DOWN`, the check `inventory` DOWN). `/q/health` (everything) answers 503.
- Making liveness depend on the inventory (`&& inventory.isAvailable()`) makes `/q/health/live` answer **503** as soon as the inventory is down (`withoutTheInventoryTheApplicationIsStillAlive` turns red): a platform would restart a healthy instance for a problem a restart cannot fix.
- A readiness check that ignores the inventory leaves `/q/health/ready` at 200: 2 tests red.
- A stalled worker: after more than one second of silence `/q/health/live` answers **503** with `order-worker` DOWN; when the worker resumes, it answers 200 again.
- A check without the `@Startup` qualifier disappears from `/q/health/started` (the list of checks there is empty).

**Task 3.**
- Names: `orders.created` becomes `orders_created_total{application="orders",type="express"}`; the timer `orders.processing` produces `orders_processing_seconds_count`, `_sum` and `_max`; the gauge is `orders_queue_size`.
- The gauge goes up while the worker is stalled and down to 0 when it resumes.
- An order rejected because the inventory is down (503) is not counted.

**Task 4.**
- Without the `MeterFilter`, none of the 178 samples of the test run had the tag (3 tests red).
- The HTTP server metrics use the **path template**: `uri="/api/orders/{id}"` for `/api/orders/1`, `/api/orders/3`, and also for `/api/orders/abc` (a request that fails the number conversion with 404 is counted under the template). A path that matches nothing is counted as `uri="NOT_FOUND"`: Micrometer does not create a series per raw path.
- Adding the order id as a tag of `orders.created` (`"orderId", String.valueOf(order.id())`) turns 2 tests red: every order becomes its own series, and the counter per type can no longer be read.
</details>

## 🎤 Self-assessment
1. State in one sentence each what liveness, readiness and startup mean, and what a platform does when each one is DOWN.
2. The database is down. Which probe should report it, and what must the others keep saying? What happens to the whole fleet if the wrong one reports it?
3. What is cardinality? Give a tag you would accept and one you would refuse for `orders.created`, with the number of series each creates.
4. `orders.created` in the code and `orders_created_total` in Prometheus. Why does the name change, and why does it matter when a dashboard already exists?
5. Why is the management interface on a different port, and what must a deployment do about that port?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
