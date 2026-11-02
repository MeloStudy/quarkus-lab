# 🔗 Lab 09 - Relationships and inheritance

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
> **Docker (or Podman) must be running.** The tests start a real PostgreSQL in a container (Dev Services), as in lab 08.

## 🎯 Why it matters
Real data is never one table. Products have labels, reviews belong to a product, reviews come in kinds. Mapping these relationships is easy to get *almost* right: the code compiles, the happy path works, and then you find an extra table nobody asked for, a `500` on an endpoint you did not touch, a `DELETE` that fails on a foreign key, or a list endpoint that runs one query per row. This lab is about mapping them on purpose and noticing those four failures before production does.

## 🧠 Concepts
- **Owning side vs inverse side.** In a bidirectional relationship exactly one side owns the database column(s). The other side says `mappedBy = "..."` and is just a read view.
- **`@ManyToMany`** needs a join table (`@JoinTable`) declared on the owning side. **`@OneToMany(mappedBy = ...)`** plus **`@ManyToOne`** is the usual way to model "a product has many reviews": the foreign key lives in the `review` table.
- **Cascade and orphan removal:** `cascade` propagates operations (persist, remove...) from parent to children; `orphanRemoval` deletes a child removed from the parent's collection.
- **Lazy loading:** collections are loaded on first access, and only while the Hibernate *session* is open. The REST layer serializes your return value *after* your `@Transactional` method has ended.
- **N+1:** one query for the list plus one per row for a lazy association. Batch fetching loads many collections in one statement.
- **Inheritance strategies:**

| Strategy | Tables | Polymorphic query | Cost |
|---|---|---|---|
| `SINGLE_TABLE` (default) | One table for the whole hierarchy, a *discriminator* column tells the kinds apart | One `SELECT`, no joins | Subclass columns must be nullable |
| `JOINED` | One table per class, joined by id | Joins on every query | Normalized, slower |
| `TABLE_PER_CLASS` | One full table per concrete class | `UNION` | Rarely a good idea |

