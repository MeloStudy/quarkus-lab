# 🧱 Lab 11 - Schema migrations with Flyway

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
> **Docker (or Podman) must be running.** The tests start a real PostgreSQL in a container (Dev Services), as in labs 08 to 10. Task 4 starts one more container by hand.

## 🎯 Why it matters
Until now Hibernate built the schema from the entities. That is fine for a test database and wrong for production: `drop-and-create` would erase the data, `update` guesses, and `none` (what the jar used) leaves you with an empty database and a `relation "product" does not exist` error. A real project needs the schema to be **versioned code**: reviewed in a pull request, applied in order, applied exactly once, identical in dev, tests and production. That is a migration tool, and Flyway is the one most Quarkus teams use.

## 🧠 Concepts
- **Versioned migration** `V<version>__<description>.sql` (two underscores): runs once, in version order. Flyway records it in the table `flyway_schema_history` together with a **checksum** of the file.
- **Applied migrations are immutable.** Editing one changes its checksum and Flyway refuses to start. A change is always a *new* migration.
- **Repeatable migration** `R__<description>.sql`: runs again whenever its content changes, after all versioned ones. Good for views and for dev sample data; it must be idempotent.
- **Who owns the schema:** with Flyway, Hibernate stops generating it. `quarkus.hibernate-orm.schema-management.strategy=validate` makes Hibernate *check* at startup that entities and tables match, and fail otherwise.
- **What `validate` does not see:** indexes, check constraints and defaults. Those exist only because a migration created them. Rules the database enforces are the only ones that protect the data from every writer (scripts, other services, a bug).
- **Baseline:** adopting Flyway on a database that already exists means telling it "this database is already at version N"; see task 4.

| Where | Who builds the schema | Why |
|---|---|---|
| Before this lab (dev, test) | Hibernate (`drop-and-create`) | Fast and disposable |
| Before this lab (prod) | Nobody (`none`) | Hibernate must not touch production |
| This lab (every profile) | Flyway; Hibernate only `validate`s | Same SQL everywhere: what you test is what you deploy |

