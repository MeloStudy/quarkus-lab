# 🧪 Lab 16 - Testing with `@QuarkusTest`

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
> **Docker (or Podman) must be running.** The `@QuarkusTest` classes start a real PostgreSQL in a container (Dev Services), as in labs 08 to 15.

## 🎯 Why it matters
Every other lab gave you tests to make green. In a job nobody does: *you* write the tests, and the hard part is not the assertion, it is the **arrangement**: replacing the remote service that is never reachable from the build server, controlling the clock, getting a database that is the same at the start of every test, and keeping the suite fast enough that people keep running it. A suite that is slow, order-dependent or calls real services gets ignored, then deleted.

## 🧠 Concepts
- **`@QuarkusTest`:** boots the real application once (CDI, REST, database through Dev Services) and runs the tests inside it. The test class instance is a bean too: `@Inject` works.
- **`@InjectMock` / `@InjectSpy`** (Mockito): replace a CDI bean *inside the running application* for the duration of one test. Only for beans with a **normal scope** (`@ApplicationScoped`, `@RequestScoped`); a `@Singleton` or `@Dependent` bean cannot be mocked this way.
- **`QuarkusMock.installMockForType(object, Type.class)`:** the same idea with any object, not only a Mockito mock (here a fixed `java.time.Clock`). Needs no Mockito.
- **`@TestTransaction`:** runs the test inside a transaction that is **rolled back** at the end. Fast cleanup for database tests, with a limit you will meet in task 3.
- **Unit test without Quarkus:** if a class gets its collaborators through the constructor, `new` and Mockito are enough. No container, milliseconds.
- **Test lifecycle:** the application starts once and is shared by every `@QuarkusTest` class, **including its in-memory state and its database**. A different `@TestProfile` means a different application, started again.

| You want to test | Use | Cost |
|---|---|---|
| Pure logic of one class | Plain JUnit + Mockito (`new`) | milliseconds |
| One layer with the real database | `@QuarkusTest` + `@TestTransaction` | shared application start |
| A whole request through HTTP | `@QuarkusTest` + RestAssured | shared application start |
| The same with different configuration | `@TestProfile` | another application start |

