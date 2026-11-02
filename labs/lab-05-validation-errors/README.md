# 🧩 Lab 05 - Validation and errors

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
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead. In PowerShell use `curl.exe`, and put `-D...` arguments in quotes.

## 🎯 Why it matters
In lab 04 you found requests that crashed the API with a `500`, an error body that exposed Jackson internals, and a bug-for-bug different shape for every failure. In a real project that is not acceptable: clients (and other teams) need **validation at the edge, the right status code, and one predictable error format**, while operators need the full story in the **logs** and nothing sensitive in the **response**. This lab hardens the product API from lab 04.

## 🧠 Concepts
- Bean Validation (`quarkus-hibernate-validator`): constraints on request bodies, records and query parameters
- `@Valid` and why `@NotNull` is still needed
- Custom constraints and **validators that are CDI beans**
- Exception mappers with `@ServerExceptionMapper`: most specific type wins
- One error contract (`ApiError`): stable `code`, human `message`, per-field `errors`, `errorId`
- A catch-all mapper: log everything, reveal nothing
- Business errors as exceptions (`409 Conflict`)

## 📚 Documentation
- [Validation with Hibernate Validator](https://quarkus.io/guides/validation)
- [Quarkus REST: exception mapping](https://quarkus.io/guides/rest#exception-mapping)

> [!NOTE]
> The public guides show the latest version; your project uses 3.40.1.1 (LTS), which matches.

## 🧭 Scenario
`starter/` is the product API from lab 04 (resource, in-memory repository, `X-Request-Id` filter) plus a few ready-made pieces:

| Provided | What it is |
|---|---|
| `ApiError`, `FieldError` | The error contract (records) you must use everywhere |
| `DuplicateProductException` | Thrown by the repository when a product name is taken (case-insensitive) |
| `CategoryCatalog` | Allowed categories, read from config (`catalog.categories`, default `books,games,music`) |
| `BoomResource` (in `src/test`) | A test-only endpoint that fails with a *sensitive* message, to prove nothing leaks |

There are 39 tests: the 18 from lab 04 plus 21 new ones. At the start **20 fail**; the rest are regression guards and must stay green all along.

### The error contract

| Situation | Status | `code` |
|---|---|---|
| Constraint violations (body or parameters) | 400 | `VALIDATION_ERROR` (with `errors[]`) |
| JSON that cannot be read into the input type | 400 | `MALFORMED_REQUEST` |
| Resource not found | 404 | `NOT_FOUND` |
| Duplicate name | 409 | `CONFLICT` |
| Any other HTTP error (405, 406, 415...) | its own status | `HTTP_<status>` |
| Unexpected failure | 500 | `INTERNAL_ERROR` (with `errorId`) |

```json
{
  "code": "VALIDATION_ERROR",
  "message": "The request is not valid",
  "errors": [{ "field": "price", "message": "must be greater than 0" }]
}
```

## ✅ Tasks

### Task 1 - Validate the request body
**Validated by:** `ValidationTest` (first group).

- [ ] Add the `hibernate-validator` extension:

```bash
quarkus extension add hibernate-validator
```

```powershell
.\mvnw.cmd quarkus:add-extension "-Dextensions=hibernate-validator"
```

```bash
./mvnw quarkus:add-extension -Dextensions=hibernate-validator
```

- [ ] Constrain `ProductInput` (the constraints go on the record components):
  - `name`: not blank, at most 100 characters.
  - `category`: not blank.
  - `price`: not null, positive, at most 7 integer digits and 2 decimals.
- [ ] Make Quarkus validate the bodies of `POST` and `PUT`.

🔮 **Predict first:** with the constraints in place and the bodies validated, what does `POST /products` with an **empty body** return? Run the tests and find out.

> [!WARNING]
> `@Valid` on its own does not do what you probably expect when the body itself is missing. Read the failing test and the status before you fix it.

### Task 2 - Validate query parameters
**Validated by:** `ValidationTest` (`pagingParametersAreValidated`).

- [ ] Constrain `page` (minimum 0) and `size` (between 1 and 100) in `list`. This closes the `page=-1` bug you found in lab 04.

### Task 3 - A custom constraint backed by a CDI bean
**Validated by:** `ValidationTest` (`onlyConfiguredCategoriesAreAccepted`).

- [ ] Create an `@AllowedCategory` constraint and its `ConstraintValidator`. The validator must use `CategoryCatalog` (inject it!) and accept `null`/blank values (`@NotBlank` already reports those, so each problem appears once).
- [ ] Apply it to `ProductInput.category`.

### Task 4 - One error shape for validation, wrong-shape JSON and conflicts
**Validated by:** `ErrorFormatTest` (first group).

- [ ] Create a class with `@ServerExceptionMapper` methods returning `RestResponse<ApiError>` (set the content type to JSON explicitly):
  - `ConstraintViolationException` → `400 VALIDATION_ERROR`, one `FieldError` per violation. The field must be the **last element of the property path** (`price`, not `create.input.price`), sorted for stable output.
  - `MismatchedInputException` (Jackson: valid JSON with the wrong shape, e.g. `"price": "abc"`) → `400 MALFORMED_REQUEST`. Do not copy Jackson's message.
  - `DuplicateProductException` → `409 CONFLICT`.
- [ ] Look at what the validation response looked like **before** your mapper (run a request in dev mode or read the failing assertion): note the extra prefix in the field names and the language of the messages.
- [ ] Make the messages independent of the server's language. Add this to `application.properties`:

```properties
quarkus.default-locale=en
```

> [!NOTE]
> Validation messages follow the JVM default locale. On a Spanish-language machine they come out in Spanish unless you pin the locale. Tests in this lab never compare message text, on purpose.

### Task 5 - HTTP errors and the catch-all
**Validated by:** `ErrorFormatTest` (second group) and the lab 04 regression tests.

- [ ] Add a mapper for `Throwable`: log the exception **with its stack trace** and a freshly generated `errorId`, and answer `500 INTERNAL_ERROR` with only that id and a generic message.

🔮 **Predict first:** after adding it, which of the previously green tests will turn red, and why? Run `ProductResourceTest` and see.

- [ ] Fix it with a mapper for `WebApplicationException` that keeps the original status and only changes the body (`NOT_FOUND` for 404, `HTTP_<status>` otherwise).
- [ ] Make `malformedJsonIsA400WithoutJacksonInternals` pass. Find out which exception a *syntactically broken* JSON really arrives as (hint: look at its cause).

### Task 6 - Re-run the bad requests against the jar
**Goal:** check the hardened API the way it runs in production.

```powershell
.\mvnw.cmd package -DskipTests
java -jar target\quarkus-app\quarkus-run.jar
```

```bash
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

🔮 **Predict first** the status and `code` for each request, then run them from another terminal:

```powershell
curl.exe -i http://localhost:8080/products/abc
curl.exe -i -H "Accept: text/plain" http://localhost:8080/products/1
curl.exe -i -X POST -H "Content-Type: application/json" -d '{oops' http://localhost:8080/products
curl.exe -i -X POST -H "Content-Type: application/json" -d '{\"name\":\"a\",\"category\":\"books\",\"price\":1,\"color\":\"red\"}' http://localhost:8080/products
curl.exe -i -X POST -H "Content-Type: application/json" http://localhost:8080/products
curl.exe -i -X DELETE http://localhost:8080/products
curl.exe -i "http://localhost:8080/products?page=-1"
curl.exe -i "http://localhost:8080/products?size=abc"
curl.exe -i -X POST -H "Content-Type: application/json" -d '{\"name\":\"a\",\"category\":\"books\",\"price\":\"abc\"}' http://localhost:8080/products
curl.exe -i -X POST -H "Content-Type: application/json" -d '{\"name\":\"Chess\",\"category\":\"games\",\"price\":5}' http://localhost:8080/products
curl.exe -i -X POST -H "Content-Type: application/json" -d '{\"name\":\"\",\"category\":\"weapons\",\"price\":-1}' http://localhost:8080/products
curl.exe -i http://localhost:8080/nope
```

```bash
curl -i http://localhost:8080/products/abc
curl -i -H "Accept: text/plain" http://localhost:8080/products/1
curl -i -X POST -H "Content-Type: application/json" -d '{oops' http://localhost:8080/products
curl -i -X POST -H "Content-Type: application/json" -d '{"name":"a","category":"books","price":1,"color":"red"}' http://localhost:8080/products
curl -i -X POST -H "Content-Type: application/json" http://localhost:8080/products
curl -i -X DELETE http://localhost:8080/products
curl -i "http://localhost:8080/products?page=-1"
curl -i "http://localhost:8080/products?size=abc"
curl -i -X POST -H "Content-Type: application/json" -d '{"name":"a","category":"books","price":"abc"}' http://localhost:8080/products
curl -i -X POST -H "Content-Type: application/json" -d '{"name":"Chess","category":"games","price":5}' http://localhost:8080/products
curl -i -X POST -H "Content-Type: application/json" -d '{"name":"","category":"weapons","price":-1}' http://localhost:8080/products
curl -i http://localhost:8080/nope
```

- [ ] Compare with the table you filled in lab 04. Which rows changed, which did not, and is each of them the right call?

**Success criteria:** all 39 tests pass inside `starter/` and you have done Task 6.

```powershell
.\mvnw.cmd verify
```

```bash
mvn verify
```

## 👀 What you should see
- A request with several invalid fields returns **all** of them in `errors[]`, not just the first.
- Every error body, from `404` to `405` to `500`, has the same JSON shape and an `X-Request-Id` header.
- In the log, an unexpected failure appears as `Unexpected error <uuid>: ...` with the stack trace; the response only contains that same uuid.

## 💼 In real projects
- **Validate at the edge, enforce in the domain.** Bean Validation catches malformed input cheaply; rules that depend on state (name uniqueness, stock, permissions) belong to the service and, for uniqueness, to a **database constraint**. The in-memory check here is racy by nature; lab 08 and lab 11 give you the real thing.
- **The error contract is part of your API.** Stable machine-readable `code`s let clients react without parsing messages. There is a standard for this, *Problem Details* (RFC 9457, `application/problem+json`); the shape here is a simplified version of the same idea. Decide once per organization.
- **Never put `e.getMessage()` of an unexpected exception in a response.** It can contain SQL, hostnames or credentials (the `BoomResource` message is a realistic example). Log it, return an id.
- **Log levels matter.** Client mistakes (`4xx`) are not errors on your side: logging them at `ERROR` buries the real problems and triggers false alerts. Only the catch-all logs at `ERROR`.
- **Sensitive data stays out of logs too:** do not log request bodies or tokens just to "have more context".
- **Do not let a catch-all hide client errors.** As you saw in Task 5, a mapper for `Throwable` swallows `404`, `405` and `415` unless you keep a mapper for `WebApplicationException`.
- **Be careful with broad mappers.** A mapper for `JsonProcessingException` would also catch *serialization* failures of your own responses and blame the client for your bug; map only the input-side exceptions.
- **Pin the locale of API messages** or return codes only and translate in the client; otherwise behavior changes with the server's OS language.
- **`400` vs `422`:** many teams use `400` for malformed requests and `422` for well-formed but semantically invalid ones. This lab uses `400` for both; consistency inside your API matters more than the choice.

## 🚀 Stretch goal
1. Add an optional `details` map to `ApiError` and fill it for conflicts (`{"existingId": 4}`). Which tests need to change, and which clients could break?
2. Add a *class-level* constraint (cross-field), e.g. "games cannot cost more than 100", and see how its violation appears in `errors[]` (what is the `field`?).
3. Switch the contract to RFC 9457 Problem Details and keep the tests green by adapting them.

## 💡 Hints
<details><summary>Hint 1 (direction)</summary>

- Task 1: constraints on a record go on its components. Controlling "is the body there at all?" is a different constraint from "is the body valid?".
- Task 3: in Quarkus a `ConstraintValidator` can be a CDI bean; give it a scope and constructor-inject the catalog.
- Task 4/5: exception mappers are selected by the **most specific** exception type among those you declare.
- Task 5: a syntactically broken JSON does not reach you as a Jackson exception directly.
</details>

<details><summary>Hint 2 (more concrete)</summary>

```java
public record ProductInput(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @AllowedCategory String category,
        @NotNull @Positive @Digits(integer = 7, fraction = 2) BigDecimal price) {
}
```

```java
public RestResponse<Product> create(@Valid @NotNull ProductInput input, @Context UriInfo uriInfo)
```

```java
@Constraint(validatedBy = AllowedCategoryValidator.class)
@Target({ FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface AllowedCategory {
    String message() default "is not an allowed category";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

```java
@ServerExceptionMapper
public RestResponse<ApiError> validation(ConstraintViolationException e) { ... }
```

- The last element of a property path: iterate `violation.getPropertyPath()` and keep the last `Node.getName()`.
- Build the response with `RestResponse.ResponseBuilder.<ApiError>create(status).entity(body).type(MediaType.APPLICATION_JSON_TYPE).build()`.
- Broken JSON: a `WebApplicationException` (status 400) whose `getCause()` is a Jackson `JsonProcessingException`.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1 (test mode, and the packaged jar for Task 6).

- **Task 1 (empty body):** with only `@Valid`, an empty body is **not** a violation: validation skips `null`. Your code then receives `null` and fails with a `NullPointerException` → `500`. Adding `@NotNull` to the parameter makes it a `400` (`errors[0].field` is `input`, message "must not be null").
- **Task 4 (before your mapper):** Quarkus already answers `400` with its own body, `{"title":"Constraint Violation","status":400,"violations":[{"field":"create.input.name","message":"..."}]}`. The field names expose the Java method and parameter (`create.input.name`, `list.page`). With no pinned locale the messages came out in Spanish on a Spanish-language machine ("no debe estar vacío").
- **Task 4 (`"price": "abc"`):** the built-in mapper answers `400` with `{"objectName":"Class","attributeName":"price","line":1,"column":40,"value":"abc"}`. Your own mapper for `MismatchedInputException` replaces it.
- **Task 5 (catch-all):** a mapper for `Throwable` alone turns **404, 405 and 415 into 500** (verified), so the lab 04 regression tests fail until the `WebApplicationException` mapper exists.
- **Task 5 (broken JSON):** `{oops` arrives as a plain `WebApplicationException` (status 400) whose cause is a `JsonParseException`. That is why a `JsonProcessingException` mapper never sees it; check `getCause()` inside the `WebApplicationException` mapper.
- **Task 5 (`X-Request-Id` on 500):** once the failure goes through a mapper it travels the normal JAX-RS pipeline, so the response filter runs. In lab 04 the unhandled `500` was produced by the HTTP server layer and had no header.
- **Task 6:**

| Request | Status | `code` |
|---|---|---|
| `GET /products/abc` | 404 | `NOT_FOUND` |
| `Accept: text/plain` | 406 | `HTTP_406` |
| malformed JSON | 400 | `MALFORMED_REQUEST` |
| extra unknown field | 201 | (accepted: unknown fields are ignored; strict mode is a design decision) |
| empty body | 400 | `VALIDATION_ERROR`, field `input` |
| `DELETE /products` | 405 | `HTTP_405` |
| `page=-1` | 400 | `VALIDATION_ERROR`, field `page` |
| `size=abc` | 404 | `NOT_FOUND` (unconvertible query parameters are answered as 404) |
| `"price": "abc"` | 400 | `MALFORMED_REQUEST` |
| duplicate name | 409 | `CONFLICT` |
| everything invalid | 400 | `VALIDATION_ERROR` with `category`, `name` and `price` |
| `GET /nope` | 404 | `NOT_FOUND` (even unknown routes now use the contract) |
</details>

## 🎤 Self-assessment
Answer without looking at the code; you should be able to explain each one in an interview.

1. Why does `@Valid` alone not reject a missing request body, and how do you fix it?
2. How does Quarkus choose among several exception mappers that could handle the same exception?
3. You add a `Throwable` mapper and suddenly your `404`s become `500`s. Why, and what do you do?
4. What belongs in the response of an unexpected failure, and what belongs only in the log? How does support connect the two?
5. Where do you validate "the product name must be unique"? Why is an application-level check not enough?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand about validation and error handling?
- What was hard?
- What should I reinforce?
