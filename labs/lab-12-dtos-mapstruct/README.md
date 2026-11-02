# 🪞 Lab 12 - DTOs and MapStruct

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
> **Docker (or Podman) must be running.** The tests start a real PostgreSQL in a container (Dev Services), as in labs 08 to 11.

## 🎯 Why it matters
So far the JSON the API returns *is* the JPA entity, held together with `@JsonIgnore` and `@JsonProperty` annotations. It works until someone adds a column. The business now wants to track what the shop pays for each product (`cost_price`): one new field in the entity and the purchase cost is public, to every client, with no code review noticing, because nothing in the diff touches the API. Separating the **persistence model** (entities) from the **API model** (DTOs) makes every exposed property a deliberate decision, lets both evolve independently, and keeps lazy relationships from leaking into serialization.

## 🧠 Concepts
- **Entity vs DTO:** the entity is shaped by the database; the DTO is shaped by the API contract. Java records are a natural fit for DTOs.
- **MapStruct:** an *annotation processor*. At compile time it generates plain Java that copies properties (`ProductMapperImpl`), with no reflection and no runtime magic. You can read the generated code, step through it, and mistakes are compile errors.
- **`componentModel`:** how the generated implementation is created. For Quarkus it must be a CDI bean so you can `@Inject` it.
- **`unmappedTargetPolicy = ERROR`:** a target property that nobody maps is a build error instead of a silent `null`. This is the safety net that makes mappers trustworthy.
- **Where to map:** a mapper *reads* the entity, including lazy relationships. Reading them after the Hibernate session is gone fails (lab 09); decide on purpose where the mapping runs.

| | Entity | DTO |
|---|---|---|
| Shaped by | Tables and relationships | The API contract |
| Changes when | The schema changes | The API changes (versioned) |
| Carries | Lazy relationships, internal columns | Only what clients may see |
| Annotations | JPA | JSON, validation |

