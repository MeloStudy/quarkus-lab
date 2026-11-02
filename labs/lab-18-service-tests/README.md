# 🔗 Lab 18 - Integration tests between services

| ⏱️ Duration | 🚀 Quarkus | ☕ Java | 🧪 Type |
|---|---|---|---|
| 2 h | 3.40.1.1 LTS | 21 | A (starter + solution) |

## 📍 Where to run commands
Open a terminal **inside this lab's `starter/` folder** (it contains two Maven modules, `inventory` and `orders`, and the `contracts` folder) and check where you are before running anything:

```powershell
Get-Location
```

```bash
pwd
```

> [!NOTE]
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead. `-pl orders` runs one module.

> [!NOTE]
> No Docker needed: neither service has a database.

## 🎯 Why it matters
Two services that pass all their own tests can still fail together: the provider renames a field, the consumer keeps reading the old one, and nobody notices until production. Testing each service against a hand-written fake only moves the problem: the fake says what the author *believes* the other service does. A **contract** written down in one place, used to generate the fakes on the consumer side and replayed against the real thing on the provider side, closes the gap without needing both services running at once.

## 🧠 Concepts
- **Consumer and provider:** `orders` (consumer) calls `inventory` (provider). The consumer decides what it needs from the provider; the provider must not break it.
- **WireMock:** a real HTTP server that you program from the test: what it answers to which request, with what delay or network fault, and what it received. The application under test talks to it over real HTTP, so timeouts, retries and parsing are really exercised, which a Mockito mock of the client cannot do.
- **Contract (consumer-driven):** a file that lists the interactions the consumer relies on (request + expected response). Here it is plain JSON in `contracts/`, outside both services, because it belongs to neither. Tools such as Pact formalize the same idea.
- **Consumer side:** WireMock stubs are *generated from* the contract, so consumer tests run against what the provider promised.
- **Provider side:** every interaction is replayed against the real provider, after putting it in the **state** the interaction assumes ("given product A1 has 5 units"). If the provider breaks a promise, its own build fails.
- **`@QuarkusIntegrationTest`:** runs the **packaged** application (`java -jar`) in another process and tests it as a black box. It catches what `@QuarkusTest` cannot: packaging, class loading, configuration of the real artifact. The price: no `@Inject`, no `@InjectMock`, no `QuarkusMock`.

| Test | Application | Dependencies | Catches |
|---|---|---|---|
| Unit test | none | mocks | logic |
| `@QuarkusTest` + WireMock | started inside the test JVM | real HTTP to a fake | wiring, HTTP behavior, timeouts, error mapping |
| Provider contract test | provider in the test JVM | the contract file | provider breaking a consumer |
| `@QuarkusIntegrationTest` | the packaged jar, another process | WireMock via test resource | packaging and runtime problems |