## 📚 Documentation
- [Quarkus - Flyway](https://quarkus.io/guides/flyway)
- [Quarkus - Hibernate ORM: development mode and Flyway](https://quarkus.io/guides/hibernate-orm)
- [Quarkus - Datasources](https://quarkus.io/guides/datasource)
- [Flyway documentation](https://documentation.red-gate.com/flyway)

## 🧭 Scenario
`starter/` is the lab 10 application (products, labels, reviews, orders, audit trail), unchanged, plus the `quarkus-flyway` extension. There are **no migrations yet** and the dev/test schema still comes from Hibernate. Your job is to hand the schema over to Flyway and to add the rules only a database can enforce.

The tests of labs 04 to 10 are in the project as a **regression guard**: they must stay green (and they are green in the starter). The new tests are `MigrationsTest`, `ConstraintsTest`, `DataMigrationTest` and `CategoryNormalizationTest`.

> [!NOTE]
> Lab 10 only ever worked with the schema Hibernate generated. Never change the entities' tables by hand-editing the database in this lab either: every change goes into a migration file under `src/main/resources/db/migration`.

## ✅ Tasks

### Task 1 - Flyway owns the schema
**Goal:** the schema of every profile comes from `V1__...sql`; Hibernate only validates it. **Validated by:** `MigrationsTest` (and the whole regression guard).

- [ ] In `application.properties` turn on `quarkus.flyway.migrate-at-start`, and set the Hibernate strategy to `validate` for **all** profiles (replace the two TODO lines).
- [ ] Get the DDL Hibernate would run. Do not write seven tables by hand: let Hibernate print them (the properties are in the Hibernate ORM configuration reference), run one test, and read the file it produced.

```powershell
.\mvnw.cmd test "-Dtest=ProductResourceTest" "-Dquarkus.hibernate-orm.scripts.generation=create" "-Dquarkus.hibernate-orm.scripts.generation.create-target=target/create.sql"
Get-Content target\create.sql
```

```bash
./mvnw test -Dtest=ProductResourceTest -Dquarkus.hibernate-orm.scripts.generation=create -Dquarkus.hibernate-orm.scripts.generation.create-target=target/create.sql
cat target/create.sql
```

- [ ] Create `src/main/resources/db/migration/V1__create_schema.sql` from it. **Review it, do not paste blindly**: format it, add the foreign keys, and keep the sequence definitions.
- [ ] The sample data in `import.sql` is no longer loaded (Hibernate does not generate anything now). Move it to a **repeatable migration** `R__dev_sample_data.sql` in a second folder `db/dev`, and add that folder to the Flyway locations **only in the dev profile**. Make it idempotent (`ON CONFLICT ... DO NOTHING`). Delete `import.sql`.

> [!WARNING]
> Two traps live here. One is about *which* sequences the generated script creates and how they are declared. The other is what happens if you only turn Flyway on and forget the Hibernate line.

🔮 **Predict first:** you set `migrate-at-start=true` but leave `schema-management.strategy=drop-and-create` in the test profile. Does the application start? Do the regression tests pass? What happens to the tables Flyway created?

```powershell
.\mvnw.cmd test "-Dtest=MigrationsTest"
```

### Task 2 - Rules only the database can enforce
**Goal:** a second migration with the constraints Hibernate cannot declare. **Validated by:** `ConstraintsTest`.

The application already validates its input, but a script, another service or a bug can still write bad rows. Add `V2__...sql` so that the database refuses:

- [ ] two products whose names differ only in case, **even when `name_key` is set to anything**: a unique index on `lower(name)`;
- [ ] a price that is zero or negative, and a negative stock;
- [ ] a review rating outside 1 to 5.

> [!TIP]
> `CHECK (kind IN (...))` on `review.kind` is already in your V1: Hibernate does generate a check for the discriminator column. It is the *business* checks above that it cannot guess.

🔮 **Predict first:** after adding V2, does Hibernate's `validate` notice if you *forget* the `lower(name)` index?

```powershell
.\mvnw.cmd test "-Dtest=ConstraintsTest"
```

### Task 3 - A migration that changes existing data
**Goal:** `V3` fixes legacy rows and adds a constraint on them, and the application stops producing the bad rows. **Validated by:** `DataMigrationTest`, `CategoryNormalizationTest`.

The API accepts categories in any case and with spaces (`"Books"`, `"  GAMES "`) and stores them as typed, so old rows exist with several spellings of the same category. In `V3__...sql`:

- [ ] normalize the existing categories (lower case, trimmed);
- [ ] add a `CHECK` so that only normalized categories can be stored from now on.

`DataMigrationTest` builds a throwaway schema at version 2 with legacy rows in it, applies the remaining migrations and checks the rows. It shows how to test a migration *against data* instead of an empty database.

- [ ] Run `CategoryNormalizationTest`. Read the failure carefully: the database is now stricter than the application. Fix the **application** so it stores what the constraint accepts.

> [!WARNING]
> The order of the two statements in V3 matters. Try the wrong order and read the error and what Flyway reports about the half-applied script.

```powershell
.\mvnw.cmd test "-Dtest=DataMigrationTest,CategoryNormalizationTest"
```

### Task 4 - Outside the tests: the jar, a real database and the history table
**Goal:** see Flyway where it matters. No test validates this task.

- [ ] Start a PostgreSQL and run the jar against it (the variable names follow lab 02):

```powershell
.\mvnw.cmd package
docker run -d --name lab11-pg -e POSTGRES_USER=shop -e POSTGRES_PASSWORD=pw -e POSTGRES_DB=shop -p 55433:5432 postgres:17.11
$env:QUARKUS_DATASOURCE_JDBC_URL = "jdbc:postgresql://localhost:55433/shop"
$env:QUARKUS_DATASOURCE_USERNAME = "shop"
$env:QUARKUS_DATASOURCE_PASSWORD = "pw"
java -jar target\quarkus-app\quarkus-run.jar
```

```bash
./mvnw package
docker run -d --name lab11-pg -e POSTGRES_USER=shop -e POSTGRES_PASSWORD=pw -e POSTGRES_DB=shop -p 55433:5432 postgres:17.11
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:55433/shop QUARKUS_DATASOURCE_USERNAME=shop \
QUARKUS_DATASOURCE_PASSWORD=pw java -jar target/quarkus-app/quarkus-run.jar
```

- [ ] Read the log, stop the jar and start it again. Then look at the history table (in another terminal):

```powershell
docker exec lab11-pg psql -U shop -d shop -c "select installed_rank, version, description, checksum, success from flyway_schema_history"
```

- [ ] 🔮 **Predict first:** edit **only a comment** inside `V1__create_schema.sql`, rebuild and start the jar again. Does it start? Undo the edit afterwards.
- [ ] 🔮 **Predict first:** a team already runs a database built by Hibernate (`update`), with rows in it and no history table, and now deploys this version. What does Flyway do? Reproduce it with a second database, then find the two settings that adopt it without losing data (`baseline`):

```powershell
docker exec lab11-pg psql -U shop -d shop -c "create database legacy"
```

Point the jar at `.../legacy`, first with `QUARKUS_FLYWAY_MIGRATE_AT_START=false` and `QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY=update` to create a Hibernate-built schema, insert a row with category `Books` through `psql`, and then start it normally.

- [ ] Clean up when finished (stop the jar with Ctrl+C first):

```powershell
docker rm -f lab11-pg
Remove-Item Env:QUARKUS_DATASOURCE_JDBC_URL, Env:QUARKUS_DATASOURCE_USERNAME, Env:QUARKUS_DATASOURCE_PASSWORD
```

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green.

## 👀 What you should see
The first start of the jar on an empty database:

```text
Schema history table "public"."flyway_schema_history" does not exist yet
Migrating schema "public" to version "1 - create schema"
Migrating schema "public" to version "2 - sql level constraints"
Migrating schema "public" to version "3 - normalize categories"
Successfully applied 3 migrations to schema "public", now at version v3
```

In dev mode (`quarkus:dev`) there is a fourth step, the repeatable migration with the sample data, and `GET /products` returns the five sample products.

## 💼 In real projects
- **Never edit an applied migration**, not even a comment or whitespace. If it was wrong, add a new migration that fixes it. In a shared team branch, two people creating `V4` at the same time is a normal merge conflict: renumber before merging.
- **One change per migration**, with a name that says what and why. Review migrations like code: they are the only record of how production got its shape.
- **Make destructive changes in steps** ("expand and contract"): add the new column, deploy code that writes both, backfill, switch reads, and only a release later drop the old one. A rename or a `NOT NULL` added to a big table in one step locks the table or breaks the running version during a rolling deploy.
- **`migrate-at-start` is convenient, not always right.** A slow migration delays every start-up and a failed one prevents the whole service from booting (task 4 shows it). Teams with big tables run migrations as a separate step of the pipeline (a job before the deploy) and keep the application on `validate`.
- **`clean` wipes the database.** Keep it disabled everywhere except throwaway databases, and never enable it through configuration in production.
- **Test migrations against data**, not only against an empty schema (task 3). A migration that works on an empty database and fails on the production copy is the classic incident.
- **Roll forward, do not roll back.** Fix mistakes with a new migration, and take a backup before a risky one.
- **Database credentials** are environment variables or a secret, never in the repository (lab 22 goes deeper).

## 🚀 Stretch goal
The column names `createdAt`, `productId`, `newPrice`... are what Hibernate generated in lab 10, so PostgreSQL folds them to `createdat`, `productid`... Write `V4` that renames them to `created_at`, `product_id`, ... and change the entities (`@Column(name = ...)`) in the same commit. Which tests catch a mismatch, and what does `validate` say if you forget one side? No hints.

## 💡 Hints
<details><summary>Hint 1 (task 1)</summary>

Properties to look for: `quarkus.flyway.migrate-at-start`, `quarkus.hibernate-orm.schema-management.strategy`, and `quarkus.flyway.locations` with a `%dev.` prefix. `locations` replaces the default (`db/migration`), so the dev profile must list both folders.
</details>

<details><summary>Hint 2 (task 1)</summary>

Panache entities (`extends PanacheEntity`) take their ids from a sequence named `<table>_SEQ`. Look at the `increment by` of the generated script. If you want to know what `validate` compares, break one thing on purpose and read the error.
</details>

<details><summary>Hint 3 (task 2)</summary>

`CREATE UNIQUE INDEX ... ON product (lower(name))` and `ALTER TABLE ... ADD CONSTRAINT ... CHECK (...)`. The test looks at the SQLSTATE (`23505` unique violation, `23514` check violation), so your constraint names are up to you.
</details>

<details><summary>Hint 4 (task 3)</summary>

`UPDATE` first, then `ALTER TABLE ... ADD CONSTRAINT ... CHECK (category = lower(trim(category)))`. In the application, the entity already has a `@PrePersist`/`@PreUpdate` hook that derives `nameKey`; that is where derived values are computed.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1 (Flyway through the `quarkus-flyway` extension, PostgreSQL 17 and the Dev Services image).

**Task 1.**
- With `migrate-at-start=true` and `strategy=drop-and-create` left in place, the application **starts** and the regression tests pass, so nothing looks wrong. But Hibernate drops and recreates the tables *after* Flyway built them, so the result has none of the rules of V2 and V3: with that setting `ProductResourceTest` stays green and **all 4 tests of `ConstraintsTest` fail**. This is why `MigrationsTest` checks the configured strategy: green tests alone do not prove who built the schema.
- With the Flyway extension on the classpath and **no** strategy configured, the effective value in tests was `none`: Hibernate stops generating the schema by itself. In the starter it is set explicitly to `drop-and-create` for exactly that reason.
- The sequence trap: `PanacheEntity` ids use sequences named `<table>_SEQ` with `increment by 50`. If V1 declares one with the default increment 1, `validate` stops the application with `Schema validation: sequence [label_SEQ] defined inconsistent increment-size; found [1] but expecting [50]`.
- The generated script uses unquoted camelCase names (`createdAt`), which PostgreSQL stores as `createdat`. That is legal and `validate` accepts it; renaming is the stretch goal.
- Dev mode: Flyway applies V1 to V3 and then the repeatable `R__dev_sample_data.sql`; `GET /products` returns the five sample products.

**Task 2.**
- `validate` does **not** notice a missing index or check constraint: it only compares tables, columns, types and sequences. `ConstraintsTest` is what notices.
- `review.kind` already has `CHECK (kind IN ('CRITIC','CUSTOMER'))` in the script Hibernate generates.

**Task 3.**
- Adding the `CHECK` before the `UPDATE` fails with `check constraint "product_category_normalized" of relation "product" is violated by some row`, and Flyway logs `Migration of schema "public" to version "3 - normalize categories" failed! Changes successfully rolled back.` Nothing was left half applied: on PostgreSQL the whole script runs in one transaction.
- With V3 in place and the entity unchanged, `POST /products` with category `"Books"` answers `500` (the catch-all of lab 05, body `An unexpected error occurred`) and the log shows `new row for relation "product" violates check constraint "product_category_normalized"`. The constraint found a latent bug: the application stored what it received. The fix is to normalize in the entity hook (`category = category.trim().toLowerCase(Locale.ROOT)`).
- `DataMigrationTest` uses its own schema (`migration_scratch`) and drops it afterwards; the schema of the application is never touched, so it does not interfere with the other tests.

**Task 4.**
- A fresh database: the log shows the three migrations and `flyway_schema_history` holds three rows with `success = t` and one checksum each. A second start logs `Schema "public" is up to date. No migration necessary.`
- Editing even a comment of the applied V1 makes the jar fail at startup: `FlywayValidateException: Validate failed: Migrations have failed validation` / `Migration checksum mismatch for migration version 1` / `Applied to database : 1805062355` / `Resolved locally : 730694001` (the numbers depend on the file).
- A non-empty database without history table: `FlywayException: Found non-empty schema(s) "public" but no schema history table. Use baseline() or set baselineOnMigrate to true to initialize the schema history table.`
- With `QUARKUS_FLYWAY_BASELINE_ON_MIGRATE=true` and `QUARKUS_FLYWAY_BASELINE_VERSION=1`, Flyway logs `Successfully baselined schema with version: 1`, **skips V1**, applies V2 and V3, and the legacy rows (`Books`, `  GAMES `) come back as `books` and `games`. The history table shows `<< Flyway Baseline >>` as version 1. Choosing the baseline version is the real decision: it must be the version whose schema the database really has.
</details>

## 🎤 Self-assessment
1. Why is `drop-and-create` acceptable in a test and wrong in production, and what replaces it?
2. A teammate fixed a typo inside `V5__add_index.sql`, which is already deployed. What happens at the next start, and what should they have done?
3. What does `schema-management.strategy=validate` check, and what can it never tell you?
4. You must add a `NOT NULL` column to a table with 50 million rows in a service that deploys without downtime. Describe the migrations and the releases.
5. A database has been running for two years without Flyway. How do you adopt it, and how do you pick the baseline version?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
