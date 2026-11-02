# 🧩 Lab 04 - Quarkus REST

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
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead. In PowerShell use `curl.exe` (plain `curl` is an alias of another command).

## 🎯 Why it matters
Most Quarkus services you will write at work expose a REST API. Getting it right means more than returning JSON: correct **status codes**, a proper `Location` header, query parameters with defaults, headers, and knowing what happens when a client sends something unexpected. This lab builds a small, complete resource and then deliberately pokes it with bad requests to see what the framework does for you (and what it does not).

## 🧠 Concepts
- Resources and HTTP verbs: `@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE`
- Content negotiation: `@Produces`, `@Consumes`, and the JSON extension (`quarkus-rest-jackson`)
- Parameters: `@RestPath`, `@RestQuery`, `@DefaultValue` (the Quarkus REST shortcuts for `@PathParam`/`@QueryParam`)
- Typed responses: `RestResponse<T>` (status, headers, `Location`)
- Errors: `NotFoundException` and the status codes Quarkus produces by itself
- Response filters: `@ServerResponseFilter`
- Resource lifecycle (singleton by default)

## 📚 Documentation
- [Quarkus REST guide](https://quarkus.io/guides/rest) (parameters, `RestResponse`, filters, lifecycle)
- [Writing JSON REST services](https://quarkus.io/guides/rest-json)

> [!NOTE]
> This lab uses **Quarkus REST** (`quarkus-rest`), the current implementation. Older tutorials mention *RESTEasy Classic* (`quarkus-resteasy`); do not mix them up.

## 🧭 Scenario
`starter/` has a product catalog backend ready to use: the `Product` and `ProductInput` records and an in-memory `ProductRepository` (ids 1-5 are seeded: three `books` and two `games`). The persistence layer is already done on purpose; **you build the REST layer** in `ProductResource` (plus a filter). All 18 tests fail at the start.

| Endpoint | Behavior |
|---|---|
| `GET /products` | List, with `?category=`, `?page=` (default 0), `?size=` (default 10) and an `X-Total-Count` header |
| `GET /products/{id}` | `200` + JSON, or `404` |
| `POST /products` | `201` + `Location` header + the created product |
| `PUT /products/{id}` | `200` + updated product, or `404` |
| `DELETE /products/{id}` | `204`, or `404` if it does not exist |

Run the tests at any time:

```powershell
.\mvnw.cmd test
```

```bash
mvn test
```

## ✅ Tasks

### Task 1 - Read one product
**Validated by:** `ProductResourceTest` (`getReturnsTheProductAsJson`, `getUnknownProductIs404`).

- [ ] Annotate `ProductResource` so it produces and consumes JSON, and inject `ProductRepository` through the constructor.
- [ ] Implement `GET /products/{id}`.

🔮 **Predict first:** if `findById` returns an empty `Optional` and your method returns `null` (`.orElse(null)`), which status will the client get? Try it, then make the test pass for real.

### Task 2 - Create
**Validated by:** `ProductResourceTest` (`createReturns201WithLocationAndBody`, `createWithAnotherContentTypeIs415`).

- [ ] Implement `POST /products`: it must answer `201 Created`, with a `Location` header pointing at the new resource and the created product as body.
- [ ] Read the `415` test: you do not have to write code for it. Which annotation makes Quarkus reject other content types?

### Task 3 - Update and delete
**Validated by:** `ProductResourceTest`.

- [ ] Implement `PUT /products/{id}` (replace) and `DELETE /products/{id}`.
- [ ] Decide what deleting twice returns. The tests expect `404` the second time; think about the alternative (`204` again) and when each is the better design.

### Task 4 - Listing, filtering and paging
**Validated by:** `ProductQueryTest`.

- [ ] Implement `GET /products` with `category`, `page` (0-based) and `size` as query parameters with defaults.
- [ ] Return the total count **before paging** in the `X-Total-Count` header.

### Task 5 - A response filter
**Validated by:** `RequestIdFilterTest`.

- [ ] Create a filter that adds an `X-Request-Id` header to every response: the one the client sent, or a new UUID if there is none.

### Task 6 - Poke it with bad requests (no tests)
**Goal:** see what Quarkus does for you and what it leaves to you. Package and run the jar (this is how it would run in production):

```powershell
.\mvnw.cmd package -DskipTests
java -jar target\quarkus-app\quarkus-run.jar
```

```bash
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

From **another terminal**, run each request. 🔮 **Predict first, in writing:** the status code of every row.

| # | Request |
|---|---|
| a | `GET /products/abc` (non-numeric id) |
| b | `GET /products/1` with header `Accept: text/plain` |
| c | `POST /products` with malformed JSON |
| d | `POST /products` with an extra, unknown JSON field |
| e | `POST /products` with an empty body |
| f | `DELETE /products` (the collection) |
| g | `GET /products?page=-1` |
| h | `GET /products?size=abc` |
| i | `POST /products` with `"price": "abc"` |

```powershell
curl.exe -i http://localhost:8080/products/abc
curl.exe -i -H "Accept: text/plain" http://localhost:8080/products/1
curl.exe -i -X POST -H "Content-Type: application/json" -d '{oops' http://localhost:8080/products
curl.exe -i -X POST -H "Content-Type: application/json" -d '{\"name\":\"a\",\"category\":\"b\",\"price\":1,\"color\":\"red\"}' http://localhost:8080/products
curl.exe -i -X POST -H "Content-Type: application/json" http://localhost:8080/products
curl.exe -i -X DELETE http://localhost:8080/products
curl.exe -i "http://localhost:8080/products?page=-1"
curl.exe -i "http://localhost:8080/products?size=abc"
curl.exe -i -X POST -H "Content-Type: application/json" -d '{\"name\":\"a\",\"category\":\"b\",\"price\":\"abc\"}' http://localhost:8080/products
```

```bash
curl -i http://localhost:8080/products/abc
curl -i -H "Accept: text/plain" http://localhost:8080/products/1
curl -i -X POST -H "Content-Type: application/json" -d '{oops' http://localhost:8080/products
curl -i -X POST -H "Content-Type: application/json" -d '{"name":"a","category":"b","price":1,"color":"red"}' http://localhost:8080/products
curl -i -X POST -H "Content-Type: application/json" http://localhost:8080/products
curl -i -X DELETE http://localhost:8080/products
curl -i "http://localhost:8080/products?page=-1"
curl -i "http://localhost:8080/products?size=abc"
curl -i -X POST -H "Content-Type: application/json" -d '{"name":"a","category":"b","price":"abc"}' http://localhost:8080/products
```

> [!WARNING]
> Look closely at rows e and g: they are **your** bugs, not the client's. Which header is missing from those responses, and why? Lab 05 fixes all of this.

### Task 7 - Resource lifecycle
**Goal:** discover that a resource is not created per request.

🔮 **Predict first:** add a mutable `int calls` field to `ProductResource`, increment it in an endpoint you add temporarily, and call that endpoint three times. What do you get? Then think about what that means for state in resources, and what lab 03 says about the alternatives. Revert the experiment afterwards.

**Success criteria:** the 18 tests pass inside `starter/` and you have done Tasks 6 and 7.

```powershell
.\mvnw.cmd verify
```

```bash
mvn verify
```

## 👀 What you should see
- `POST` answers `201 Created` with `Location: http://localhost:8080/products/6` (host and port depend on where you run it).
- `GET /products?page=0&size=2` returns two products and `X-Total-Count: 5`.
- Every response, including the `404`s, carries an `X-Request-Id`.

## 💼 In real projects
- **Use the right status code.** `201` + `Location` on create, `204` when there is nothing to return, `404` for a missing resource, `409` for conflicts, `400` for bad input. Clients and gateways make decisions based on them.
- **Do not expose your entities.** `Product` (output) and `ProductInput` (input) are separate on purpose: the server owns `id`, and you can evolve the database without breaking the API. Lab 12 covers DTO mapping properly.
- **Keep resources thin.** Parsing and HTTP concerns in the resource, rules in a service bean, data access in a repository. Resources are singletons: no per-request state in fields.
- **Always bound paging.** Here `size=100000` is accepted; in production cap it (for example 100) so one client cannot ask for the whole table.
- **Idempotency is a design decision.** `DELETE` returning `404` the second time is informative; returning `204` again is friendlier to retrying clients. Pick one and document it.
- **Never trust request headers blindly.** Echoing `X-Request-Id` is common for tracing, but in production validate its length and format before logging or propagating it.
- **Do not leak internals in errors.** In prod mode Quarkus hides the stack trace of an unexpected `500` behind an error id (you saw it in Task 6), but expected failures (`400`, `404`, `409`) should return a clear, consistent error body that you design (lab 05).
- **Version and document the API** (`/v1/...` or media types, OpenAPI in lab 06) before other teams depend on it.

## 🚀 Stretch goal
Add a second method for the list that produces `text/csv` (a plain `String`) and see how Quarkus picks one of the two based on the `Accept` header. Or: replace the `list` signature to return a `PagedResponse<Product>` record (items, page, size, total) instead of the `X-Total-Count` header, and discuss which one you would prefer to maintain.

## 💡 Hints
<details><summary>Hint 1 (direction)</summary>

- Class level: `@Path`, `@Produces`, `@Consumes`.
- Task 1: Quarkus REST lets you take the path variable as a method parameter with `@RestPath`. Throw the standard JAX-RS exception for "not found".
- Task 2: you need the URI of the request to build the `Location`; look at `UriInfo` and at `RestResponse.ResponseBuilder`.
- Task 4: query parameters with defaults use `@RestQuery` plus `@DefaultValue`; a response with extra headers needs `RestResponse`.
- Task 5: look for `@ServerResponseFilter` in the Quarkus REST guide.
</details>

<details><summary>Hint 2 (more concrete)</summary>

```java
@GET
@Path("/{id}")
public Product get(@RestPath long id) {
    return repository.findById(id).orElseThrow(NotFoundException::new);
}
```

```java
@POST
public RestResponse<Product> create(ProductInput input, @Context UriInfo uriInfo) {
    Product created = repository.create(input);
    URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id())).build();
    return RestResponse.ResponseBuilder.<Product>created(location).entity(created).build();
}
```

- The explicit `<Product>` type witness avoids a Java inference error.
- For the list: `RestResponse.ResponseBuilder.ok(items).header("X-Total-Count", total).build()`.
- A `void` method that returns normally answers `204`.
- The filter is a method annotated `@ServerResponseFilter` that receives `ContainerRequestContext` and `ContainerResponseContext`; set the header with `response.getHeaders().putSingle(...)`.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1 (test mode, and the packaged jar where noted).

- **Task 1 (null):** returning `null` from a resource method answers **`204 No Content`**, not `404`. That is why you must throw `NotFoundException`.
- **Task 2 (415):** the class-level `@Consumes(APPLICATION_JSON)`. A request with another `Content-Type`, or with none, gets `415`.
- **Task 6:**

| # | Status | Notes |
|---|---|---|
| a | `404` | `abc` cannot be converted to `long`; JAX-RS answers `404` for an unconvertible path parameter, not `400` |
| b | `406` | `@Produces` only offers JSON |
| c | `400` | Malformed JSON |
| d | `201` | Unknown fields are **ignored** by default |
| e | `500` | Your code receives `null` and fails with a `NullPointerException`. In the packaged jar the body is a generic text page with an error id (no stack trace), and **there is no `X-Request-Id`**: the response filter does not run for errors handled at the HTTP server level |
| f | `405` | There is no `DELETE` on the collection |
| g | `500` | `skip(-10)` throws `IllegalArgumentException`; same generic `500`, no `X-Request-Id` |
| h | `404` | Same rule: an unconvertible query parameter is also answered with `404` |
| i | `400` | JSON body produced by Jackson that echoes internal details: `{"objectName":"Class","attributeName":"price","line":1,"column":36,"value":"abc"}` |

Also verified: a trailing slash (`/products/`) returns the same as `/products`, `size=0` returns an empty list, and with `quarkus-rest-jackson` a method without `@Produces` still answers `application/json`.
- **Task 7:** the counter returns `1`, `2`, `3`: Quarkus REST resources are **singletons** by default, so mutable fields are shared by all requests (and all threads). Keep resources stateless; if you really need per-request state use a `@RequestScoped` bean (lab 03).
</details>

## 🎤 Self-assessment
Answer without looking at the code; you should be able to explain each one in an interview.

1. What status do you return for a successful `POST` that creates a resource, and which header must accompany it?
2. A method returns `null`. What does the client receive and why can that hide bugs?
3. A client sends `GET /products/abc`. What status does Quarkus answer and why is it not `400`?
4. What is the scope of a resource class in Quarkus REST, and what is the danger of that?
5. Name three things the framework did **not** protect you from in Task 6 and which later lab fixes them.

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand about Quarkus REST?
- What was hard?
- What should I reinforce?