## 📚 Documentation
- [MapStruct reference guide](https://mapstruct.org/documentation/stable/reference/html/)
- [MapStruct - installation (Maven)](https://mapstruct.org/documentation/installation/)
- [Quarkus - Hibernate ORM with Panache](https://quarkus.io/guides/hibernate-orm-panache)
- [Quarkus - Writing REST services](https://quarkus.io/guides/rest)

> [!NOTE]
> **MapStruct is not managed by the Quarkus BOM** (checked in `quarkus-bom-3.40.1.1.pom`), so this lab is the exception to the "no explicit versions" rule. Justification: it is the de facto standard mapper, it is a compile-time processor with no runtime dependency beyond a small annotations jar, and Quarkus works with it through plain CDI. The version is pinned in one property (`mapstruct.version`, **1.6.3**, the latest stable release when this lab was written; 1.7 was still in beta).

## 🧭 Scenario
`starter/` is the lab 11 application plus a new migration `V4__add_product_cost_price.sql` and the entity field `Product.costPrice`. Run the new tests and see the leak: the product JSON now contains `costPrice`. Your job is to put DTOs between the entities and the API, generate the mapping with MapStruct, and leave the entities knowing nothing about JSON.

The tests of labs 04 to 11 are a **regression guard** (green in the starter, they must stay green). The new ones are `ApiShapeTest`, `EntitiesAreNotTheApiTest` and `MapperBeansTest`.

```powershell
.\mvnw.cmd test "-Dtest=ApiShapeTest"
```

```bash
./mvnw test -Dtest=ApiShapeTest
```

## ✅ Tasks

### Task 1 - MapStruct and the product DTO
**Goal:** stop the leak: the product endpoints return a `ProductResponse`, mapped by a generated `ProductMapper`. **Validated by:** `ApiShapeTest` (product tests), `MapperBeansTest`.

- [ ] Add MapStruct to `pom.xml`: the `org.mapstruct:mapstruct` dependency and, in `maven-compiler-plugin`, an `annotationProcessorPaths` entry for `org.mapstruct:mapstruct-processor`, both with `${mapstruct.version}` = `1.6.3`.
- [ ] Create the record `ProductResponse` with exactly the properties the API had before `costPrice` existed: `id`, `name`, `category`, `price`, `stock` and `labels` (a list of label names).
- [ ] Create the interface `ProductMapper` (that exact name) annotated with `@Mapper`, as a CDI bean, with `unmappedTargetPolicy = ReportingPolicy.ERROR`. Labels are a `Set<Label>` in the entity and a sorted `List<String>` in the DTO: write that conversion as a `default` method of the mapper.
- [ ] Inject the mapper in `ProductResource` and return `ProductResponse` from `get`, `list`, `create`, `update` and `setLabels`.
- [ ] Remove `labelNames()` and every `@Json...` annotation from `Product`.

> [!WARNING]
> Two ways to get this wrong without any compile error. One is in the `pom.xml`: leave out one of the two pieces and the project still compiles. The other is the property list of the DTO: the test fails in a different way depending on whether you forget one or add one.

🔮 **Predict first:** you add the `mapstruct` dependency but not the `annotationProcessorPaths`. Does `mvn compile` fail? Does `mvn test`? Where and with which message?

```powershell
.\mvnw.cmd test "-Dtest=ApiShapeTest,MapperBeansTest"
```

### Task 2 - Reviews, price history and orders
**Goal:** no entity is left in the API, including the polymorphic ones. **Validated by:** `EntitiesAreNotTheApiTest`, `ApiShapeTest`, `MapperBeansTest`.

- [ ] `ReviewResponse`: a sealed interface with two records (`Customer`, `Critic`) and the Jackson `type` property (`CUSTOMER` / `CRITIC`). The Jackson annotations now live **on the DTO**; remove them from `Review`.
- [ ] `ReviewMapper` that maps each concrete entity to its record (`@SubclassMapping`). `Review` is abstract, and so is the target: MapStruct will complain until you tell it what to do when a subclass is not covered.
- [ ] `PriceChangeResponse` and `OrderResponse` (records), mapped by `ProductMapper` and a new `OrderMapper`.
- [ ] No method of any resource may take or return an entity. `EntitiesAreNotTheApiTest` checks it by reflection, including `List<...>` and `RestResponse<...>`.

> [!TIP]
> The generated code is plain Java in `target/generated-sources/annotations`. When a mapping surprises you, read it before searching the web.

```powershell
.\mvnw.cmd test "-Dtest=EntitiesAreNotTheApiTest,ApiShapeTest,MapperBeansTest"
```

### Task 3 - Compile-time safety and where to map
**Goal:** make the mappers fail loudly, and understand when they may read the entity. **Validated by:** the whole regression guard.

- [ ] Use the mapper in the other direction too: `ProductInput` to `Product`, for `create` (new entity) and `update` (`@MappingTarget`, copying onto the loaded entity). With the policy at `ERROR`, the compiler will list everything in `Product` that the input does not provide: decide for each (`ignore = true`) and write it down.
- [ ] Add `public String sku;` to the `Product` entity (do not commit it). Does the build still pass? Then add a `String sku` to `ProductResponse` instead. Compare. Remove both.
- [ ] `ProductRepository.update` calls `Hibernate.initialize(product.labels)` so the labels can be read after the transaction. Your mapper runs in the resource, after `update` returned. 🔮 **Predict first:** what happens if you delete that line? Try it, and put it back.

> [!WARNING]
> A mapper is just code that reads entity properties. If one of them is a lazy collection and the session is closed, the mapper throws exactly as serialization did in lab 09.

```powershell
.\mvnw.cmd test
```

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green.

## 👀 What you should see
After task 1, the product JSON again has six properties:

```json
{"id":1,"name":"Clean Code","category":"books","price":30.50,"stock":10,"labels":[]}
```

The generated implementation is a `@ApplicationScoped` class you can open:

```powershell
Get-Content target\generated-sources\annotations\lab\melostudy\ProductMapperImpl.java -TotalCount 40
```

```bash
head -40 target/generated-sources/annotations/lab/melostudy/ProductMapperImpl.java
```

## 💼 In real projects
- **Version the DTO, not the entity.** When the API must change incompatibly, you add `ProductResponseV2` and a mapper; the table does not care.
- **Input and output DTOs are different things.** `ProductInput` (what a client may send) and `ProductResponse` (what it receives) have different fields. Binding request bodies directly to entities invites *mass assignment*: a client sets `id`, `stock` or any other public field the entity has.
- **Keep `unmappedTargetPolicy = ERROR`** (or set it once for the whole project with a compiler argument). The default is to ignore, and ignored properties are how a `null` reaches production.
- **Mappers are not business logic.** Copy and convert; calculations, defaults and rules belong in the domain code, where they are tested. A mapper full of `expression = "java(...)"` is a service in disguise.
- **Where to map is an architecture decision.** Mapping in the resource is simple, but lazy collections force you to initialize them before the transaction ends (as `update` does here). The alternative is a transactional service that returns DTOs: the entities never leave it, at the price of the service knowing the DTOs. Either way, never turn on "open session in view" to hide the problem.
- **Do not map entity graphs blindly.** A bidirectional relationship (`Product.reviews` and `Review.product`) mapped both ways recurses. Map the side the client needs.
- **Alternatives:** hand-written `static` mapping methods are fine for a few fields and have zero dependencies; reflection-based mappers (ModelMapper) fail at runtime instead of compile time and are slower.

## 🚀 Stretch goal
Add a `GET /products/{id}/summary` that returns a DTO built from **two** sources (the product and its review summary), using a MapStruct method with two parameters. Then write a test for the mapper alone, without starting Quarkus. No hints.

## 💡 Hints
<details><summary>Hint 1 (direction, task 1)</summary>

Properties of the `@Mapper` annotation: `componentModel` (look at `MappingConstants.ComponentModel`) and `unmappedTargetPolicy`. A `default` method in the interface is a hand-written conversion that MapStruct will call when types match.
</details>

<details><summary>Hint 2 (task 1, pom)</summary>

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>
```

and inside `maven-compiler-plugin` / `configuration`:

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
    </path>
</annotationProcessorPaths>
```
</details>

<details><summary>Hint 3 (task 2)</summary>

`@SubclassMapping(source = CustomerReview.class, target = ReviewResponse.Customer.class)` once per subclass, on the method that takes a `Review`. The error about an abstract return type is solved with an attribute of `@Mapper` whose name starts with `subclass`.
</details>

<details><summary>Hint 4 (task 3)</summary>

For the mapper that fills an existing entity, the entity is a method parameter annotated with `@MappingTarget` and the method returns `void`. `@Mapping(target = "...", ignore = true)` is repeatable.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1 with MapStruct 1.6.3.

**Task 1.**
- The leak in the starter: the product JSON contains `costPrice`. The test reports `Expected: iterable with items ["id", "name", "category", "price", "stock", "labels"] in any order` and `Actual: <[id, name, category, price, stock, costPrice, labels]>`.
- Dependency without `annotationProcessorPaths`: `mvn compile` **passes** (nothing generates `ProductMapperImpl`, but nothing needs it at compile time, because the code only refers to the interface). The failure comes when the application starts: `Unsatisfied dependency for type lab.melostudy.ProductMapper and qualifiers [@Default]` (and the same for the other mappers): the application does not start, so the `@QuarkusTest` classes fail, not only the new ones.
- `componentModel = "cdi"` (the plain string) also works with 1.6.3: the generated class imports `jakarta.enterprise.context.ApplicationScoped`. The constant `MappingConstants.ComponentModel.JAKARTA_CDI` says it explicitly and is what the solution uses.
- A property in the DTO that nobody maps stops the build: `Unmapped target property: "sku".`

**Task 2.**
- Mapping `Review` (abstract) to `ReviewResponse` (interface) with only `@SubclassMapping` fails to compile: `The return type ReviewResponse is an abstract class or interface. Provide a non abstract / non interface result type or a factory method.` The fix is `subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION` on the `@Mapper`: a `Review` of an unexpected subclass throws at runtime instead of being silently mapped.
- Moving the Jackson annotations from the entity to `ReviewResponse` keeps the JSON properties identical: `type`, the common ones, and `verifiedPurchase` or `outlet` depending on the kind (`ApiShapeTest` checks the exact set).

**Task 3.**
- A new field `sku` in `Product` breaks the build, but only because the two `ProductInput` to `Product` methods have `Product` as a target: `Unmapped target property: "sku".` The response mapper does not notice (an unmapped *source* property is ignored by default). The input direction is where the policy protects you from forgetting a field; the output direction is protected by the DTO itself.
- Deleting `Hibernate.initialize(product.labels)` from `update` makes the `PUT /products/{id}` calls answer `500`, and the log shows `LazyInitializationException: Cannot lazily initialize collection of role 'lab.melostudy.Product.labels' with key '4' (no session)`. The read endpoints worked in this application without that line (their tests pass), so the failure is easy to miss until an update is tested.
</details>

## 🎤 Self-assessment
1. Give two concrete problems of returning JPA entities from a REST endpoint.
2. MapStruct or reflection-based mappers (ModelMapper, Dozer): what changes when a property is renamed or forgotten?
3. What does `unmappedTargetPolicy = ERROR` protect you from, and why is the input direction (DTO to entity) where it matters most?
4. A mapper called from the resource throws `LazyInitializationException`. Why, and what are two ways to fix it (and one you should not use)?
5. How would you map a polymorphic hierarchy (two review kinds) to DTOs, and what happens if someone adds a third kind and forgets the mapper?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
