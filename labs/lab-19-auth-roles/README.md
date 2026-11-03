# 🔐 Lab 19 - Authentication and roles

| ⏱️ Duration | 🚀 Quarkus | ☕ Java | 🧪 Type |
|---|---|---|---|
| 1-2 h | 3.40.1.1 LTS | 21 | A (starter + solution) |

## 📍 Where to run commands
Open a terminal **inside this lab's `starter/` folder** and check where you are before running anything:

```powershell
Get-Location
```

```bash
pwd
```

> [!NOTE]
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead. No Docker is needed in this lab: the app has no database.

## 🎯 Why it matters
Every real service answers two questions on every request: **who are you** (authentication) and **may you do this** (authorization). Getting them wrong is not a bug, it is a breach: a forgotten endpoint that is open, a 403 that confirms a secret exists, a test suite that never proves the doors are closed. This lab builds the basic machinery with HTTP Basic and the embedded users of Quarkus, and above all the habits that will not change when labs 20 and 21 replace Basic with tokens: closed by default, roles on methods, ownership checked in code, and tests that prove it.

## 🧠 Concepts
- **Authentication** (who are you) and **authorization** (what may you do) are different steps with different failures: **401** "I do not know who you are" (the answer carries a `WWW-Authenticate` challenge), **403** "I know you and the answer is no", **404** "there is nothing here for you".
- An **authentication mechanism** reads the credentials from the request (here, the `Authorization: Basic` header). An **identity provider** checks them against a user store (here, the embedded realm that reads `application.properties`). Both are needed. Production uses another provider (database, OIDC: labs 20 and 21).
- The result is a `SecurityIdentity`: a principal (the name) and a set of **roles**. It can be injected anywhere.
- **Where to write the rules:** annotations on the code (`@RolesAllowed`, `@Authenticated`, `@PermitAll`, `@DenyAll`) for *which roles may call this method*; code that asks the identity for *whose data this is*; **path policies** in configuration for what no annotation can reach (static files, other extensions' endpoints).
- **`@TestSecurity`** installs an identity in a test without logging in. It is the quick way to test authorization and it cannot test authentication.

## 📚 Documentation
- [Security overview](https://quarkus.io/guides/security-overview)
- [Basic authentication](https://quarkus.io/guides/security-basic-authentication-howto)
- [Properties file identity provider (embedded realm)](https://quarkus.io/guides/security-properties)
- [Authorization of web endpoints (annotations and path policies)](https://quarkus.io/guides/security-authorize-web-endpoints-reference)
- [Identity providers](https://quarkus.io/guides/security-identity-providers)
- [Testing security](https://quarkus.io/guides/security-testing)

## 🧭 Scenario
A small **reports API** (in memory). The production code is written; what is missing is everything about security.

| Endpoint | Rule it must obey |
|---|---|
| `GET /public/status` | anybody |
| `GET /api/me` | any authenticated user, even one without roles |
| `GET /api/reports` | role `user` or `admin`. A user sees **their own** reports, an admin sees all |
| `GET /api/reports/{id}` | role `user` or `admin`. Somebody else's report looks exactly like one that does not exist |
| `POST /api/reports` | role `user` or `admin`. The owner is the authenticated user, whatever the body says |
| `DELETE /api/reports/{id}` | role `admin` |
| `GET /admin/stats` | role `admin` (every method of `AdminResource`), except `GET /admin/health-note`, which `user` may read too |
| `GET /api/drafts` | nobody wrote a rule (a forgotten endpoint). It must be **closed** |
| `GET /internal/handbook.html` | a static file: role `user` or `admin` |

The demo users (made-up values, same in dev and tests):

| User | Role | Password |
|---|---|---|
| `alice` | `admin` | `alice-pw` |
| `bob` | `user` | `bob-pw` |
| `carol` | `user` | `carol-pw` |
| `dave` | *(none)* | `dave-pw` |

## ✅ Tasks

### Task 1 - Who are you? (HTTP Basic and the embedded realm)
**Goal:** `GET /api/me` answers with the identity for valid credentials and with a 401 challenge for everybody else. **Validated by:** `AuthenticationTest`.

- [ ] In `application.properties`, enable the Basic mechanism.
- [ ] Define the four users and their roles in the embedded realm, for the **dev and test profiles only**.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=AuthenticationTest'
```

```bash
./mvnw test -Dtest=AuthenticationTest
```

> [!WARNING]
> Two ways to get this wrong that look alike. Forgetting a setting is not the same as a wrong password.

🔮 **Predict first:** you define the users but **forget** to turn Basic on. A request with alice's right password: which status, and does the response carry a `WWW-Authenticate` header? And if you turn Basic on but forget to say the passwords are plain text, what happens when the application starts?

### Task 2 - What may you do? (roles and a closed default)
**Goal:** the right role gets in, the wrong one gets 403, an anonymous visitor gets 401, and an endpoint nobody annotated is closed. **Validated by:** `RolesTest`.

- [ ] Put the roles on the methods of `ReportResource` (see the table above).
- [ ] Protect all of `AdminResource` with one annotation on the class, and give `health-note` its own.
- [ ] Make "no annotation" mean "closed" for the whole project.

```powershell
.\mvnw.cmd test '-Dtest=RolesTest'
```

```bash
./mvnw test -Dtest=RolesTest
```

> [!WARNING]
> Look at `DraftResource` and at what `/api/drafts` answers *before* you change the default, and at which status each kind of visitor gets *after*. Then read `health-note` again: does a method annotation add to the class one, or replace it?

🔮 **Predict first:** with the project-wide default turned **off**, what do an anonymous visitor and bob get from `/api/drafts`? And with `@RolesAllowed("admin")` on the class and `@RolesAllowed({"admin","user"})` on one method, which roles can call that method?

### Task 3 - Whose data is it? (rules only the code can enforce)
**Goal:** the owner of a report comes from the identity, and nobody reads somebody else's data. **Validated by:** `OwnershipTest`.

- [ ] `list`: a user gets their own reports, an admin gets all of them.
- [ ] `get`: somebody else's report is a 404 (an admin reads any).
- [ ] `create`: the owner is the authenticated user. The body has no say.

```powershell
.\mvnw.cmd test '-Dtest=OwnershipTest'
```

```bash
./mvnw test -Dtest=OwnershipTest
```

> [!WARNING]
> Why 404 and not 403 for somebody else's report? What would an attacker learn from each? And what if the JSON body *does* contain an `owner` field?

### Task 4 - Rules by path (configuration)
**Goal:** the static file under `/internal/` needs a role, and the check does not leak which files exist. **Validated by:** `StaticPathTest`.

- [ ] Define a policy that allows the roles `user` and `admin`, and a permission that applies it to everything under `/internal/`.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=StaticPathTest'
```

```bash
./mvnw test -Dtest=StaticPathTest
```

🔮 **Predict first:** an anonymous visitor asks for `/internal/secret-plans.html`, a file that **does not exist**. 401 or 404? And bob, who is allowed in?

### Task 5 - Tests that need an identity (`@TestSecurity`)
**Goal:** run the tests of `TestSecurityTest` as the identities their names describe. **Validated by:** `TestSecurityTest`.

- [ ] Replace each `TODO task 5` comment by the annotation that gives the test that identity.
- [ ] Read the two tests that mix the annotation with a real header, and the one with authorization off.

```powershell
.\mvnw.cmd test '-Dtest=TestSecurityTest'
```

```bash
./mvnw test -Dtest=TestSecurityTest
```

🔮 **Predict first:** a test is annotated as `bob`, and the request carries a real Basic header for `alice`. Who is the request? And with a **wrong** password in that header?

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green (28 tests).

## 👀 What you should see
After all five tasks, the whole suite is green:

```text
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
```

Try it by hand too (dev mode needs no Docker here):

```powershell
.\mvnw.cmd quarkus:dev
```

```bash
./mvnw quarkus:dev
```

In another terminal (`curl.exe` in PowerShell, because `curl` is an alias there):

```powershell
curl.exe -i http://localhost:8080/api/me
curl.exe -u bob:bob-pw http://localhost:8080/api/me
curl.exe -i -u bob:bob-pw http://localhost:8080/admin/stats
curl.exe -i -u alice:alice-pw http://localhost:8080/admin/stats
```

You should see a `401` with a `www-authenticate: basic` header, bob's identity as JSON, a `403`, and a `200`.

Then run the **packaged application**, the way production does, and read what happens (in `solution/` once you are done, or in your own project):

```powershell
.\mvnw.cmd package -DskipTests
java -jar target\quarkus-app\quarkus-run.jar
```

```bash
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

```powershell
curl.exe -i http://localhost:8080/public/status
```

## 💼 In real projects
- **The embedded realm is for demos and tests, never for production.** Users and passwords in a properties file do not scale, cannot be rotated and end up in git. Production uses a database (`quarkus-security-jpa`, passwords hashed with bcrypt) or an identity server (labs 20 and 21). That is why the users in this lab live only in the `%dev` and `%test` profiles.
- **Basic is not encryption.** The header is `user:password` in base64. It is acceptable only over TLS (lab 22), and a browser app should not keep passwords around at all.
- **Closed by default** (`deny-unannotated-endpoints`) turns "I forgot" into a 403 in a test, instead of an open endpoint in production. Pair it with `@PermitAll` to make every public endpoint a visible decision.
- **Authorization is checked on the server, always.** Hiding a button is a courtesy; the 403 is the control.
- **Do not leak with your errors.** 404 for data the caller must not know about, the same 401 for "wrong password" and "no such user", and no stack traces (lab 05).
- **Test the doors, not only the rooms.** For every rule, one test that is *allowed* (so you know the endpoint works) and one that is *refused*. `@TestSecurity` is fast but skips the login; keep a few real-header tests.
- **Every profile needs a provider.** A configuration that works in tests can fail in production in a way no test sees (see the verified answers). Always run the packaged application once.

## 🚀 Stretch goal
Without hints: make `DELETE` leave an audit line of *who* deleted *what* (use the identity, not a parameter), and write the test that proves the name in the log is the authenticated one even if the request carries an `X-User` header with a different name.

## 💡 Hints
<details><summary>Hint 1 (direction)</summary>

Task 1 is all configuration: one property turns the mechanism on, the `quarkus.security.users.embedded.*` family defines the realm (look at its `users` and `roles` entries and at `plain-text`). Task 2 uses `jakarta.annotation.security.RolesAllowed` and one property in the `quarkus.security.jaxrs` group. Task 4 needs a *policy* (who) and a *permission* (where) in the `quarkus.http.auth` group.
</details>

<details><summary>Hint 2 (more concrete)</summary>

The properties are `quarkus.http.auth.basic`, `quarkus.security.users.embedded.enabled`, `quarkus.security.users.embedded.plain-text`, `quarkus.security.users.embedded.users.<name>` and `...roles.<name>`, `quarkus.security.jaxrs.deny-unannotated-endpoints`, `quarkus.http.auth.policy.<policy>.roles-allowed`, `quarkus.http.auth.permission.<name>.paths` and `.policy`. In the code, `identity.hasRole(...)` and `identity.getPrincipal().getName()` answer the questions of task 3. The test annotation is `io.quarkus.test.security.TestSecurity` with `user` and `roles`.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was run on Quarkus 3.40.1.1.

**Task 1.**
- Turning Basic on is a *separate* setting from defining users. With the users defined and `quarkus.http.auth.basic=true` **missing**, alice with the right password gets **401 and no `WWW-Authenticate` header**: with no mechanism, the credentials are never even read, and the answer does not ask for them.
- With `quarkus.security.users.embedded.plain-text` missing, the application **does not start**: `Unable to register password for user:carol make sure it is a valid hex encoded DIGEST-SHA-512 hash`. Without the flag, the realm expects hashes, not passwords.
- Hashed passwords work: in this version the default algorithm is **`digest-sha-512`** and the stored value is the lowercase hex of `SHA-512(username:realm:password)` with the realm name `Quarkus`. (The text of the guide mentions MD5; MD5 works only if you set `quarkus.security.users.embedded.algorithm=digest-md5`.) To compute one:

```powershell
$bytes = [Text.Encoding]::UTF8.GetBytes("alice:Quarkus:alice-pw")
([BitConverter]::ToString([Security.Cryptography.SHA512]::Create().ComputeHash($bytes)) -replace '-','').ToLower()
```

```bash
printf 'alice:Quarkus:alice-pw' | sha512sum
```

- Wrong password, unknown user and a garbage `Authorization` header give the **same** `401` with an empty body. A client that waits for the challenge (not preemptive) gets in with a second request. A lowercase scheme (`basic ...`) is accepted.
- A user without roles (`dave`) is authenticated: `GET /api/me` answers 200 with `"roles": []`.

**Task 2.**
- With `deny-unannotated-endpoints` **off**, `GET /api/drafts` answers **200 to everybody, anonymous included**. With it **on**: **401** anonymous, **403** bob and **403** alice. Closed for everyone, administrators included, until somebody writes a rule.
- A method-level annotation **replaces** the class-level one: with `@RolesAllowed("admin")` on `AdminResource` and `@RolesAllowed({"admin","user"})` on `health-note`, bob gets 200 there, dave (no roles) gets 403, and bob gets 403 on `/admin/stats`.
- Role names are case-sensitive: `@RolesAllowed("Admin")` with a realm role `admin` gives alice a **403** on that endpoint.
- **Two mechanisms, one request.** In a scratch copy, protect `/admin/*` with a *path policy only* (`roles-allowed=admin`, no annotation on `AdminResource`) and keep the closed default: alice gets **403**, and so does everybody else: the closed default denies the unannotated endpoint even though the path policy would let alice in. Adding `@PermitAll` (or `@RolesAllowed`) on the resource fixes it: alice 200, bob 403, anonymous 401. `applies-to=JAXRS` on the permission does **not** help (alice stays 403). That is why this lab uses annotations for endpoints and path policies for what is not an endpoint.

**Task 3.**
- Somebody else's report, and a report that does not exist, answer the same 404. A 403 would tell an attacker "report 7 exists, it is just not yours": enumeration.
- `{"title":"mine","body":"x","owner":"carol"}` posted by bob creates a report owned by **bob**. `ReportRequest` has no `owner`, and Quarkus ignores unknown JSON properties by default, so the field is dropped (lab 14 shows how to reject unknown fields instead). If the owner were a field of the type you save, this would be *mass assignment*.

**Task 4.**
- The path check runs **before routing**: an anonymous visitor asking for `/internal/secret-plans.html` (which does not exist) gets **401**; bob gets **404**. The visitor cannot tell which files exist. `/internal` without the trailing slash is covered too (401). `dave` has no role: 403 on the existing handbook.
- A path outside every policy and every endpoint is a plain 404 even for anonymous visitors (`/nothing-here`).

**Task 5.**
- `@TestSecurity(user = "bob", roles = "user")` answers `/api/me` with `bob` and `["user"]` without any header. The user does not need to exist in the realm: `@TestSecurity(user = "boss", roles = "admin")` passes the admin check although `boss` is defined nowhere.
- A real Basic header on a test annotated as bob is **still processed and wins**: with alice's header the request is alice; with a **wrong** password it is a **401**, even though the annotation alone would have let it in.
- `@TestSecurity(authorizationEnabled = false)` skips every security annotation (`/admin/stats` answers 200 without any identity) and the identity it gives has an **empty name**. Useful to test business logic, useless as proof that the doors are closed.
- A test annotated with an identity never sees a 401 challenge: it cannot test authentication. `AuthenticationTest` uses real headers for exactly that.

**The packaged application.**
- The packaged jar runs in the `prod` profile, where this lab defines **no users** (the realm is off). Basic is still enabled, so every request fails, `GET /public/status` included, with **500** and, in the log, `HttpAuthenticationMechanism 'io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism' requires one or more IdentityProviders supporting at least one of the following credentials types: [class io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest]`. The mechanism authenticates every request first, and it has nobody to ask. No test of this lab can see it, because the test profile does define users. It fails closed, but loudly and everywhere: a real deployment needs its provider configured, and a smoke test of the packaged application must be part of delivery (lab 18 showed `@QuarkusIntegrationTest`).
</details>

## 🎤 Self-assessment
1. What is the difference between a 401 and a 403, and which header does a correct 401 carry? When do you answer 404 instead of 403?
2. What is the difference between an authentication mechanism and an identity provider? Which one is Basic, and which one is the embedded realm?
3. Why is `deny-unannotated-endpoints` a better default than remembering to annotate? What does it do to an endpoint protected only by a path policy?
4. A method has `@RolesAllowed("admin")` on its class and `@RolesAllowed("user")` on itself. Who can call it? How do you test your answer?
5. What can `@TestSecurity` prove, and what can it not prove? Give a case where a suite using only `@TestSecurity` would stay green while production lets anybody in (or nobody).

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
