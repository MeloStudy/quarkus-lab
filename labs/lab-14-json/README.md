# 🧾 Lab 14 - JSON in depth

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

> [!NOTE]
> No Docker needed: no database in this lab, reservations live in memory.

## 🎯 Why it matters
The JSON an API accepts and returns is its **contract**, and contracts outlive code. A client written two years ago still sends `"suite"` and still parses `"2026-12-01"`. Jackson's defaults are generous (they accept almost anything and guess the rest), which is convenient on day one and expensive on day 500: a misspelled field silently becomes `null`, a date turns into an array of numbers, a renamed Java enum breaks every client. This lab makes the contract explicit, strict and independent of the Java names.

## 🧠 Concepts
- **Quarkus changes two Jackson defaults:** unknown properties are **ignored** (plain Jackson fails) and dates are written as **ISO-8601 text** (plain Jackson writes numbers). Both are configuration (`quarkus.jackson.*`).
- **Property naming:** one global strategy (`quarkus.jackson.property-naming-strategy=SNAKE_CASE`) or per class (`@JsonNaming`). Java keeps `camelCase`; the wire format is a decision of the API.
- **Dates and times:** `LocalDate` is a calendar day (no zone), `Instant` is a point in time (UTC). `LocalDateTime` is neither a point in time nor a day: avoid it in an API.
- **Enums:** the wire value is part of the contract. `@JsonValue` decouples it from the constant's name.
- **Polymorphism:** `@JsonTypeInfo` + `@JsonSubTypes` make a discriminator property choose the Java type, in both directions. Use `Id.NAME` with an explicit list of subtypes.
- **Custom serializers:** teach Jackson a type with its own text format. Register them in the `ObjectMapper` through a CDI bean implementing `io.quarkus.jackson.ObjectMapperCustomizer`, so the type itself stays free of Jackson annotations.

| Default of plain Jackson | Default in Quarkus | Setting |
|---|---|---|
| Fail on unknown properties | **Ignore** them | `quarkus.jackson.fail-on-unknown-properties` |
| Dates as numbers | **ISO-8601 text** | `quarkus.jackson.write-dates-as-timestamps` |
| Enum by constant name | same | `@JsonValue` / `quarkus.jackson.accept-case-insensitive-enums` |
| Names as written in Java | same | `quarkus.jackson.property-naming-strategy` |