## 📚 Documentation
- [Hibernate ORM in Quarkus](https://quarkus.io/guides/hibernate-orm)
- [Simplified Hibernate ORM with Panache](https://quarkus.io/guides/hibernate-orm-panache)
- [Hibernate ORM documentation](https://hibernate.org/orm/documentation/) (the user guide has the chapters on associations, fetching and inheritance)
- [Jakarta Persistence 3.2](https://jakarta.ee/specifications/persistence/3.2/)

## 🧭 Scenario
The catalog of lab 08 grows three features. Everything from labs 04-08 must keep working (their tests are carried over as regression guards).

| Feature | Endpoint | Mapping |
|---|---|---|
| Labels | `PUT /products/{id}/labels` (replaces the set), `GET /products?label=sale` | Product ↔ Label, many-to-many |
| Reviews | `POST` / `GET /products/{id}/reviews`, `GET /products/{id}/review-summary` | Product → Review, one-to-many |
| Two kinds of review | `type` is `CUSTOMER` (optional `verifiedPurchase`) or `CRITIC` (required `outlet`) | `Review` with two subclasses |

```mermaid
erDiagram
    PRODUCT ||--o{ REVIEW : "has (review.product_id)"
    PRODUCT }o--o{ LABEL : "product_label"
    REVIEW <|-- CUSTOMER_REVIEW : kind = CUSTOMER
    REVIEW <|-- CRITIC_REVIEW : kind = CRITIC
```

Already provided in `starter/`: the `Label` entity, the whole REST layer, `ReviewRepository`, the label logic of `ProductRepository`, the JSON side of the review types (Jackson `type` property) and the tests. What is missing is **the mapping**. The relationship fields are marked `@Transient` (Hibernate ignores them) and carry a `// TODO` that says what to map.

Run the tests once before touching anything:

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

You should see 87 tests and 11 failures, all in `LabelsTest`, `ReviewsTest` and `FetchingTest`.

## ✅ Tasks

### Task 1 - Many-to-many: labels
**Goal:** a product has many labels and a label belongs to many products, stored in the join table `product_label`. **Validated by:** `LabelsTest`.

- [ ] In `Product`, replace the `@Transient` on `labels` with the right mapping. `Product` is the owning side, so the join table is declared here (`product_id` and `label_id` columns).
- [ ] Run `LabelsTest`.

```powershell
.\mvnw.cmd test "-Dtest=LabelsTest"
```

```bash
./mvnw test -Dtest=LabelsTest
```

> [!WARNING]
> After this task, run the **whole** suite again, not just `LabelsTest`. Something you did not touch will turn red. Do not fix it yet: it is task 5.

🔮 **Predict first:** the `Set<Label>` is lazy. Which existing endpoint returns a `Product` from a `@Transactional` method and will therefore serialize it *after* the transaction ended?

### Task 2 - One-to-many and many-to-one: reviews
**Goal:** reviews belong to one product; the foreign key is `review.product_id`. **Validated by:** `ReviewsTest` (all but the `kind` and the delete tests).

- [ ] In `Review`, map `product` as the owning many-to-one (required, lazy, column `product_id`).
- [ ] In `Product`, map `reviews` as the inverse side. Decide `cascade` and `orphanRemoval`.

🔮 **Predict first:** if you forget `mappedBy` on the `@OneToMany`, the tests may still pass. What would Hibernate create that you did not ask for? Try it, then list the tables of the test database (the query is in "Verified answers").

> [!WARNING]
> A bidirectional relationship has two sides in memory. `ReviewRepository.add` sets `review.product` **and** adds the review to `product.reviews`. Which of the two writes the foreign key?

### Task 3 - Inheritance
**Goal:** customer and critic reviews live in the **same** table `review`, told apart by a column named `kind` with the values `CUSTOMER` and `CRITIC`. **Validated by:** `ReviewsTest.bothKindsShareOneTableToldApartByTheKindColumn`.

- [ ] In `Review`, choose the strategy and declare the discriminator column.
- [ ] In each subclass, declare its discriminator value.

🔮 **Predict first:** the subclasses add one field each (`verifiedPurchase`, `outlet`). With a single table, can you declare those columns `NOT NULL`? What do the rows of the *other* kind contain in them?

### Task 4 - Delete a product
**Goal:** `DELETE /products/{id}` removes the product together with its reviews and its label links, and leaves the labels themselves for the other products. **Validated by:** `ReviewsTest.deletingAProductDeletesItsReviewsAndLabelLinksToo`, `LabelsTest.deletingAProductKeepsTheLabelForTheOthers`.

- [ ] Run the suite. The delete test fails with a `500`. Read the log: which constraint complains and why?
- [ ] Fix `ProductRepository.delete`.

> [!WARNING]
> `delete("id", id)` and `deleteById(id)` look alike, but they are different operations. One is a single `DELETE ... WHERE` statement sent to the database; the other loads the entity and removes it through the persistence context. Only one of them applies your `cascade` settings.

### Task 5 - Lazy loading and N+1
**Goal:** the endpoints that return a `Product` work again, and listing products with their labels does not cost one query per product. **Validated by:** `ProductResourceTest`, `ValidationTest`, `PriceHistoryTest`, `FetchingTest` and the other carried-over tests that update a product.

- [ ] Find out why `PUT /products/{id}` now answers `500`. The log has the exception name and the property that failed to load. Fix it in `ProductRepository.update`.
- [ ] `FetchingTest` counts SQL statements for a page of five products. Find what limits the batching (look at `application.properties`) and fix it.

🔮 **Predict first:** how many SQL statements does `GET /products?size=10` run for 5 products that each have labels? Write a number, then run `FetchingTest` and read the assertion.

## 👀 What you should see
Full suite of the solution:

```text
Tests run: 87, Failures: 0, Errors: 0, Skipped: 0
```

## 💼 In real projects
- **Entities are not API objects.** Here `Product` is returned directly, so a lazy collection can blow up inside Jackson. A DTO that you fill *inside* the transaction removes the whole class of problem (lab 12).
- **Count queries in tests.** `QueryCounter` reads Hibernate statistics (`quarkus.hibernate-orm.statistics=true` in the test profile). A test that asserts "at most N statements" is the cheapest N+1 alarm a team can have. It must be test-only: statistics cost memory in production.
- **`CascadeType.ALL` and `REMOVE` are a design decision**, not a default. Reviews die with their product (they have no life of their own); labels do not (they are shared), so `Label` is never cascaded.
- **Bulk statements bypass the persistence context.** `delete(...)` with a query and `update(...)` are fast and skip cascades, lifecycle callbacks (`@PreUpdate`) Use them knowingly, for maintenance jobs, not for ordinary domain operations.
- **Concurrent creation of a new label.** Two requests that create the same new label at the same moment can make one of them fail on the unique constraint (a `500`). `setLabels` does not handle it; a real service retries or uses `INSERT ... ON CONFLICT DO NOTHING`.
- **Constraints belong in migrations.** The `kind` column, the foreign keys and the join table are generated by Hibernate here. In production the schema comes from migrations (lab 11), and `NOT NULL` for subclass columns would need a `CHECK` constraint tied to `kind`.

## 🚀 Stretch goal
1. Switch the hierarchy to `JOINED`, run the tests, and list the tables. What changed in `ReviewRepository.of` and in the SQL?
2. Write a query that loads a page of products **with** their labels using `join fetch` and paging, and find out how Hibernate behaves. Compare with batch fetching.
3. Add a `reviewCount` to the product JSON without loading the reviews collection.

## 💡 Hints
<details><summary>Hint 1 (direction)</summary>

- Task 1: `@ManyToMany` + `@JoinTable(name = ..., joinColumns = ..., inverseJoinColumns = ...)`.
- Task 2: `@ManyToOne(fetch = ..., optional = ...)` + `@JoinColumn`, and `@OneToMany(mappedBy = "product", ...)`.
- Task 3: `@Inheritance`, `@DiscriminatorColumn`, `@DiscriminatorValue`.
- Task 4: Panache has an operation that removes **one entity**; use that one.
- Task 5: `org.hibernate.Hibernate` has a method that initializes a lazy association explicitly. And `quarkus.hibernate-orm.fetch.batch-size`.
</details>

<details><summary>Hint 2 (more concrete)</summary>

- Task 2: `cascade = CascadeType.ALL, orphanRemoval = true` on the product side; `FetchType.LAZY` and `optional = false` on the review side.
- Task 3: `InheritanceType.SINGLE_TABLE`, discriminator column `kind` of type `STRING`.
- Task 5: initialize `product.labels` inside the transaction, before the method returns.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1 (Hibernate ORM 7.4).

**Task 1 / task 5 - why `PUT /products/{id}` returned `500`.** Mapping `labels` as a lazy collection made the update endpoint fail. The log says:

```text
org.hibernate.LazyInitializationException: Cannot lazily initialize collection of role 'lab.melostudy.Product.labels' with key '4' (no session)
```

`update` is `@Transactional` and returns the entity; the transaction (and its session) ends when the method returns; then Jackson calls `labelNames()` and touches the unloaded collection. The `GET` endpoints did not fail in these runs: they are not `@Transactional`, and Quarkus keeps a session open for the request. The fix in the solution is `Hibernate.initialize(product.labels)` inside the transaction. The same exception reproduces anywhere you hold an entity after its transaction: loading a product inside `QuarkusTransaction.requiringNew().call(...)` and calling `labelNames()` afterwards throws it too.

**Task 2 - the forgotten `mappedBy`.** Removing `mappedBy = "product"` from `@OneToMany` makes Hibernate create a *third* table, `product_review`, besides `review.product_id`:

```text
[label, price_change, product, product_label, product_review, review]
```

The query used to list them (run it through `em.createNativeQuery(...)` in a test, or in `psql`):

```sql
select table_name from information_schema.tables where table_schema = 'public' order by 1
```

Both sides then believe they own the relationship. The tests still pass; only the schema shows it. (If your `TestData` truncates a fixed list of tables, the extra table's foreign key makes `TRUNCATE` fail with `cannot truncate a table referenced in a foreign key constraint`.)

**Task 3 - `SINGLE_TABLE` columns.** Columns of the `review` table (names are lower-cased by PostgreSQL) and their nullability:

```text
rating NO, createdat NO, id NO, product_id NO, kind NO, comment NO, verifiedpurchase YES, outlet YES
```

Rows: `[CRITIC, NYT]` for a critic review (`verifiedpurchase` is null) and `[CUSTOMER, null]` for a customer review (`outlet` is null). Subclass columns must be nullable because the other kind's rows leave them empty; a rule such as "critics must have an outlet" is enforced by bean validation here (`ReviewInput.Critic`), and by a `CHECK` constraint if you want the database to enforce it.

**Task 4 - bulk delete.** `delete("id", id)` on a product that has reviews fails at the database:

```text
ERROR: update or delete on table "product" violates foreign key constraint "fk..." on table "review"
```

The constraint name is generated by Hibernate. The product's `product_label` rows did **not** cause a failure: in this version a bulk HQL delete of an entity also clears the join table that entity owns. `deleteById(id)` loads the entity and calls `remove`, so `cascade = ALL` removes the reviews first.

**Task 5 - N+1.** With nothing configured, listing 5 products with labels ran **3** statements (the page, the count, one batched load of all the labels). With `quarkus.hibernate-orm.fetch.batch-size=1` it ran **7**: the page, the count, and one `SELECT` per product. The property can also be overridden from the command line without editing files:

```powershell
.\mvnw.cmd test "-Dtest=FetchingTest" "-Dquarkus.hibernate-orm.fetch.batch-size=1"
```

```bash
./mvnw test -Dtest=FetchingTest -Dquarkus.hibernate-orm.fetch.batch-size=1
```

That run fails; without the override it passes. The starter ships with `batch-size=1`; removing the line (or raising it) fixes `FetchingTest`.

**Invalid review types.** An unknown or missing `type` is rejected before validation, with the lab 05 shape: `400 MALFORMED_REQUEST`. A known type with bad data (`rating` 0, a critic without `outlet`) gives `400 VALIDATION_ERROR` with the field names.
</details>

## 🎤 Self-assessment
1. In a bidirectional relationship, which side writes the foreign key, and what does `mappedBy` tell Hibernate?
2. Why can a `@Transactional` method that returns an entity still end in a `LazyInitializationException`, and give two different ways to avoid it.
3. What is the N+1 problem, how would you detect it in a test, and what are two ways to fix it?
4. `SINGLE_TABLE` or `JOINED` for a hierarchy with many subclass-specific columns? What does each cost?
5. Why does `delete("id", id)` fail where `deleteById(id)` works, and what else does a bulk statement skip?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
