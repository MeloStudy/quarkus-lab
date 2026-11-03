# 🪪 Lab 21 - OIDC and Keycloak

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
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead.

> [!IMPORTANT]
> **Docker must be running.** Dev Services starts a real Keycloak container. The first run downloads the image (about 255 MB; it took 13 s here) and Keycloak needs about 25 s to start, so a test run takes about a minute. Later runs reuse the image. All test classes share one Keycloak.

## 🎯 Why it matters
In lab 20 the lab minted its own tokens with a key it kept in the test folder. In a real system nobody does that: a central **identity provider** (Keycloak, Entra ID, Auth0, Okta...) knows the users, checks their passwords, and signs the tokens. Your service only says "I trust that provider". This lab connects the service of lab 20 to a real Keycloak, using the **OpenID Connect** extension instead of hand-written key settings, and adds what appears as soon as there are several services: passing the caller's token on to the next service.

## 🧠 Concepts
- **OAuth 2.0** is about *authorization* (getting an access token); **OpenID Connect (OIDC)** adds *authentication* (who the user is) on top of it. A token endpoint, a discovery document and a key set (JWKS) all hang from one URL: the **issuer**, here `.../realms/lab`.
- `quarkus-oidc` has three modes. This lab is the first one, **service** (bearer token): the caller sends `Authorization: Bearer ...` and the service only verifies it. The others, **web-app** (authorization code flow with a login page) and **hybrid**, are for applications with a browser.
- With one URL, the extension downloads the keys, caches them and rotates them. You do not configure keys (compare with lab 20).
- **Dev Services for Keycloak** starts a Keycloak container in dev and test mode, and fills `quarkus.oidc.auth-server-url` for you. A **realm file** (`lab-realm.json`) describes realm, clients, roles and users as code, and Dev Services imports it.
- A token can be for a different **audience**. A token of the same realm and a valid signature is not necessarily meant for this service.
- **Token propagation:** when service A calls service B for a user, A can forward the user's token so that B decides with the user's identity. (The alternative, A calling with its own identity, has different security implications.)
- In production the provider is a service somebody operates; the app is told its URL (`quarkus.oidc.auth-server-url`) and Dev Services stays off.