## 📚 Documentation
- [Quarkus - Writing JSON REST services](https://quarkus.io/guides/rest-json)
- [Quarkus - All configuration options](https://quarkus.io/guides/all-config) (section Jackson)
- [Jackson annotations](https://github.com/FasterXML/jackson-annotations/wiki/Jackson-Annotations)
- [Jackson - polymorphic deserialization](https://github.com/FasterXML/jackson-docs/wiki/JacksonPolymorphicDeserialization)

## 🧭 Scenario
`starter/` is a small reservations API: `POST /reservations` and `GET /reservations/{id}`, kept in memory. It works, but its JSON is Jackson's improvised JSON. Run it and look at the output before touching anything:

```powershell
.\mvnw.cmd quarkus:dev
```

```bash
./mvnw quarkus:dev
```

In a second terminal (this body has no nested object, so the quotes survive PowerShell 5.1):

```powershell
curl.exe -s -X POST -H "Content-Type: application/json" -d '{\"guestName\":\"Ana\",\"checkIn\":\"2026-12-01\",\"checkOut\":\"2026-12-04\",\"roomType\":\"SUITE\",\"vip\":true}' "http://localhost:8080/reservations"
```

```bash
curl -s -X POST -H "Content-Type: application/json" -d '{"guestName":"Ana","checkIn":"2026-12-01","checkOut":"2026-12-04","roomType":"SUITE","vip":true}' http://localhost:8080/reservations
```

You will see arrays instead of dates, a time with no zone, the Java names of everything, and a `"vip"` field that was accepted and thrown away. The tests describe the contract the clients expect: `ContractTest`, `StrictInputTest`, `PaymentTest`, `MoneyTest`. Every request in them is raw JSON text, so they do not depend on your classes.

## ✅ Tasks

### Task 1 - Names, enum codes and dates
**Goal:** the response uses the public contract. **Validated by:** `ContractTest`.

- [ ] Property names on the wire are `snake_case` (`guest_name`, `check_in`, ...), set **once**, not property by property.
- [ ] Calendar days are ISO text (`"2026-12-01"`) and `createdAt` is a UTC instant (`"2026-11-02T20:12:55.827Z"`). Find what in `application.properties` and in `Reservation` breaks that.
- [ ] Room types travel as `"single"`, `"double"`, `"suite"`, whatever the Java constants are called, and anything else (including `"SUITE"`) is a `400`.

🔮 **Predict first:** a rename refactoring changes the constant `SUITE` to `PENTHOUSE`. With the starter's enum, what happens to the clients? With your solution?

```powershell
.\mvnw.cmd test "-Dtest=ContractTest"
```

### Task 2 - Strict input
**Goal:** the API refuses what it does not understand. **Validated by:** `StrictInputTest`.

- [ ] An unknown property is a `400`. A *misspelled optional* property too: the test sends `depsoit` instead of `deposit`.
- [ ] A calendar day with a time of day (`"2026-12-01T10:00:00"`) is a `400`.

> [!WARNING]
> Two of these only fail because of a default you may not know. Read what the starter does with the `"vip"` field above, and **try** the datetime string against the plain starter before deciding what to change.

🔮 **Predict first:** after your changes, the old array form `[2026,12,1]` of a date: accepted or rejected? Check it, and decide whether you care.

```powershell
.\mvnw.cmd test "-Dtest=StrictInputTest"
```

### Task 3 - A payment is a card or a transfer
**Goal:** one JSON property decides the Java type. **Validated by:** `PaymentTest`.

- [ ] Make `Payment` readable and writable as `{"method":"card","last4":"4242"}` and `{"method":"transfer","iban":"..."}`.
- [ ] A missing or unknown `method` is a `400`.
- [ ] A client must not be able to name a Java class: `{"method":"java.lang.ProcessBuilder"}` and `{"method":"lab.melostudy.Payment$Card"}` are both `400`.

> [!WARNING]
> There are several ways to tell Jackson "the type is in this property". One of them looks equivalent, passes the happy-path tests, and lets the client pick the class. The last test of `PaymentTest` exists to catch it.

```powershell
.\mvnw.cmd test "-Dtest=PaymentTest"
```

### Task 4 - Money is one string
**Goal:** `Money` travels as `"120.50 EUR"`, without annotating `Money`. **Validated by:** `MoneyTest`.

- [ ] A serializer and a deserializer for `Money`, registered in the application's `ObjectMapper` through a CDI bean.
- [ ] `"100 EUR"` and `"7.5 EUR"` are accepted and shown as `"100.00 EUR"` and `"7.50 EUR"`.
- [ ] `"abc"`, `"1.234 EUR"` (a third decimal would be silently rounded), `"10 eur"`, a bare number and the old object form are all `400`.

```powershell
.\mvnw.cmd test "-Dtest=MoneyTest"
```

### Task 5 - Outside the tests: the packaged jar
**Goal:** see the contract from a client's point of view. No test validates this task.

```powershell
.\mvnw.cmd package
java -jar target\quarkus-app\quarkus-run.jar
```

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

- [ ] Send a valid request and a request with an unknown property, and compare the answers (look at the status line and the body of the second one, then compare it with what the test profile shows):

```powershell
curl.exe -i -X POST -H "Content-Type: application/json" -d '{\"guest_name\":\"Ana\",\"check_in\":\"2026-12-01\",\"check_out\":\"2026-12-04\",\"room_type\":\"suite\",\"deposit\":\"30 EUR\"}' "http://localhost:8080/reservations"
curl.exe -i -X POST -H "Content-Type: application/json" -d '{\"guest_name\":\"Ana\",\"check_in\":\"2026-12-01\",\"check_out\":\"2026-12-04\",\"room_type\":\"suite\",\"vip\":true}' "http://localhost:8080/reservations"
```

```bash
curl -i -X POST -H "Content-Type: application/json" -d '{"guest_name":"Ana","check_in":"2026-12-01","check_out":"2026-12-04","room_type":"suite","deposit":"30 EUR"}' http://localhost:8080/reservations
curl -i -X POST -H "Content-Type: application/json" -d '{"guest_name":"Ana","check_in":"2026-12-01","check_out":"2026-12-04","room_type":"suite","vip":true}' http://localhost:8080/reservations
```

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green.

## 👀 What you should see
The valid request in the jar:

```json
{"id":1,"guest_name":"Ana","check_in":"2026-12-01","check_out":"2026-12-04","room_type":"suite","nights":3,"total":"750.00 EUR","deposit":"30.00 EUR","payment":null,"created_at":"2026-11-02T20:12:55.827634500Z"}
```

## 💼 In real projects
- **Write the contract down before the code**, and test it as text (as these tests do). A test that serializes and deserializes with the same `ObjectMapper` passes even when both sides are wrong in the same way.
- **Strict input, tolerant output.** Rejecting unknown properties catches typos and attacks; but a client that must *read* your responses should ignore properties it does not know, so that you can add fields without a new version. Both rules live in the contract.
- **Never use a Java class name as a discriminator** (`Id.CLASS`, or "default typing"). It lets the caller choose which class is instantiated; that is the origin of a whole family of deserialization vulnerabilities. Use `Id.NAME` and a closed list of subtypes.
- **Time:** store and exchange `Instant` (UTC) for moments and `LocalDate` for calendar days. A `LocalDateTime` needs a zone to mean anything; when a business rule is "check-in at 14:00 hotel time", send the zone or the zone id explicitly.
- **Money:** never `double`. A `BigDecimal` as a JSON *number* can still lose precision in a JavaScript client; many APIs send amounts as strings, or integer minor units (cents).
- **Enums grow.** A client that fails on an unknown enum value breaks when you add a constant. Decide per field whether an unknown value is an error (input) or tolerated (output), and document it.
- **Configuration vs annotations:** global behavior (naming, strictness, dates) belongs in `application.properties` or an `ObjectMapperCustomizer`; per-type exceptions belong in annotations. A mix with no rule is how two endpoints end up using different date formats.
- **Error responses are JSON too.** The names of their properties follow the same naming strategy (in the test profile a `400` for a bad body returns a small JSON describing the property and the position; in the packaged jar the body of that same `400` is empty). Do not rely on that default as your error contract: lab 05 owns it.
- **Native image:** reflection-based JSON needs the classes registered (`@RegisterForReflection`) when they are not found automatically; lab 34 covers it.

## 🚀 Stretch goal
The array form of a date (`[2026,12,1]`) is still accepted after task 2. Find out which Jackson feature accepts it and whether you can switch it off for `LocalDate` only. Then write the test first.

## 💡 Hints
<details><summary>Hint 1 (direction)</summary>

Everything in task 1 and the strictness of task 2 is configuration: look at the Jackson section of the Quarkus configuration reference. The enum code is one annotation on one method of the enum.
</details>

<details><summary>Hint 2 (task 2, dates)</summary>

Jackson has a per-type "leniency" setting. In an `ObjectMapperCustomizer` you can set it for one class with `mapper.configOverride(SomeType.class).setFormat(JsonFormat.Value.forLeniency(...))`.
</details>

<details><summary>Hint 3 (task 3)</summary>

`@JsonTypeInfo(use = ..., property = "method")` on the interface and `@JsonSubTypes` listing each record with its name. Read the values of `JsonTypeInfo.Id` and pick the one that does not involve class names.
</details>

<details><summary>Hint 4 (task 4)</summary>

A class extending `StdSerializer<Money>`, another extending `StdDeserializer<Money>`, a `SimpleModule` with both, and a `@Singleton` bean implementing `ObjectMapperCustomizer` that registers the module. For a bad string, ask the `DeserializationContext` to report it rather than failing in your own code.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1.

**The starter.**
- With `quarkus.jackson.write-dates-as-timestamps=true` the response has `"checkIn":[2026,12,1]` and `"createdAt":[2026,11,2,15,12,0,845486400]`: numbers, nanoseconds, and no zone at all (it is a `LocalDateTime` of the server's clock).
- An unknown property (`"vip":true`) is accepted and discarded: Quarkus turns Jackson's failure off by default.

**Task 1.**
- Removing `write-dates-as-timestamps=true` is enough for ISO text (it is already the Quarkus default; the solution writes `=false` to make it explicit).
- With the starter's plain enum the contract is the constant names (`"SUITE"`); renaming `SUITE` to `PENTHOUSE` changes the JSON and breaks every client. With `@JsonValue` the code is the contract.
- After switching to snake_case, a request with camelCase keys (`guestName`) is rejected once strictness is on, because they are unknown properties.

**Task 2.**
- `quarkus.jackson.fail-on-unknown-properties=true` makes both `"vip"` and `"depsoit"` a `400`. The misspelled one matters more: `deposit` is optional, so without strictness the request succeeds and the deposit is lost without a trace.
- By default a `LocalDate` accepts `"2026-12-01T10:00:00"` and keeps only the day (`201`, `check_in` = `2026-12-01`). `mapper.configOverride(LocalDate.class).setFormat(JsonFormat.Value.forLeniency(Boolean.FALSE))` turns it into a `400`. It does **not** switch off the array form `[2026,12,1]`, which is still accepted.

**Task 3.**
- With `Id.CLASS` and property `method`, the happy-path requests with `"card"` and `"transfer"` stop working (`400`), and `{"method":"lab.melostudy.Payment$Card","last4":"1"}` is **accepted** (`201`): the client chose the class. With `Id.NAME` and `@JsonSubTypes`, class names are `400`.
- A missing `method`, an unknown `method` and a class name that is not in the list are all `400`.

**Task 4.**
- Both a deserializer that reports through `context.handleWeirdStringValue(...)` and one that throws `IllegalArgumentException` end in a `400` (Jackson wraps it). The first one is preferred: the error carries the property name and the position in the document.
- `Money` itself needs no annotation: `MoneyJson` (an `ObjectMapperCustomizer`) registers a module in the application's `ObjectMapper`.

**Task 5.**
- In the jar both rejected requests answer `400 Bad Request` with an **empty body** (`content-length: 0`). In the tests the same `400` carries a small JSON naming the property and the position. This is the usual dev/test versus production difference.
</details>

## 🎤 Self-assessment
1. Which two Jackson defaults does Quarkus change, and why does each matter for an API?
2. `LocalDate`, `LocalDateTime`, `Instant`, `ZonedDateTime`: which would you put in an API contract for (a) a birthday, (b) the moment an order was created, (c) a meeting "at 10:00 local time of the venue"?
3. What is wrong with `@JsonTypeInfo(use = Id.CLASS)` on a request body type?
4. Why should the JSON value of an enum be independent of the Java constant name? What would you do if a client sends a value you do not know yet?
5. When do you use an `ObjectMapperCustomizer` instead of an annotation on the type, and what does registering a module change for the rest of the application?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
