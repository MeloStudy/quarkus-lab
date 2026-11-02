# 🧰 Lab 17 - Dev Services and test profiles

| ⏱️ Duration | 🚀 Quarkus | ☕ Java | 🧪 Type |
|---|---|---|---|
| 2 h | 3.40.1.1 LTS | 21 | A (starter + solution) |

## 📍 Where to run commands
Open a terminal **inside this lab's `starter/` folder** and check where you are before running anything:

```powershell
Get-Location
```

```bash
pwd
```

> [!NOTE]
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead.

> [!IMPORTANT]
> **Docker (or Podman) must be running.** Dev Services starts a real PostgreSQL in a container for the tests.

## 🎯 Why it matters
Lab 16 used the infrastructure the framework gave for free. Real projects need to *shape* it: the test database must be the **same engine and major version as production** (not a different default, not H2), it must have the data the tests rely on, some collaborators are not containers at all (a remote HTTP service), and a few tests need the application configured differently. Each of those is easy to set up, and each has a cost in start-up time or a trap that makes the suite pass on your machine and fail in CI.

## 🧠 Concepts
- **Dev Services:** when a datasource has no URL configured, Quarkus starts a throwaway container in dev mode and tests and wires the connection. Its behavior is configuration: `quarkus.datasource.devservices.*` (image, initialization scripts, credentials, port, reuse).
- **Initialization script:** SQL from the classpath run against the Dev Services database at start. Not a replacement for migrations (lab 11); it is for data or objects that only exist in dev and test.
- **`QuarkusTestResourceLifecycleManager`:** a class with `start()` and `stop()` that runs **outside** the application, **before** it starts. What `start()` returns (a map of configuration properties) is merged into the application's configuration. This is how a test starts something that is not a Dev Service (a fake server, a container you build yourself) and tells the application where it is.
- **`@WithTestResource`:** attaches a lifecycle manager to a test class. Its `scope` decides who shares it: `MATCHING_RESOURCES` (default: tests with the same resources run together, other groups get a restart), `RESTRICTED_TO_CLASS`, or `GLOBAL`.
- **`@TestProfile` + `QuarkusTestProfile`:** a test class that needs different configuration (or a different set of resources) names a profile class. Everything that changes the application, changes it **for a new application start**.

| Tool | Changes | Applies to | Cost |
|---|---|---|---|
| `application.properties` | configuration for everyone | all tests | none |
| `@WithTestResource` (matching) | adds something started outside the app | the classes that declare it | a restart for each distinct group |
| `@WithTestResource(scope = GLOBAL)` | the same, for the whole run | all tests | one start for everybody |
| `@TestProfile` | configuration (and resources) | the classes that name that profile class | a restart per profile class |