## 📚 Documentation
- [WireMock - stubbing](https://wiremock.org/docs/stubbing/), [verifying](https://wiremock.org/docs/verifying/) and [simulating faults](https://wiremock.org/docs/simulating-faults/)
- [Quarkus - Testing your application](https://quarkus.io/guides/getting-started-testing) (`@QuarkusIntegrationTest`, test resources)
- [Martin Fowler - Consumer-driven contracts](https://martinfowler.com/articles/consumerDrivenContracts.html)
- [Pact](https://docs.pact.io/) (the standard tool for the same idea)

> [!NOTE]
> **WireMock is not managed by the Quarkus BOM** (checked in `quarkus-bom-3.40.1.1.pom`). Justification: it is the standard tool to fake HTTP services and it only touches the test classpath. `orders/pom.xml` pins `wiremock-standalone` **3.13.2** in the property `wiremock.version` (the latest stable 3.x when this lab was written; 4.0 was still in beta). The "standalone" artifact bundles and relocates its own dependencies, so it cannot clash with the ones of the application.

## 🧭 Scenario
Two services, each a Quarkus module:
- `inventory` (provider): `GET /stock/{sku}` and `POST /stock/{sku}/reserve`.
- `orders` (consumer): `POST /orders` asks the inventory how much stock there is, then reserves it. Every way the inventory can fail is translated into one status: unknown product `422`, not enough stock `409`, broken or garbage answer `502`, too slow `504`.

`contracts/stock-contract.json` lists the six interactions that `orders` relies on. The production code of both services is finished. As in labs 16 and 17, **the test infrastructure is missing**: the WireMock lifecycle manager, the contract-to-stub converter, the failure stubs, the comparison rule of the provider verification and the provider state. Everything starts red.

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

## ✅ Tasks

### Task 1 - A fake inventory over real HTTP
**Goal:** a WireMock server that exists for the tests of `orders`. **Validated by:** `OrderFlowTest` (all of it needs it).

- [ ] Implement `WireMockResource` (in `orders/src/test`): start a `WireMockServer` on a **free port** before the application, give the application its URL as the property `inventory.url`, and stop it afterwards.
- [ ] Make it hand the running server to the test class (the field annotated `@InjectWireMock`).
- [ ] Run `OrderFlowTest` with only this done and read **what status** the unstubbed tests get.

🔮 **Predict first:** the application calls `GET /stock/A1` on a WireMock that has no stub for it. What does WireMock answer, and therefore what does the orders service answer to its own client?

```powershell
.\mvnw.cmd test -pl orders "-Dtest=OrderFlowTest"
```

### Task 2 - Stubs generated from the contract
**Goal:** the consumer is tested against what the provider promised. **Validated by:** `OrderFlowTest` (the first five tests).

- [ ] Implement `ContractStubs.stub(WireMockServer, Interaction)`: a stub for the interaction's method and path, matching the request **body as JSON** when there is one, answering the contract's status and body with a JSON content type.
- [ ] Look at how the tests then use WireMock to `verify(...)` what the application sent: `times`, `0` times, and the body of the reserve call.

### Task 3 - What a contract cannot say: other people's bad days
**Goal:** stub the failures by hand. **Validated by:** `OrderFlowTest` (the rest).

- [ ] A reserve that answers `409` after the stock check said it was fine (someone else was faster).
- [ ] A `500`; an HTML maintenance page with status `200`; a `200` JSON without the field `available`; a connection reset (WireMock can simulate network faults); a correct answer that takes 3 seconds.
- [ ] Each test also checks that **no order was created** and, for the slow one, that the application did **not wait** for the three seconds.

> [!WARNING]
> The slow test depends on a timeout configured in the orders service (`inventory.timeout=1s`). If the application had no timeout, the test would still get its `504`... after waiting. Which assertion of the test catches that?

### Task 4 - The provider keeps its promises
**Goal:** the inventory service replays the contract against itself. **Validated by:** `ContractVerifierTest`, `ProviderContractTest` (module `inventory`).

- [ ] Implement `ContractVerifier.compatible(expected, actual)` following the rule in its comment. `ContractVerifierTest` is a plain unit test of the rule: a verifier that says "yes" to everything makes every contract test green, so the verifier itself needs tests.
- [ ] Run `ProviderContractTest` and read the failure. The provider is shared by all interactions and some of them change it: put it in the **state** each interaction asks for before sending the request.

🔮 **Predict first:** the provider renames the JSON field `available` to `stock` (a harmless refactoring in the provider's eyes). Which module's tests fail, `inventory` or `orders`? Try it in a scratch copy.

```powershell
.\mvnw.cmd test -pl inventory
```

### Task 5 - The packaged application
**Goal:** test the artifact, not the classes. **Validated by:** `OrdersIT`.

- [ ] Finish `OrdersIT` (one stub is missing). It is a `@QuarkusIntegrationTest`: read its class comment, and notice that it uses the same `WireMockResource`.
- [ ] Integration tests run in the `verify` phase of Maven, after the jar exists:

```powershell
.\mvnw.cmd verify -pl orders "-DskipITs=false"
```

```bash
./mvnw verify -pl orders -DskipITs=false
```

- [ ] Try to add `@Inject InventoryClient client;` (any bean of the application) to `OrdersIT`, and use it. What happens?

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green (both modules, unit and integration tests).

## 👀 What you should see
The integration test starts the packaged application as a separate process:

```text
[INFO] --- failsafe:3.5.6:integration-test (default) @ orders ---
INFO  [io.quarkus] (main) orders 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.40.1.1) started in 0.931s. Listening on: http://0.0.0.0:8081
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in lab.orders.OrdersIT
```

## 💼 In real projects
- **A fake you wrote by hand is a guess.** Use a contract (or the provider's OpenAPI document, or recorded real traffic) as the single source of the fake's behavior, and verify the provider against it in the provider's pipeline.
- **Consumer-driven means the consumer lists only what it uses.** The provider stays free to add fields and endpoints; breaking a listed field fails *its* build, with the name of the consumer that depended on it. That is a conversation starter, not a blocker: sometimes the right answer is to change the contract with the consumer's agreement.
- **Where does the contract live?** In a shared repository, or published by the consumer's build and fetched by the provider's (Pact Broker does exactly this). The versions of both sides must be recorded so you can ask "can I deploy this version of the provider?".
- **Test the unhappy network.** Timeouts, resets, garbage and slow answers are what production does on bad days. WireMock makes them cheap to simulate; mocking the client class hides them.
- **WireMock answers `404` to anything it has no stub for.** If your code treats `404` as a meaningful answer (as `orders` does: unknown product), a missing stub looks like a business case. Make tests prove the stub was used (`verify(...)`), or configure unmatched requests to fail loudly.
- **Reset between tests** (`resetAll()` clears stubs and the request journal) or tests depend on their order.
- **`@QuarkusIntegrationTest` is slower and blinder than `@QuarkusTest`.** Keep a few of them as smoke tests of the artifact (and of the container image, with the right extensions), not as the main suite. In native builds it is the only way to test the native executable.
- **End-to-end with both services running** is a separate, small suite (deployed environment), not a replacement for contracts: it is slow, flaky and cannot say which side is wrong.
- **Timeouts are the consumer's responsibility.** Fault tolerance (retry, circuit breaker) is lab 23; the test pattern here (WireMock with delay and faults) is how you will test it.

## 🚀 Stretch goal
Add to the contract an interaction for `reserve` with a quantity of `0` (the provider answers `400` with `INVALID_QUANTITY`) and make the consumer reject that case before calling the provider, with a WireMock `verify(0, ...)`. Then change the provider to answer `422` instead and see which test notices. No hints.

## 💡 Hints
<details><summary>Hint 1 (task 1)</summary>

`new WireMockServer(WireMockConfiguration.options().dynamicPort())`, then `start()`, and `baseUrl()` gives the address. For the injection, `TestInjector#injectIntoFields(Object, Predicate<Field>)` with the predicate `new TestInjector.AnnotatedAndMatchesType(InjectWireMock.class, WireMockServer.class)`.
</details>

<details><summary>Hint 2 (task 2)</summary>

`WireMock.request(method, urlEqualTo(path))` builds a stub for any method; `.withRequestBody(equalToJson(...))`; `.willReturn(aResponse().withStatus(...).withHeader(...).withBody(...))`; `server.stubFor(...)`.
</details>

<details><summary>Hint 3 (task 3)</summary>

`aResponse().withFixedDelay(ms)` and `aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)`. For a stub that answers a `POST`, use `post(urlEqualTo(...))`.
</details>

<details><summary>Hint 4 (task 4)</summary>

Jackson's `JsonNode` has `isObject()`, `isArray()`, `fields()`, `has(name)`, `get(name)`, `size()`, `getNodeType()`. The state of an interaction is in `interaction.given()`; the repository has `reset()` and `set(sku, units)`.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1 with WireMock 3.13.2.

**Task 1.**
- WireMock answers `404` to a request without a stub. The orders service reads `404` from the inventory as "unknown product" and answers `422`. With only the resource implemented, the failure tests report `Expected status code <502> but was <422>` (and the slow test `<504> but was <422>`): a missing stub looks like a business case.

**Task 2.**
- With the contract-stubs done, the five tests that use the contract pass, and what is left red are the failure tests: the race (`Expected status code <409> but was <422>`), the `500`, the HTML page, the missing field, the connection reset and the slow answer.

**Task 3.**
- A `200` whose body is valid JSON without `available`, an HTML page, a `500` and a connection reset all end as `502`; the slow answer ends as `504` after the 1-second timeout, not after three. The test measures the elapsed time (`< 2500 ms`): a missing timeout would still produce a status, but only after waiting for the full delay.

**Task 4.**
- With the verifier done and no provider state, one interaction fails: `reserve more than the available stock: expected {"error":"INSUFFICIENT_STOCK","available":5} but the provider answered {...}`. An earlier interaction (`reserve within the available stock`) took two units of A1, so the provider was no longer in the state the contract assumes. Resetting and applying `given` before each interaction fixes it.
- Renaming the provider's field `available` to `stock`: `ProviderContractTest` fails for the two interactions that read stock (`expected {"sku":"A1","available":5} but the provider answered {"sku":"A1","stock":5}`), while all 12 consumer tests stay green, because the consumer's stubs come from the contract and not from the real provider. Only the provider-side verification can see this change; this is why both halves are needed.

**Task 5.**
- `mvn verify -DskipITs=false` builds the jar and runs `OrdersIT` against it in the failsafe plugin (`integration-test` phase). If the unit tests of the module fail first, the integration tests are not even started.
- Adding `@Inject InventoryClient client;` to `OrdersIT` fails before any test runs: `JUnit @Inject is not supported in @QuarkusIntegrationTest tests. Offending field is lab.orders.OrdersIT.client`. The application runs in another process, so the WireMock test resource is how the test reaches the outside world and configures the application. (The application also starts in a different way: the log of the integration run shows `Listening on: http://0.0.0.0:8081`, the packaged jar, while the `@QuarkusTest` run shows `http://localhost:8081`.)
</details>

## 🎤 Self-assessment
1. What does a contract test give you that a WireMock-based test written by the consumer alone does not? Which side fails when the provider renames a field?
2. Why does the provider verification need a "state", and what goes wrong without it?
3. WireMock answers `404` to unstubbed requests. Why is that dangerous for a client that interprets `404`? How do you protect your tests?
4. What can `@QuarkusIntegrationTest` find that `@QuarkusTest` cannot, and what can't you do in it?
5. The provider wants to add a new required field to a response. Is that compatible? And removing one? How does the contract decide?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