## 📚 Documentation
- [OpenID Connect bearer token authentication](https://quarkus.io/guides/security-oidc-bearer-token-authentication)
- [Dev Services and UI for Keycloak](https://quarkus.io/guides/security-openid-connect-dev-services)
- [OpenID Connect configuration properties](https://quarkus.io/guides/security-oidc-configuration-properties-reference)
- [OIDC token propagation (REST client)](https://quarkus.io/guides/security-openid-connect-client)
- [Testing security](https://quarkus.io/guides/security-testing)

## 🧭 Scenario
The notes API of lab 20, now behind Keycloak.

| Endpoint | Rule it must obey |
|---|---|
| `GET /api/public/status` | anybody |
| `GET /api/me` | any valid token for this service |
| `GET /api/notes` | role `reader`, `editor` or `admin` |
| `POST /api/notes` | role `editor` or `admin`. The author is the user name |
| `DELETE /api/notes/{id}` | role `admin` |
| `GET /api/summary` | role `reader`, `editor` or `admin`. Builds its answer by **calling `GET /api/notes` over HTTP** with the caller's token (in this lab the "other service" is the same application, to keep it small) |

The realm `lab` (file `src/main/resources/lab-realm.json`, already written; made-up values) has:

| Piece | Content |
|---|---|
| Client `notes-service` | the service the tokens are meant for (secret `notes-secret`). Its tokens carry the audience `notes-service` |
| Client `billing-service` | another service of the same realm (secret `billing-secret`). Its tokens do **not** carry that audience |
| Users | `rita` (`reader`), `eve` (`reader`, `editor`), `adam` (`admin`); the password is always `<name>-pw` |

The test helper `Tokens` asks the running Keycloak for real tokens (password grant, a shortcut for tests; real users log in through a browser).

## ✅ Tasks

### Task 1 - Trust the provider (Dev Services and the realm)
**Goal:** the tests obtain tokens from the Keycloak that Dev Services starts with the `lab` realm, and the service accepts them. **Validated by:** `AuthenticationTest`.

- [ ] In `application.properties`, tell Dev Services to import `lab-realm.json`.
- [ ] Pin the Keycloak image to `quay.io/keycloak/keycloak:26.7.4`.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=AuthenticationTest'
```

```bash
./mvnw test -Dtest=AuthenticationTest
```

> [!WARNING]
> There is no `quarkus.oidc.auth-server-url` anywhere in the file, and no key. Look at what the test log says about Dev Services.

🔮 **Predict first:** (a) with no Keycloak setting at all, which users exist in the Keycloak that Dev Services starts? (b) You package the application and run the jar with nothing else configured: does it start?

### Task 2 - What may you do? (realm roles)
**Goal:** the realm roles in the token decide who can call what. **Validated by:** `RolesTest`.

- [ ] Put the roles of the scenario table on the three methods of `NotesResource`.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=RolesTest'
```

```bash
./mvnw test -Dtest=RolesTest
```

🔮 **Predict first:** Keycloak puts the roles in the nested claim `realm_access.roles`, not in `groups`. Do you have to tell the extension where to look (the setting that takes a claim path)? Decide before you run.

> [!NOTE]
> Once the annotations are in, some tests of other classes turn red that were green. That is not a mistake: before this task the notes were open to everybody, so tests that call them passed with any identity.

### Task 3 - Is this token for me? (audience)
**Goal:** a valid token of the same realm, issued for another client, is refused. **Validated by:** `AudienceTest`.

- [ ] Require the audience `notes-service` in `application.properties`.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=AudienceTest'
```

```bash
./mvnw test -Dtest=AudienceTest
```

🔮 **Predict first:** rita's token for `billing-service` has the right issuer, a valid signature and has not expired. Before you configure the audience, what does `/api/me` answer? After?

### Task 4 - Pass the caller on (token propagation)
**Goal:** `/api/summary` calls `/api/notes` as the caller. **Validated by:** `PropagationTest`.

- [ ] Make the REST client `NotesClient` send the token of the current request with every call. The dependency `quarkus-rest-client-oidc-token-propagation` is already in the `pom.xml`.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=PropagationTest'
```

```bash
./mvnw test -Dtest=PropagationTest
```

🔮 **Predict first:** before you add anything, rita calls `/api/summary`, which is allowed for her. The inner call to `/api/notes` has no token. What does *rita* get: 200, 401, 403 or 500?

### Task 5 - Tests without a Keycloak token (`@TestSecurity` + `@OidcSecurity`)
**Goal:** run the tests of `TestSecurityTest` with the identity and claims their names describe. **Validated by:** `TestSecurityTest`.

- [ ] Replace each `TODO task 5` comment by the annotations that give the test that identity.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=TestSecurityTest'
```

```bash
./mvnw test -Dtest=TestSecurityTest
```

> [!WARNING]
> `@TestSecurity` installs an identity; nobody has a token to hand to the REST client. Think about which endpoint of this lab that breaks, then check.

🔮 **Predict first:** a test with `@TestSecurity(user = "rita", roles = "reader")` calls `/api/summary`. 200 or not?

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green.

## 👀 What you should see
With the finished project, run the app. Dev Services starts Keycloak:

```powershell
.\mvnw.cmd quarkus:dev
```

In another terminal find the port of the container (it is random) and ask Keycloak for a token as `eve`:

```powershell
docker ps --filter "ancestor=quay.io/keycloak/keycloak:26.7.4" --format "{{.Ports}}"
$issuer = "http://localhost:<port>/realms/lab"
$token = (curl.exe -s -d "grant_type=password&client_id=notes-service&client_secret=notes-secret&username=eve&password=eve-pw" "$issuer/protocol/openid-connect/token" | ConvertFrom-Json).access_token
curl.exe -i -H "Authorization: Bearer $token" http://localhost:8080/api/me
curl.exe -i http://localhost:8080/api/me
```

```bash
docker ps --filter "ancestor=quay.io/keycloak/keycloak:26.7.4" --format "{{.Ports}}"
ISSUER=http://localhost:<port>/realms/lab
TOKEN=$(curl -s -d "grant_type=password&client_id=notes-service&client_secret=notes-secret&username=eve&password=eve-pw" "$ISSUER/protocol/openid-connect/token" | python -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/me
curl -i http://localhost:8080/api/me
```

```text
HTTP/1.1 200 OK
{"name":"eve","subject":"...","issuer":"http://localhost:<port>/realms/lab","roles":["editor","reader"],"email":"eve@lab.example","expiresAt":...}

HTTP/1.1 401 Unauthorized
www-authenticate: Bearer
```

Now the packaged application. Build it, and run it first **without** telling it where Keycloak is:

```powershell
.\mvnw.cmd package '-DskipTests'
java -Dquarkus.http.port=8090 -jar target\quarkus-app\quarkus-run.jar
```

```bash
./mvnw package -DskipTests
java -Dquarkus.http.port=8090 -jar target/quarkus-app/quarkus-run.jar
```

```text
'quarkus.oidc.auth-server-url' property must be configured
```

Run it again with `-Dquarkus.oidc.auth-server-url=$issuer` (and `-Dquarkus.rest-client.notes.url=http://localhost:8090`) while dev mode is still running, and repeat the calls against port 8090: the answers are the same as in dev mode. In production that URL comes from an environment variable (`QUARKUS_OIDC_AUTH_SERVER_URL`), not from the repository.

## 💼 In real projects
- **Realm as code, provider as a service.** `lab-realm.json` is a lab convenience. Real realms are managed in the provider (and exported or applied with its own tooling); never reuse test secrets and passwords. Dev Services only runs in dev and test; in production the application must be told the URL of the real provider.
- **Pin the image.** Without `image-name`, Dev Services uses the version your Quarkus release picked. A realm file and a test suite should not change meaning on an upgrade.
- **The service holds no secret.** A bearer-only service verifies signatures with public keys; it needs no client secret (this lab's service sets none, only the *tests* use the client secrets to get tokens). Secrets appear when the service itself must call the provider (introspection, client credentials, token exchange).
- **Audience is the isolation between services.** If every service of the realm accepts every token of the realm, a stolen token for the least important service opens the most important one.
- **Propagation or the service's own identity?** Propagating the caller's token keeps the user's rights, but the token has to be meant for *both* services (audience!), and an expired or revoked token breaks the whole chain. Calling with its own identity (client credentials) is simpler to operate but loses "who really asked". Decide per call, and write it down.
- **Roles from the provider vs roles in your code.** Roles live in the provider and travel in the token. Do not map them into your own database unless you must; the token is the source of truth for this request.
- **Machine tokens exist.** Another service can get a token for itself (`client_credentials`). Its identity is a **service account** and it is a valid user for `@Authenticated` (see the stretch goal).
- **Keycloak is a dependency of your tests.** A test suite that needs Docker and a minute of startup belongs in CI with Docker available. Keep `@TestSecurity` tests for the many authorization cases and a few real-token tests for authentication.

## 🚀 Stretch goal
Ask Keycloak for a token as the **client itself**, with no user:

```powershell
curl.exe -s -d "grant_type=client_credentials&client_id=notes-service&client_secret=notes-secret" "$issuer/protocol/openid-connect/token"
```

Predict, then check with the running service: what does `/api/me` answer with that token, who is the "user", and may it read `/api/notes`? Then decode the payload (see lab 20) and find out where its roles are.

## 💡 Hints
<details><summary>Hint 1 (direction)</summary>

Task 1 and task 3 are `application.properties` only: the family `quarkus.keycloak.devservices.*` for the realm and the image, and `quarkus.oidc.token.audience`. Task 2 is the annotation of lab 19 and lab 20. Task 4 is one annotation on the REST client interface from the package `io.quarkus.oidc.token.propagation.common`. Task 5 is the two annotations from `io.quarkus.test.security` and `io.quarkus.test.security.oidc`.
</details>

<details><summary>Hint 2 (more concrete)</summary>

`quarkus.keycloak.devservices.realm-path`, `quarkus.keycloak.devservices.image-name`, `quarkus.oidc.token.audience`, `@AccessToken`, `@TestSecurity(user = ..., roles = ...)`, `@OidcSecurity(claims = { @Claim(key = ..., value = ...) })`; a number needs `type = ClaimType.LONG` (and the injected token has only the claims you give it: `/api/me` reads `exp`).
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1 with Keycloak 26.7.4.

**Task 1.**
- (a) With no Keycloak setting, Dev Services starts a Keycloak with its own realm `quarkus`, the client `quarkus-app` and two users: `alice` (roles `admin` and `user`) and `bob` (role `user`). `rita`, `eve` and `adam` do not exist there, so the tests fail while asking for tokens.
- With the realm file imported, the access token of rita contains `"iss":"http://localhost:<port>/realms/lab"`, `"aud":"notes-service"` and `"realm_access":{"roles":["reader"]}`.
- (b) The jar **does not start**: `'quarkus.oidc.auth-server-url' property must be configured`. Compare with lab 19 and lab 20, where the packaged application started and failed on requests. Here the missing provider is caught at startup.
- A garbage token (`not-a-jwt`) is a 401, and the log shows the extension tried to **introspect it as an opaque token** at the provider: `.../token/introspect has failed: status: 401`. Not an error of yours: a service accepts opaque tokens too, and the client has no secret here.

**Task 2.** No claim path is needed: removing any role-claim setting leaves all tests green, because the extension finds the Keycloak roles in `realm_access/roles` by default (`/api/me` shows `roles: [editor, reader]` for eve). If another provider puts roles elsewhere, `quarkus.oidc.roles.role-claim-path` is the setting. Also checked: `quarkus.oidc.client-id` is not needed in the service either (removing it leaves everything green).

**Task 3.** Without the audience setting the token of `billing-service` is accepted (**200**); with it, **401**. Without the setting, 3 tests turn red (`/api/me`, `/api/notes` and `/api/summary` with the billing token). The log says `No Audience (aud) claim present`, since the billing client's tokens have no audience claim at all.

**Task 4.** Without the annotation, `/api/summary` answers **500** to rita: the inner call has no token, gets a 401, and the unhandled client exception becomes a 500 for the *caller*. All 3 tests of the class fail. With the annotation the answer is 200 and the inner call is authorized as rita.

**Task 5.** `/api/summary` under `@TestSecurity` answers **500** too: the identity exists, but there is no real token to forward. Test endpoints that call other services with a real token, or replace the REST client with a mock (lab 16).

**Stretch.** The token of `client_credentials` carries the roles `offline_access`, `default-roles-lab` and `uma_authorization` (none of your roles), `preferred_username` is `service-account-notes-service`, and the audience is `["notes-service","account"]`. `/api/me` answers **200** with that name; `GET /api/notes` answers **403**.

**Jar.** Started with `-Dquarkus.oidc.auth-server-url=<issuer>` against the same Keycloak, the packaged application answered like dev mode: public 200, `/api/me` with token 200, without token 401, `/api/summary` 200.
</details>

## 🎤 Self-assessment
1. What is the difference between OAuth 2.0 and OpenID Connect? In which of the three `quarkus-oidc` modes is this service, and what would change if it were a web application with a login page?
2. In lab 20 you configured a public key. Here you configured no key at all. Where do the keys come from, and what happens when the provider rotates them?
3. A token of the same realm and with a valid signature opens `/api/me` of the billing service but not of the notes service. Which claim and which setting are responsible?
4. Service A calls service B on behalf of a user. Compare propagating the user's token with calling B using A's own identity (client credentials): what does each gain and lose?
5. Why does the packaged jar of this lab refuse to start without an issuer URL, while the one of lab 19 started and answered 500 to everything? Which behavior would you prefer in production, and why?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