## 📚 Documentation
- [Quarkus - Testing your application](https://quarkus.io/guides/getting-started-testing)
- [Quarkus - Dev Services overview](https://quarkus.io/guides/dev-services)
- [Mockito](https://site.mockito.org/)
- [JUnit 5 user guide](https://docs.junit.org/current/user-guide/)
- [REST Assured](https://rest-assured.io/)

## 🧭 Scenario
`starter/` is a small loyalty-points service: `POST /rewards/orders` gives one point per 10 currency units (rounded down, **double on Saturday and Sunday**), unless a remote fraud service flags the order; the customer is notified; balances live in PostgreSQL and a counter lives in memory.

**The production code is finished and correct.** What is missing is the arrangement of the tests: the test classes are written (acts, assertions, `verify(...)` calls) but their mocks, their time control and their cleanup are not installed. Most tests are red; your job is to make each one pass **by fixing the test, not the production code**. There is exactly one exception, which a test will force you to find (task 1).

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

## ✅ Tasks

### Task 1 - Replace what you cannot call
**Goal:** the remote fraud service and the notifier become mocks inside the running application. **Validated by:** `RewardsResourceTest`.

- [ ] Run `RewardsResourceTest` and read what happens to a request when `FraudClient` is the real bean.
- [ ] Make `fraud` and `notifier` mocks that replace the beans (`@InjectMock`). Read the **first** error you get.
- [ ] Fix it. This is the one allowed change in `src/main`.
- [ ] Look at the stubbing and the `verify(...)` calls the tests contain: what do `times(1)`, `never()`, `verifyNoInteractions` and `doThrow(...).when(...)` each prove?

> [!WARNING]
> `@InjectMock` replaces the bean for the **current test**. Something stubbed in one test (`fraudIsSuspiciousForOneCustomerOnly` stubs the fraud service to say "suspicious") must not be visible in the next. Check it: `aMockStartsEveryTestWithoutTheStubbingOfThePreviousOne` is there for that.

🔮 **Predict first:** a test does not stub `fraud.isSuspicious(...)` at all. What does the mock return, and what does the order become?

```powershell
.\mvnw.cmd test "-Dtest=RewardsResourceTest"
```

### Task 2 - Time is a dependency
**Goal:** a test decides what day it is. **Validated by:** `RewardsResourceTest` (the two clock tests).

- [ ] The application asks for a `java.time.Clock` (see `ClockProducer`) instead of calling `Instant.now()`. Install a **fixed** clock for `ordersOnASaturdayGiveDoublePoints` and for `ordersOnAWednesdayGiveNormalPoints`, for that test only, with `QuarkusMock`. It does not need to be a Mockito mock.
- [ ] Check that the next test goes back to the real clock.

🔮 **Predict first:** the weekend test passes on Saturdays and fails the rest of the week if you forget to control the clock. How would you notice that in CI?

### Task 3 - State that outlives a test
**Goal:** each test starts from a known state. **Validated by:** `RewardsResourceTest` (the accumulation and metrics tests) and `AccountRepositoryTest`.

- [ ] With task 1 and 2 done, run `RewardsResourceTest` again and read the failures. Where do the numbers come from?
- [ ] Make `cleanTheSharedState()` run **before and after** every test.
- [ ] In `AccountRepositoryTest`, make every test run in a transaction that is rolled back. Then read the last test: what does it prove about *who* can see the data?

> [!WARNING]
> Three things live as long as the application and not as long as a test: the database, the in-memory `RewardsMetrics`, and any static field. A suite that passes alone and fails when run with other classes (or in another order) has a leak of this kind.

🔮 **Predict first:** `@TestTransaction` rolls back, so is it enough to guarantee the database is empty at the start of every test class? What about a test that goes through HTTP?

```powershell
.\mvnw.cmd test "-Dtest=RewardsResourceTest,AccountRepositoryTest"
```

### Task 4 - The fast test: no Quarkus at all
**Goal:** test the business rules in milliseconds. **Validated by:** `RewardsServiceTest`.

- [ ] Create the three collaborators as plain Mockito mocks in `setUp()`, and make a customer without an account the default.
- [ ] Compare the time of this class with the time of the others (Surefire prints it in `target/surefire-reports`).
- [ ] Why can this test build `RewardsService` with `new`? What would have to change in the production class to make it impossible?

```powershell
.\mvnw.cmd test "-Dtest=RewardsServiceTest"
```

### Task 5 - What a test run costs (no test validates this)
**Goal:** see the lifecycle with your own numbers.

- [ ] Run the whole suite and count how many times the application starts (lines containing `powered by Quarkus`), and the total time.
- [ ] Add a new test class annotated `@TestProfile(...)` with a profile that overrides any configuration property (a `QuarkusTestProfile` implementation). Run the whole suite again and count again.

```powershell
.\mvnw.cmd clean test *> test.log
(Select-String -Path test.log -Pattern "powered by Quarkus").Count
```

```bash
./mvnw clean test > test.log
grep -c "powered by Quarkus" test.log
```

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green.

## 👀 What you should see
The fast test and the slow ones, in `target/surefire-reports` (times from the reference solution, on this machine; yours will differ but not the order of magnitude):

```text
RewardsServiceTest        8 tests    0.26 s     (no application)
RewardsResourceTest      11 tests    1.9  s     (application already running)
AccountRepositoryTest     7 tests    6.0  s     (the first class: includes starting the application and PostgreSQL)
```

## 💼 In real projects
- **Test pyramid:** many plain unit tests for the rules, fewer `@QuarkusTest` for the layers and the HTTP contract, very few end-to-end. The unit test of this lab covers eight rules in a quarter of a second; the same rules through HTTP would cost a request each and need a database.
- **Constructor injection is a testability decision.** It is also why the service in this lab can be tested with `new`. A class with `@Inject` private fields forces every test to boot the container.
- **Mock only the boundary** (a remote service, an e-mail server, the clock). Mocking your own repository and your own service in the same test proves that the mocks agree with each other, not that the application works. Prefer the real database through Dev Services for persistence code.
- **Verify behavior that matters, not every call.** A test full of `verify(...)` on internal calls breaks at every refactoring without catching a bug. Verify the side effects the business cares about (the customer was notified, the fraud service was not consulted for an invalid order).
- **Never let a test depend on the real clock, the real network or the order of execution.** Each of those is a flaky test waiting for a bad day. Inject the clock; mock the network; clean the state.
- **Cleanup before and after.** A test that cleans only after leaves a mess when it crashes in the middle; one that cleans only before leaves its mess to the next class. Doing both is cheap.
- **Speed is a feature.** Keep one application per run when possible: every extra `@TestProfile` (or `@QuarkusTestResource` with different resources, lab 17) is another start. Group tests that need the same configuration.
- **Test the failure paths** (service down, notification failing, invalid input) as carefully as the happy path. They are the ones production exercises at 3 a.m.
- **CI:** the build agent needs Docker for Dev Services, or you configure a fixed database for the pipeline. Make that a deliberate decision, not a surprise on the first run.

## 🚀 Stretch goal
Add a test with `@InjectSpy` on `RewardsService` that proves the endpoint calls `award` exactly once with the customer and the amount of the request, without replacing its behavior. Then explain when a spy is better than a mock and when it is worse.

## 💡 Hints
<details><summary>Hint 1 (task 1)</summary>

The annotation is `io.quarkus.test.InjectMock`, on the field of the test class. The first error you get is about the **scope** of the bean you are mocking. Look at the annotation on `Notifier`, and at the scope of `FraudClient`, which works.
</details>

<details><summary>Hint 2 (task 2)</summary>

`QuarkusMock.installMockForType(Clock.fixed(Instant.parse("..."), ZoneOffset.UTC), Clock.class)` inside the test method. `2026-11-07` is a Saturday, `2026-11-04` a Wednesday.
</details>

<details><summary>Hint 3 (task 3)</summary>

`@BeforeEach` and `@AfterEach` can be on the same method. Deleting rows needs a transaction: from a test method outside `@TestTransaction` use `QuarkusTransaction.requiringNew().run(...)`. The annotation for the rollback is `io.quarkus.test.TestTransaction`.
</details>

<details><summary>Hint 4 (task 4)</summary>

`Mockito.mock(SomeClass.class)` works on classes, not only on interfaces. For the "no account yet" default, stub `findByCustomer(anyString())` to return `Optional.empty()`.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1 (`quarkus-junit-mockito` for Mockito, PostgreSQL 18 from Dev Services).

**Task 1.**
- With the real `FraudClient`, every order fails: the service is never reachable, and the application answers `503`.
- `@InjectMock` on `Notifier` while it is `@Singleton` fails when the test instance is created, for every test of the class: `Invalid use of io.quarkus.test.InjectMock - the injected bean does not declare a CDI normal scope but: jakarta.inject.Singleton. Offending field is notifier of test class ...`. Changing `Notifier` to `@ApplicationScoped` fixes it (the production change of this lab). `@Singleton` and `@Dependent` beans cannot be mocked with `@InjectMock` or `QuarkusMock`.
- An unstubbed `Mockito` mock of `isSuspicious` returns `false`, so the order is accepted (`201`). The mock is installed fresh for each test: after a test that stubbed "suspicious" for every customer, the next test got `201`.

**Task 2.**
- With task 1 done and no clock control, `ordersOnASaturdayGiveDoublePoints` fails with `JSON path points doesn't match. Expected: <24> Actual: <12>` (the day the reference runs were made was a Monday). Installing the fixed clock in the test makes it pass, and the next test goes back to the real clock.

**Task 3.**
- With tasks 1 and 2 done but no cleanup, five tests of `RewardsResourceTest` fail, with numbers that betray the leak: `balance` is `29` instead of `17`, the metrics are `7` and `10` instead of `1` and `2`, and the "fraud service is down" test finds the account of a previous test (`404` expected, `200` received).
- Without `@TestTransaction`, every test of `AccountRepositoryTest` that writes fails with `Transaction is not active, consider adding @Transactional to your method to automatically activate one.`
- If the tests did commit, the second test that creates the customer `shared` would fail with `duplicate key value violates unique constraint "account_customerid_key"` (SQLSTATE 23505).
- Inside `@TestTransaction` the test sees its own uncommitted row (`count()` is 1), while an HTTP call to the application for that customer answers `404`: the server works in a different transaction and cannot see it. That is why `RewardsResourceTest` cleans with committed transactions and not with `@TestTransaction`. After the test, the row is gone.
- `@TestTransaction` does not clean what *other* classes committed. `RewardsResourceTest` cleans after itself (`@AfterEach`) so that it does not leave committed rows to the classes that run after it.

**Task 4.**
- `Mockito.mock(AccountRepository.class)` works because mocking classes is supported; the `persist(...)` call can be captured with an `ArgumentCaptor<Account>`.
- The eight unit tests run in about `0.26 s`. The same suite of rules through HTTP needs the whole application.
- The service can be built with `new` because its constructor takes every collaborator. It would not be possible with private `@Inject` fields and no constructor.

**Task 5.**
- Whole suite of the solution (3 classes): the application starts **once** (`powered by Quarkus` appears once), total test time about `8.5 s`. Adding one extra class with its own `@TestProfile` makes it start **twice** and the total goes to about `12.4 s`: a second start costs around four seconds here.
</details>

## 🎤 Self-assessment
1. What does `@InjectMock` do exactly, and why can it not mock a `@Singleton` bean?
2. A test passes alone and fails when the whole suite runs. List three causes in a `@QuarkusTest` project and how you find which one it is.
3. What is the difference between `@TestTransaction` and cleaning data with a committed transaction? When does the first stop being enough?
4. How do you test code that depends on the current date? What do you change in the production code first?
5. When is a plain Mockito unit test better than a `@QuarkusTest`, and when is it dangerous?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
