# 🎟️ Lab 20 - JWT

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
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead. No Docker is needed: the app has no database.

## 🎯 Why it matters
In lab 19 the service checked a password on every request. In a system of several services that does not scale: nobody wants every service to know the passwords. The usual answer is a **token**: a central identity provider (lab 21) checks the user once and hands out a signed JSON Web Token; every service only has to **verify the signature** and read the claims. This lab is the service side: what a service must check before it trusts a token, how it reads who the caller is from it, and how to test it.

## 🧠 Concepts
- A **JWT** has three Base64URL parts separated by dots: header, payload (the **claims**), signature. The payload is **encoded, not encrypted**: anybody holding the token can read it. Only the signature prevents changing it.
- The service verifies with a **public key** (here a file; in real systems a JWKS URL). Whoever holds the **private key** can mint tokens, so it never leaves the identity provider.
- A valid signature is not enough. The service also checks the **issuer** (`iss`), the **audience** (`aud`: is this token meant for me?), the **expiration** (`exp`) and optionally the maximum lifetime.
- The claim `groups` becomes the **roles** that `@RolesAllowed` checks. `upn` (or `preferred_username`, or `sub`) becomes the principal name.
- The mechanism is "Bearer": `Authorization: Bearer <token>`. An invalid or missing token is **401** with `WWW-Authenticate: Bearer`; a valid token without the role is **403** (same split as lab 19).
- A token is **stateless**: the service never asks anybody whether it is still good. That is its strength and its price (see "In real projects").

