# 🧩 Lab 06 - OpenAPI

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
Your API is only useful if other people (front-end, other teams, partners) can understand and call it without reading your Java code. An **OpenAPI document** is that contract: tools generate clients, docs and mocks from it. Quarkus generates one from your code, but *what it generates by default is incomplete and sometimes wrong*. This lab takes the product API from lab 05 and makes its document accurate, useful and safe to publish.

## 🧠 Concepts
- OpenAPI document and Swagger UI (`quarkus-smallrye-openapi`): `/q/openapi`, `/q/swagger-ui`
- Code-first documentation with MicroProfile OpenAPI annotations: `@OpenAPIDefinition`, `@Tag`, `@Operation`, `@Parameter`, `@APIResponse`, `@Schema`
- Reusable components (`ref`) to avoid repeating the same error responses
- What is derived for free (Bean Validation constraints) and what is not (anything your exception mappers do)
- `OASFilter`: change the document programmatically
- OpenAPI in production: what to expose, storing the document at build time

## 📚 Documentation
- [Using OpenAPI and Swagger UI](https://quarkus.io/guides/openapi-swaggerui)

> [!NOTE]
> The public guides show the latest version; your project uses 3.40.1.1 (LTS), which matches.

## 🧭 Scenario
`starter/` is the product API as you left it in lab 05 (validated, with the `ApiError` contract), **without** the OpenAPI extension. The 39 tests from lab 05 pass and must keep passing. The 14 new tests in `OpenApiTest` read the generated document itself and check that it is accurate; at the start all of them fail.

Run the tests whenever you want:

```powershell
.\mvnw.cmd test
```

```bash
mvn test
```

## ✅ Tasks

### Task 1 - Get the default document
**Validated by:** `OpenApiTest` (`theDocumentIsServedInYamlAndJson`, `swaggerUiIsAvailableDuringDevelopment`).

- [ ] Add the `smallrye-openapi` extension:

```powershell
.\mvnw.cmd quarkus:add-extension "-Dextensions=smallrye-openapi"
```

```bash
./mvnw quarkus:add-extension -Dextensions=smallrye-openapi
```

- [ ] Start dev mode and open the document and the UI:

```powershell
.\mvnw.cmd quarkus:dev
```

```bash
./mvnw quarkus:dev
```

```powershell
curl.exe "http://localhost:8080/q/openapi?format=json"
```

```bash
curl "http://localhost:8080/q/openapi?format=json"
```

Swagger UI: `http://localhost:8080/q/swagger-ui`.

🔮 **Predict first, in writing:** looking at the code, what will the generated document say about `POST /products`: which success status? And which error responses will `GET /products/{id}` list? Then read the real document and compare.

> [!WARNING]
> A generated document describes what the *code shape* suggests, not what your exception mappers and filters actually do at runtime. Look for lies.

### Task 2 - Describe the API as a whole
**Validated by:** `theApiHasATitleVersionAndDescription`, `theResourceIsGroupedUnderADocumentedTag`.

- [ ] Set a title ("Product Catalog API"), version (`1.0.0`) and description, plus a contact.
- [ ] Group `ProductResource` under a tag called `Products` and give the tag a description.
- [ ] Put the document-wide annotation where it is honored. Try it on a plain class first and check the document.

### Task 3 - Operations and parameters
**Validated by:** `everyProductOperationHasAMeaningfulSummary`, `parametersAreDescribed`.

- [ ] Give each of the five operations a clear summary (and a description where it adds value).
- [ ] Describe `category`, `page` and `size`.

### Task 4 - Responses that tell the truth
**Validated by:** `createIsDocumentedAs201NotAs200`, `listDocumentsTheTotalCountHeader`, `failuresAreDocumentedWithTheErrorSchema`.

- [ ] `POST` must be documented as `201`, with its `Location` header and the `Product` body.
- [ ] `GET /products` must document the `X-Total-Count` header.
- [ ] Document the error responses (`400`, `404`, `409`) of each operation with the `ApiError` schema. Declare each error response **once** as a reusable component and reference it, instead of copying it onto every method.

### Task 5 - Schemas
**Validated by:** `errorSchemasArePublished`, `schemaFieldsHaveDescriptionsAndExamples`, `validationConstraintsShowUpInTheContract`.

- [ ] Describe `Product`, `ProductInput`, `ApiError` and `FieldError` and add a realistic example to each field.
- [ ] Before writing anything for it, read `validationConstraintsShowUpInTheContract`: you did not annotate lengths or minimums for OpenAPI. Where do they come from?

### Task 6 - A filter for what every operation shares
**Validated by:** `everyProductOperationDocumentsTheUnexpectedFailure`.

- [ ] Every operation can fail with a `500`. Instead of annotating each method, write an `OASFilter` that adds a `500` response (with the `ApiError` schema) to all operations, and register it with the `mp.openapi.filter` property.

### Task 7 - Production behavior (no tests)
**Goal:** see what actually ships.

```powershell
.\mvnw.cmd package -DskipTests "-Dquarkus.smallrye-openapi.store-schema-directory=target/openapi"
Get-ChildItem target\openapi
java -jar target\quarkus-app\quarkus-run.jar
```

```bash
./mvnw package -DskipTests -Dquarkus.smallrye-openapi.store-schema-directory=target/openapi
ls target/openapi
java -jar target/quarkus-app/quarkus-run.jar
```

🔮 **Predict first:** with the jar running, what do `GET /q/openapi` and `GET /q/swagger-ui/` answer? Check them from another terminal:

```powershell
curl.exe -s -o NUL -w "%{http_code}`n" http://localhost:8080/q/openapi
curl.exe -s -o NUL -w "%{http_code}`n" -L http://localhost:8080/q/swagger-ui/
```

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/q/openapi
curl -s -o /dev/null -w "%{http_code}\n" -L http://localhost:8080/q/swagger-ui/
```

- [ ] Stop the jar and start it again overriding the title at runtime, then read the document again:

```powershell
java "-Dquarkus.smallrye-openapi.info-title=Staging Catalog" -jar target\quarkus-app\quarkus-run.jar
```

```bash
java -Dquarkus.smallrye-openapi.info-title="Staging Catalog" -jar target/quarkus-app/quarkus-run.jar
```

- [ ] Look at the files stored in `target/openapi`: who could use them, and in which step of a pipeline?
- [ ] Find in the guide how to make Swagger UI part of a production build, and decide when you would (not) do it.

**Success criteria:** all 53 tests pass inside `starter/` and you have done Task 7.

```powershell
.\mvnw.cmd verify
```

```bash
mvn verify
```

## 👀 What you should see
- Swagger UI lists the five operations under the `Products` tag, with `201`, `400`, `404`, `409` and `500` responses that match what the API really returns.
- The example values you wrote appear pre-filled in the request bodies.
- Constraints such as `maxLength: 100` or `maximum: 100` appear without you writing them.

## 💼 In real projects
- **The document is a contract.** Other teams generate clients from it and write code against it. A wrong document (a `200` that is really a `201`) breaks real integrations. Treat changes to it like changes to code.
- **Catch contract drift in CI.** Store the generated document at build time (Task 7), commit or publish it, and diff it against the previous version in the pipeline: breaking changes (a removed field, a new required parameter) should fail the build or need a conscious approval. The checked-in `openapi.yaml` is also what front-end teams can mock against before your service is deployed.
- **Code-first vs contract-first.** This lab is code-first: the code is the source of truth. Contract-first (write the YAML, generate interfaces) fits when several teams agree on the API before anyone codes. Know both; most Quarkus services start code-first.
- **Do not expose Swagger UI by default in production.** It is off in the production build unless you ask for it. If you need it, put it behind authentication or only on an internal network. The raw `/q/openapi` is exposed by default: decide whether that is acceptable for your API, and change its path or protect it if not.
- **Document the failure paths you actually have.** Clients write error handling from your document. Having one error component (`ApiError`) referenced everywhere keeps it consistent and short.
- **Keep descriptions and examples realistic.** Examples are copy-pasted by integrators; an `"example": "string"` teaches nothing.
- **Do not hide business rules in prose only.** Use machine-readable constraints where possible (Bean Validation shows up in the schema), and use the description for the rest ("names are unique, case-insensitive").

## 🚀 Stretch goal
1. Add an `X-Request-Id` header parameter/response header to every operation with an `OASFilter` (the filter from lab 04 sets it on every response).
2. Generate a typed client from your stored `openapi.yaml` (for example with the OpenAPI Generator CLI) and call your service with it. What did the generator get wrong because the document was vague?
3. Secure the document: add a basic security scheme to the contract (`@SecurityScheme`) even though the API is not protected yet; lab 21 will implement it.

## 💡 Hints
<details><summary>Hint 1 (direction)</summary>

- Task 2: look at `@OpenAPIDefinition` and `@Info`, and at where Quarkus expects it. A JAX-RS `Application` class may be involved.
- Task 3/4: `@Operation`, `@Parameter`, `@APIResponse` (it is repeatable), `@Header`.
- Task 4 (reuse): `@Components(responses = ...)` declares them once; `@APIResponse(ref = "Name")` references one.
- Task 5: `@Schema(description = ..., example = ...)` works on record components.
- Task 6: the interface is `OASFilter`; look at `OASFactory` and the `mp.openapi.filter` property.
</details>

<details><summary>Hint 2 (more concrete)</summary>

```java
@ApplicationPath("/")
@OpenAPIDefinition(
        info = @Info(title = "Product Catalog API", version = "1.0.0", description = "..."),
        tags = @Tag(name = "Products", description = "..."),
        components = @Components(responses = {
                @APIResponse(name = "NotFound", responseCode = "404", description = "...",
                        content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                schema = @Schema(implementation = ApiError.class))) }))
public class ApiDocumentation extends Application {
}
```

```java
@GET
@Path("/{id}")
@Operation(summary = "Get a product by id")
@APIResponse(responseCode = "404", ref = "NotFound")
public Product get(@RestPath long id) { ... }
```

```java
@APIResponse(responseCode = "201", description = "Created",
        headers = @Header(name = "Location", description = "URL of the created product",
                schema = @Schema(type = SchemaType.STRING)),
        content = @Content(schema = @Schema(implementation = Product.class)))
```

```java
public class InternalErrorResponseFilter implements OASFilter {
    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        // for every PathItem -> every Operation -> operation.getResponses().addAPIResponse("500", ...)
    }
}
```

```properties
mp.openapi.filter=lab.melostudy.InternalErrorResponseFilter
```

A schema reference in a programmatic response is `OASFactory.createSchema().ref("#/components/schemas/ApiError")`.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1.

- **Task 1 (the default document):** it is OpenAPI `3.1.0`. Summaries are just the Java method names (`List`, `Get`, `Create`, `Update`, `Delete`); the tag is `Product Resource`; the title is `openapi-lab API` (the artifact id) and the version is the Maven project version. `POST /products` is documented as **`200`** although it answers `201`, with only a body-less `400`. There is no `404` or `409` anywhere and no `ApiError` schema. `X-Total-Count`, `Location` and `X-Request-Id` are not mentioned.
- **Task 1 (what you get for free):** Bean Validation constraints are in the schema: `maxLength: 100`, `exclusiveMinimum: 0`, `multipleOf: 0.01`, `pattern` for `@NotBlank`, `required` for `@NotNull`, and `minimum`/`maximum`/`default` on query parameters.
- **Task 1 (test mode):** test-only classes appear in the document too (`/test-support/boom` from `BoomResource`). That is why the tests only look at `/products` paths.
- **Task 2:** `@OpenAPIDefinition` on a plain class is **ignored**: neither the `info` nor the reusable components took effect. It works on a class that extends `jakarta.ws.rs.core.Application` (with `@ApplicationPath`).
- **Task 7:**
  - In the packaged jar `GET /q/openapi` answers `200` (public by default) and `GET /q/swagger-ui/` answers **`404`**: Swagger UI is not part of the production build.
  - Building with `-Dquarkus.swagger-ui.always-include=true` makes `/q/swagger-ui/` answer `200` in the jar.
  - `-Dquarkus.smallrye-openapi.info-title=...` on the running jar changes the title in the served document.
  - `quarkus.smallrye-openapi.store-schema-directory=target/openapi` writes `openapi.json` and `openapi.yaml` during the build.
</details>

## 🎤 Self-assessment
Answer without looking at the code; you should be able to explain each one in an interview.

1. What is the difference between the OpenAPI document and Swagger UI? Which one is available in a production build by default?
2. Name two things the generated document gets wrong or omits when you do not annotate anything, and why Quarkus cannot know them.
3. Where do `maxLength` and `minimum` in the schema come from if you never wrote them for OpenAPI?
4. How do you avoid repeating the same error response on twenty endpoints? Give two techniques.
5. How would you stop a team from accidentally breaking API consumers when changing a resource?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand about OpenAPI in Quarkus?
- What was hard?
- What should I reinforce?