## 📚 Documentation
- [Quarkus - Dev Services for databases](https://quarkus.io/guides/databases-dev-services)
- [Quarkus - Dev Services overview](https://quarkus.io/guides/dev-services)
- [Quarkus - Testing your application](https://quarkus.io/guides/getting-started-testing) (test resources, profiles)
- [Quarkus - Datasources](https://quarkus.io/guides/datasource) (reference of `quarkus.datasource.devservices.*`)
- [Testcontainers - container reuse](https://java.testcontainers.org/features/reuse/)

## 🧭 Scenario
`starter/` is a currency converter: `GET /convert?amount=100&from=USD&to=EUR` asks a **remote** service for the rate (`rates.url`), reads the **fee** of the pair from a table in PostgreSQL that only the Dev Services script creates, and stores the conversion in a history. With `app.strict=true` a pair with no fee entry is refused with `422` before the remote service is even asked.

As in lab 16, the production code is finished and the test classes are written. What is missing is the **infrastructure** of the tests: the Dev Services configuration, the lifecycle manager that starts the fake remote service, and the test profile. Everything is red at the start.

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

## ✅ Tasks

### Task 1 - The database of the tests is the database of production
**Goal:** Dev Services starts the right engine version and seeds the fee table. **Validated by:** `FeeTableTest`.

- [ ] Production runs PostgreSQL **17**. Run `FeeTableTest` and read what the test database is today.
- [ ] Configure Dev Services to use PostgreSQL 17 (`postgres:17.11`) and to run `src/main/resources/dev-services/fees.sql` when the database starts.
- [ ] Open `fees.sql`. Why is the fee table not an entity, and why does Hibernate not erase it when it creates the schema?

🔮 **Predict first:** you change nothing and run the tests. Which PostgreSQL version do they use, and what do the fee tests say?

```powershell
.\mvnw.cmd test "-Dtest=FeeTableTest"
```

### Task 2 - A collaborator that is not a container
**Goal:** a fake remote service that exists only during the tests. **Validated by:** `ConversionTest`.

- [ ] `rates.url` points to a port nobody listens on, so every conversion answers `503`. Read `ConversionTest` and `FakeRates` (a small server built on the JDK, ready to use).
- [ ] Implement `FakeRatesResource`: start the fake server **before** the application and hand its URL to the application as the property `rates.url`; stop it afterwards.
- [ ] Decide which of the two things, `application.properties` or the value returned by `start()`, wins, and check it.

🔮 **Predict first:** the lifecycle manager runs inside the application's JVM, but not inside the application. Can it use `@Inject`? Can it read `application.properties`?

```powershell
.\mvnw.cmd test "-Dtest=ConversionTest"
```

### Task 3 - A test profile
**Goal:** strict mode in tests, without changing the default application. **Validated by:** `StrictModeTest`, `StrictModeHistoryTest`.

- [ ] Make `StrictProfile` turn `app.strict` on.
- [ ] Run the strict tests. One of them still fails. Read the status code and think about **which** application the strict tests run in.
- [ ] Fix it from the profile, not from the test classes.

> [!WARNING]
> A resource declared with `@WithTestResource` on `ConversionTest` belongs to the tests that declare it. A test profile is a different application and does not inherit it.

🔮 **Predict first:** how many times will the whole suite start the application? Write the number down; you check it in task 4.

```powershell
.\mvnw.cmd test "-Dtest=StrictModeTest,StrictModeHistoryTest"
```

### Task 4 - What the run costs (no test validates this)
**Goal:** count the application starts and understand each one.

- [ ] Run the whole suite and count the application starts (lines with `powered by Quarkus`):

```powershell
.\mvnw.cmd clean test *> test.log
(Select-String -Path test.log -Pattern "powered by Quarkus").Count
```

```bash
./mvnw clean test > test.log
grep -c "powered by Quarkus" test.log
```

- [ ] Explain every start: which test classes ran in it and why they could not share another one.
- [ ] `StrictModeHistoryTest` uses `@TestProfile(StrictProfile.class)`, the same profile class as `StrictModeTest`. Change it to use a **different profile class** that extends `StrictProfile` and adds nothing. Count again.

### Task 5 - Fewer starts
**Goal:** the same tests, one application start less. No test validates this; the suite must stay green.

- [ ] Change the scope of the resource of `ConversionTest` so that every test shares it, and count again. What changed for `FeeTableTest`, and what did not change for the strict profile?
- [ ] Think about what you lose: what would break if a test needed a fake server that behaves differently from the others?

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green.

## 👀 What you should see
Task 4 in the finished solution:

```text
application starts: 3
  1. FeeTableTest                       default application (no resources)
  2. ConversionTest                     application with the fake rates server
  3. StrictModeTest + StrictModeHistoryTest   application of the strict profile (config + the fake server)
```

## 💼 In real projects
- **Same engine, same major version as production.** A test database on H2, or on a "latest" tag that moved last month, is how a query that works on the build agent fails in production. Pin the image and review it when you upgrade the real database.
- **Seed data belongs to the test's world, not to the production schema.** Keep it in files that only the tests/dev profile load. For schema changes the tests should run the real migrations (lab 11): then the migration is tested too.
- **Container reuse speeds up the local loop** and hides state. It needs `testcontainers.reuse.enable=true` in `~/.testcontainers.properties` (a file in your home, not in the repository), and the container keeps its data between runs. In this lab's logs, without that setting, you see `Reuse was requested but the environment does not support the reuse of containers`. Never rely on reuse in CI.
- **CI needs a container runtime.** Dev Services needs Docker/Podman on the build agent; "Docker-in-Docker" and rootless runners are a frequent first-day problem. Decide it before the first pull request.
- **Each profile is an application start.** Four profiles of three seconds are ten seconds of every build, forever. Prefer the default application and mock inside the test (lab 16), and use a profile only when configuration really must differ at start-up (a switch read once, a different set of beans, a different port).
- **Share one profile class between the classes that need the same configuration.** The profile is identified by its class, not by the properties it contains.
- **Lifecycle managers are for things outside the application.** A fake server, a container you start yourself, a temp directory. For things inside it (a bean, a clock), use mocks or `QuarkusMock`.
- **Random ports and no fixed names.** `HttpServer.create(.., 0)` takes a free port; a fixed port makes two builds on the same agent fail at random.
- **Do not put production secrets in test resources.** Use dummy values; real credentials never reach a test run.

## 🚀 Stretch goal
Make the fake rates server controllable from a test: the lifecycle manager can hand its instance to the test class (look at the `inject` methods of `QuarkusTestResourceLifecycleManager`), so a test can count the requests it received and prove that strict mode refuses a pair **without** calling the remote service. No hints.

## 💡 Hints
<details><summary>Hint 1 (task 1)</summary>

`quarkus.datasource.devservices.image-name` and `quarkus.datasource.devservices.init-script-path` (a classpath path, without a leading slash).
</details>

<details><summary>Hint 2 (task 2)</summary>

`FakeRates.start()` gives you the server and `url()` gives the address. `start()` of the manager returns `Map.of("rates.url", ...)`.
</details>

<details><summary>Hint 3 (task 3)</summary>

`QuarkusTestProfile` has default methods for configuration overrides and for test resources: `getConfigOverrides()` and `testResources()` (which returns `TestResourceEntry` objects wrapping a lifecycle manager class).
</details>

<details><summary>Hint 4 (task 5)</summary>

`@WithTestResource` has an attribute `scope`; look at the values of `TestResourceScope`.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1.

**Task 1.**
- Without configuration, Dev Services uses PostgreSQL **18**: `server_version_num = 180006`. The test for version 17 fails with exactly that message.
- Without the init script, the fee tests fail with `ERROR: relation "exchange_fee" does not exist`. The table is created by the script, not by Hibernate, so Hibernate's `drop-and-create` of the entities leaves it alone.
- With task 1 done, every conversion test still answers `503`: the converter cannot reach the rates service.

**Task 2.**
- With a `start()` that returns an empty map, every conversion is `503` (`rates.url` keeps the unreachable default). Returning `rates.url` makes the whole `ConversionTest` pass: the map returned by `start()` overrides `application.properties`.
- A lifecycle manager is created and started by Quarkus before the application exists, so it has no CDI container to inject from.

**Task 3.**
- With only `getConfigOverrides()` in the profile, `thisApplicationRunsInStrictMode` and the `422` test pass, and two tests still fail with `Expected status code <200> but was <503>`: the strict application has no fake server, because the resource was declared on `ConversionTest`. Adding `testResources()` with `new TestResourceEntry(FakeRatesResource.class)` to the profile fixes it.

**Task 4.**
- The solution suite starts the application **3 times**: (1) `FeeTableTest`, no resources; (2) `ConversionTest`, the group of tests with `FakeRatesResource`; (3) the two strict classes, sharing one application because they name the same profile class.
- If `StrictModeHistoryTest` names another profile class (even one that extends `StrictProfile` and adds nothing), the suite starts **4 times** and the same tests pass: the profile is identified by its class, not by its content.

**Task 5.**
- With `@WithTestResource(value = FakeRatesResource.class, scope = TestResourceScope.GLOBAL)` on `ConversionTest`, the suite starts **2 times** and all 14 tests pass: the default application and the conversion tests now share the one that has the fake server. The strict profile still gets its own start because its configuration differs.
- Without any container-reuse setting, the Testcontainers log says `Reuse was requested but the environment does not support the reuse of containers`.
</details>

## 🎤 Self-assessment
1. Why should the test database be the same engine and major version as production? What exactly does Dev Services let you configure about it?
2. What is the difference between an initialization script of Dev Services and a Flyway migration? When would you use each?
3. When does a `QuarkusTestResourceLifecycleManager` run relative to the application, and how does it pass information to it?
4. Three test classes, each with a different `@TestProfile`, run in one build. How many applications start? What would you do to reduce it?
5. A teammate adds `@TestProfile` to a class only to change one property that a test reads once. What are the alternatives and when is the profile justified?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