## 📚 Documentation
- [Using SmallRye JWT](https://quarkus.io/guides/security-jwt)
- [Build, sign and encrypt JWT tokens](https://quarkus.io/guides/security-jwt-build)
- [Testing security](https://quarkus.io/guides/security-testing)
- [Security overview](https://quarkus.io/guides/security-overview)

## 🧭 Scenario
A tiny **notes API** (in memory). The production code of the endpoints is written; the service trusts nothing yet.

| Endpoint | Rule it must obey |
|---|---|
| `GET /api/public/status` | anybody |
| `GET /api/me` | any valid token. Answers with name, subject, issuer, groups, `email` and expiration **read from the token** |
| `GET /api/notes` | role `reader`, `editor` or `admin` |
| `POST /api/notes` | role `editor` or `admin`. The author is the name in the token |
| `DELETE /api/notes/{id}` | role `admin` |

The tests mint their own tokens with the helper `Tokens` (signed with `src/test/resources/privateKey.pem`, a throw-away key pair created for this lab; the matching public key is `src/main/resources/publicKey.pem`). Nothing here is a secret: the private key protects nothing outside these tests. In a real project it would never be in the repository.

## ✅ Tasks

### Task 1 - Verify the token (key and issuer)
**Goal:** a token signed with the lab key and issued by `https://lab.example/issuer` opens `/api/me`; everything else is a 401. **Validated by:** `AuthenticationTest`.

- [ ] In `application.properties`, set the location of the public key (`publicKey.pem`, on the classpath) and the issuer to trust.
- [ ] Also include the key file in a native executable (a one-line property, see the guide).
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=AuthenticationTest'
```

```bash
./mvnw test -Dtest=AuthenticationTest
```

> [!WARNING]
> Misconfiguring verification can fail loudly or fail silently. Try both and compare.

🔮 **Predict first:** (a) you set the key but **forget the issuer**: what does a perfectly valid token get? (b) you write a wrong path for the key (`missing.pem`): does the application fail to start? What does `/api/public/status`, which needs no token, answer?

### Task 2 - Who is calling? (claims)
**Goal:** `/api/me` answers from the token. **Validated by:** `ClaimsTest`.

- [ ] Inject the `JsonWebToken` and use its name, subject, issuer, groups and expiration.
- [ ] Inject the custom claim `email` as a plain `String`. A token without it must give `null`, not an error.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=ClaimsTest'
```

```bash
./mvnw test -Dtest=ClaimsTest
```

> [!WARNING]
> The scope of the bean that receives a single claim matters.

🔮 **Predict first:** what happens if the resource that injects `@Claim("email") String email` is `@ApplicationScoped` instead of `@RequestScoped`? A failing request, a failing test, or something earlier?

### Task 3 - What may you do? (groups become roles)
**Goal:** the `groups` claim decides who can call what. **Validated by:** `RolesTest`.

- [ ] Put the roles of the scenario table on the three methods of `NotesResource`.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=RolesTest'
```

```bash
./mvnw test -Dtest=RolesTest
```

🔮 **Predict first:** a perfectly valid token has **no** `groups` claim at all. Calling `GET /api/notes`: 401 or 403? And a token whose group is `Reader` (capital R)?

> [!NOTE]
> Once the annotations are in, you will see tests of `TestSecurityTest` turn red that were green before. That is not a mistake: before this task the endpoints were open to everybody, so tests without an identity passed for the wrong reason. Task 4 gives them their identity.

### Task 4 - Tests without minting tokens (`@TestSecurity` + `@JwtSecurity`)
**Goal:** run the tests of `TestSecurityTest` with the identity and claims their names describe, without creating or signing a token. **Validated by:** `TestSecurityTest`.

- [ ] Replace each `TODO task 4` comment by the annotations that give the test that identity.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=TestSecurityTest'
```

```bash
./mvnw test -Dtest=TestSecurityTest
```

> [!WARNING]
> The injected token contains **only** the claims you give it. The endpoint `/api/me` reads the expiration.

🔮 **Predict first:** you add the `email` claim with `@JwtSecurity` but not `exp`. What does `/api/me` answer?

### Task 5 - A good signature is not enough (audience and lifetime)
**Goal:** reject tokens that are validly signed but not meant for this API, or that live too long. **Validated by:** `TokenRulesTest`.

- [ ] Require the audience `notes-api`.
- [ ] Limit the lifetime of a token (`exp - iat`) to one hour.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=TokenRulesTest'
```

```bash
./mvnw test -Dtest=TokenRulesTest
```

🔮 **Predict first:** a token signed with the right key, right issuer, not expired, but for the audience `billing-api`: 200, 401 or 403? And one with two audiences, `billing-api` and `notes-api`?

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green.

## 👀 What you should see
First, with the finished project, call it like a client would. Start the app:

```powershell
.\mvnw.cmd quarkus:dev
```

In another terminal (in the same folder) mint a token with the helper class in `src/test` and call the service. The helper takes the name and then the groups:

```powershell
.\mvnw.cmd test-compile -q
.\mvnw.cmd dependency:build-classpath -q "-Dmdep.outputFile=target\cp.txt"
$cp = "target\test-classes;target\classes;" + (Get-Content target\cp.txt)
$token = java -cp $cp lab.melostudy.GenerateToken alice@lab.example editor
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/me
curl.exe -i http://localhost:8080/api/me
```

```bash
./mvnw test-compile -q
./mvnw dependency:build-classpath -q -Dmdep.outputFile=target/cp.txt
TOKEN=$(java -cp "target/test-classes:target/classes:$(cat target/cp.txt)" lab.melostudy.GenerateToken alice@lab.example editor)
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/me
curl -i http://localhost:8080/api/me
```

```text
HTTP/1.1 200 OK
{"name":"alice@lab.example","subject":null,"issuer":"https://lab.example/issuer","groups":["editor"],"email":null,"expiresAt":1793680911}

HTTP/1.1 401 Unauthorized
www-authenticate: Bearer
```

Now open the payload yourself. Nothing is secret in it:

```powershell
$payload = ($token -split '\.')[1]
$padded = $payload.PadRight($payload.Length + (4 - $payload.Length % 4) % 4, '=').Replace('-', '+').Replace('_', '/')
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($padded))
```

```text
{"iss":"https://lab.example/issuer","upn":"alice@lab.example","groups":["editor"],"aud":"notes-api","iat":1793679111,"exp":1793680911,"jti":"339135c0-ed5e-4d70-ac8b-485da8868400"}
```

> [!TIP]
> Do not paste real tokens into online decoders. A token is a credential until it expires. Decode it locally, as above.

## 💼 In real projects
- **Who mints the tokens?** Not your service. An identity provider does (Keycloak and friends, lab 21); your service only has the **public** key, usually as a **JWKS URL** (`mp.jwt.verify.publickey.location` accepts a URL) so keys can rotate without a redeploy.
- **A token cannot be revoked by the service.** It is good until `exp`. That is why access tokens live minutes, not days, and why the lifetime limit of task 5 is a safety net against a provider that is too generous. Logging a user out means dropping the token on the client plus a short lifetime, or a deny-list you build yourself.
- **Never put secrets in the payload.** Encoded is not encrypted. Keep it to identifiers and roles.
- **The audience check is what stops a token for service A from working on service B.** Without it, any validly signed token of the same issuer opens every API.
- **Keep the private key out of the repository, the image and the jar.** The test key of this lab is the exception on purpose. In the packaged app of this lab the public key is inside the jar and the private key is not (verified: `jar tf` shows `publicKey.pem` and no `privateKey.pem`).
- **Never log a full token** (headers, debug logs, error reports).
- **Roles from claims:** your provider decides the shape of the token. Keycloak, for example, puts roles in a nested claim, not in `groups` (see the stretch goal).
- **Test with real tokens too.** `@TestSecurity` skips the whole verification (signature, issuer, audience): it proves the authorization rules, not the authentication.

## 🚀 Stretch goal
Your identity provider sends the roles inside `{"realm_access": {"roles": ["editor"]}}` and no `groups`. Make the service read them from there (look for the setting that takes a path to the groups claim), then write a test that mints such a token with `Tokens.valid(...).claim("realm_access", Map.of("roles", List.of("editor"))).remove("groups")` and posts a note. Then predict and check: what happens to the tokens that carry a plain `groups` claim and no `realm_access`?

## 💡 Hints
<details><summary>Hint 1 (direction)</summary>

Task 1 and task 5 are `application.properties` only: look for the `mp.jwt.verify.*` family and one `smallrye.jwt.*` setting for the lifetime. Task 2 is two injections (`JsonWebToken` and `@Claim`) and the right scope. Task 3 is the annotation you already used in lab 19. Task 4 is two annotations from `io.quarkus.test.security`.
</details>

<details><summary>Hint 2 (more concrete)</summary>

The settings are `mp.jwt.verify.publickey.location`, `mp.jwt.verify.issuer`, `quarkus.native.resources.includes`, `mp.jwt.verify.audiences` and `smallrye.jwt.time-to-live` (seconds). `@Claim("email") String` needs `@RequestScoped`. For the test claims use `@JwtSecurity(claims = { @Claim(key = ..., value = ...) })`; a number needs `type = ClaimType.LONG`.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1 (SmallRye JWT 4.6.4).

**Task 1 (a), (b).**
- **No issuer configured:** the application starts and **every** token, however valid, gets **401**. In the same way, with no key configured, every token gets 401. Nothing in the test output says why.
- **Wrong key path:** the application **starts**; the failure arrives with the first request, and it is a **500 on every endpoint, public ones included**: `SRJWT02002: Failed to read the public key content from 'mp.jwt.verify.publickey.location'` (cause: `MalformedURLException: no protocol: missing.pem`).
- Removing the issuer property turns 22 of the 29 tests red; removing the key location, the same 22.

**Task 2.** With `@ApplicationScoped` the **build** fails, before any request: `java.lang.String type can not be used to represent JWT claims in @Singleton or @ApplicationScoped beans, make the bean @RequestScoped or wrap this type with org.eclipse.microprofile.jwt.ClaimValue or jakarta.inject.Provider or jakarta.enterprise.inject.Instance`. A missing claim injected into a `@RequestScoped` bean is `null`.

**Task 3.** A valid token without `groups` is **403** (the service knows who you are and you have no role). No token at all is **401**. `Reader` is not `reader`: **403** (role names are case-sensitive).

**Task 4.** Without `exp` the endpoint answers **500**: `/api/me` calls `getExpirationTime()` and the injected token has no such claim (`NullPointerException: Cannot invoke "java.lang.Long.longValue()" because the return value of "JsonWebToken.getClaim(Claims)" is null`). The test token contains only what you put in it. Supply `exp` as `ClaimType.LONG`.

**Task 5.**
- Right signature, right issuer, not expired, audience `billing-api`: **401** (not 403: the token is not valid *for this service*).
- A token without any audience: **401** once the audience is required. With two audiences, one of them `notes-api`: **200** (one match is enough).
- A token of 24 hours: **401**; one of 30 minutes: **200**.
- Removing `mp.jwt.verify.audiences` turns 2 tests red (other audience, no audience); removing `smallrye.jwt.time-to-live` turns 1 red (24 hours).

**What the tests prove (and what they do not).**
- A token whose payload you edit by hand (for example `groups: ["admin"]`) keeps the old signature and gets **401** (checked with curl against `quarkus:dev`).
- Signed with another key, from another issuer, expired, malformed and sent with the wrong scheme: all **401**.
- In the packaged jar (`java -jar target/quarkus-app/quarkus-run.jar`, profile `prod`) the same requests give the same answers: public 200, no token 401, reader on `POST /api/notes` 403, editor 201.

**Stretch.** With `smallrye.jwt.path.groups=realm_access/roles` the Keycloak-shaped token gets 200 on `GET /api/notes` and 201 on `POST`; without it, 403. With the setting on, a token with a plain `groups` claim (and no `realm_access`) **still works**: reader gets 200 on `GET /api/notes`. The standard `groups` claim is the fallback when the configured path is not in the token.
</details>

## 🎤 Self-assessment
1. A JWT is Base64URL, not encrypted. What does the signature protect, and what does it not?
2. Name four things a service must check besides the signature. For each one, what attack does it stop?
3. A token is valid for 30 more minutes and the user is fired now. What can the service do? What would you design?
4. 401 or 403 for: (a) no token, (b) expired token, (c) valid token without the role, (d) valid token for another audience?
5. Why does a test with `@TestSecurity` pass while a real client with the same user would get a 401? Which tests must still use real tokens?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
