# 🔒 Lab 22 - TLS and secrets

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
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead. No Docker is needed. `keytool` comes with the JDK.

## 🎯 Why it matters
Labs 19-21 decided *who may do what*. None of it matters if the traffic can be read on the wire, if the browser is left to guess how to treat your responses, or if the key to the partner API is sitting in the repository (and from there in every clone, every build and every jar). This lab closes the block with the three habits that every deployment needs: **encrypt the channel** (TLS), **tell the browser what to do** (security headers) and **keep secrets out of the repository and out of the artifact**.

## 🧠 Concepts
- **TLS** gives the client three things: the server is who it claims to be (the certificate chain, ending in an authority the client trusts), nobody can read the traffic, nobody can alter it. A **self-signed** certificate gives the last two but the client has no reason to trust it: use it for dev and tests, never for production.
- Quarkus configures TLS in the **TLS registry** (`quarkus.tls.*`): a key store (the server's certificate and private key) as a PKCS12 file or PEM files. The HTTP server uses it as soon as one is defined.
- `quarkus.http.insecure-requests` decides what happens to plain HTTP: `enabled` (default), `redirect` (the port stays open and answers with a redirect) or `disabled` (the port is closed).
- **Security headers** (`quarkus.http.header."Name".value`) are the server's instructions to the browser: do not sniff content types, do not allow framing, remember to use HTTPS only (HSTS), what the page may load (CSP), whether the response may be cached.
- **A secret is anything that grants access:** passwords, API keys, private keys, tokens. Rules: not in the repository (not even once: git remembers), not in the jar, not in the logs, not in responses. Real values come from the **environment** at deploy time: environment variables, or a file mounted from the platform's secret store.
- `%dev` and `%test` profile values are **throw-away** values with no power in any real system. They are allowed in the repository; they travel inside the jar, so they must be useless there.

## 📚 Documentation
- [HTTP reference: supporting secure connections with TLS](https://quarkus.io/guides/http-reference)
- [TLS registry reference](https://quarkus.io/guides/tls-registry-reference)
- [Secrets in configuration](https://quarkus.io/guides/config-secrets)
- [Configuration reference](https://quarkus.io/guides/config-reference)

## 🧭 Scenario
A tiny API with a partner endpoint protected by a shared key.

| Endpoint | What it does |
|---|---|
| `GET /api/hello` | `{"message":"hello"}` |
| `GET /api/info` | the name of the app and a **fingerprint** of the API key (first 8 hex characters of its SHA-256), never the key |
| `GET /api/partner/data` | 200 when the header `X-Api-Key` equals the configured key, 401 otherwise |

The tests generate a self-signed certificate for `localhost` at every build (`LabCertificates`, into `target/certs`: no certificate or key file is committed) and call the service over HTTPS trusting only that certificate (`Https`).

## ✅ Tasks

### Task 1 - Encrypt the channel (HTTPS)
**Goal:** the service answers over HTTPS with the certificate of a keystore, and plain HTTP clients are sent to HTTPS. **Validated by:** `HttpsTest`.

- [ ] In `application.properties`, define the key store (PKCS12) for the `test` profile (`target/certs/lab-keystore.p12`, password `lab-pw`) and for the `dev` profile (`target/dev-keystore.p12`, password `dev-only-pw`). Those passwords are throw-away; production values never go in this file.
- [ ] Make plain HTTP **redirect** to HTTPS.
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=HttpsTest'
```

```bash
./mvnw test -Dtest=HttpsTest
```

> [!WARNING]
> Two of the tests look like they need configuration and two do not. Find out which, and why, before you write anything.

🔮 **Predict first:** (a) with `insecure-requests=redirect` but **no keystore defined**, does the application start? (b) You did not touch the protocol versions. Does the service accept a client that only speaks TLS 1.2?

### Task 2 - Tell the browser what to do (security headers)
**Goal:** every response carries the headers below. **Validated by:** `HeadersTest`.

| Header | Value |
|---|---|
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Referrer-Policy` | `no-referrer` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |
| `Content-Security-Policy` | `default-src 'none'; frame-ancestors 'none'` |
| `Cache-Control` | `no-store` |

- [ ] Add them with `quarkus.http.header` (the header name goes in double quotes inside the property name).
- [ ] Run the test class.

```powershell
.\mvnw.cmd test '-Dtest=HeadersTest'
```

```bash
./mvnw test -Dtest=HeadersTest
```

🔮 **Predict first:** does a **404** response (a path that does not exist) carry the headers too? And why is `Cache-Control: no-store` on this API worth a test of its own on the partner endpoint?

### Task 3 - Get the secret out of the repository
**Goal:** the key of the partner API is not in `application.properties` as a real value, the application still starts and works in dev and test, and the service never reveals the key. **Validated by:** `NoSecretsInRepositoryTest`, `SecretsTest`.

- [ ] Open `application.properties`: somebody committed `app.api-key`. Remove it.
- [ ] Give the `dev` profile the throw-away key `dev-only-key` and the `test` profile `test-only-key`.
- [ ] Production gets it from the environment variable `APP_API_KEY` (no line in the file is needed). Read the scanner test to see what it accepts.
- [ ] Run both test classes.

```powershell
.\mvnw.cmd test '-Dtest=NoSecretsInRepositoryTest,SecretsTest'
```

```bash
./mvnw test -Dtest=NoSecretsInRepositoryTest,SecretsTest
```

> [!WARNING]
> Run the whole suite right after **only deleting** the line. Then check how many tests fail, and why that is the behavior you want in production.

🔮 **Predict first:** you delete the line and add nothing. Do the tests fail one by one, or does something earlier go wrong? And a client sends `X-Api-Key: test-only-key ` with a trailing space: 200 or 401?

### Task 4 - Run it like production (the jar, the environment, a mounted file)
**Goal:** run the packaged application with nothing from the repository but the jar, and give it its certificate and secret from outside. **Validated by:** the checklist (no test can see this).

- [ ] Package the application and generate a throw-away keystore (see "What you should see").
- [ ] Run the jar with **nothing** configured. Then with only `APP_API_KEY`. Then with the key and the keystore both from environment variables.
- [ ] Repeat the last run taking the secrets from a file with `SMALLRYE_CONFIG_LOCATIONS` instead of environment variables.
- [ ] Look inside the jar's `application.properties`.

🔮 **Predict first:** the first run (nothing configured) and the second (only the key): which one starts? What is each message? And: the jar contains the `%dev` and `%test` lines. Can a caller of the production jar use `dev-only-key`?

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green, and the checklist above is done.

## 👀 What you should see
Dev mode first. Create the dev keystore (the one the `dev` profile points to) and start the application:

```powershell
New-Item -ItemType Directory -Force target | Out-Null
keytool -genkeypair -alias dev -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore target\dev-keystore.p12 -storepass dev-only-pw -dname "CN=localhost" -ext "SAN=dns:localhost" -validity 30
.\mvnw.cmd quarkus:dev
```

```bash
mkdir -p target
keytool -genkeypair -alias dev -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore target/dev-keystore.p12 -storepass dev-only-pw -dname "CN=localhost" -ext "SAN=dns:localhost" -validity 30
./mvnw quarkus:dev
```

```text
... Listening on: http://localhost:8080 and https://localhost:8443
```

In another terminal (`-k` means "do not verify the certificate": only for a certificate you created yourself a minute ago):

```powershell
curl.exe -sk https://localhost:8443/api/info
curl.exe -s https://localhost:8443/api/info
curl.exe -i http://localhost:8080/api/hello
curl.exe -sk -D - -o NUL https://localhost:8443/api/hello
```

```bash
curl -sk https://localhost:8443/api/info
curl -s https://localhost:8443/api/info; echo "exit $?"
curl -i http://localhost:8080/api/hello
curl -sk -D - -o /dev/null https://localhost:8443/api/hello
```

```text
{"name":"Lab 22","apiKeyFingerprint":"b252a518"}      <- the first call

(nothing printed, curl exit code 60)                  <- the second call: the certificate is not trusted

HTTP/1.1 301 Moved Permanently
Location: https://localhost:8443/api/hello             <- the third call

HTTP/1.1 200 OK
X-Frame-Options: DENY
Strict-Transport-Security: max-age=31536000; includeSubDomains   <- among the headers of the fourth call
```

Now the packaged application (stop dev mode first). The production keystore and secrets come from the environment:

```powershell
.\mvnw.cmd package '-DskipTests'
keytool -genkeypair -alias lab -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore $env:TEMP\prod-keystore.p12 -storepass prod-pw-123 -dname "CN=localhost" -ext "SAN=dns:localhost" -validity 2
java -jar target\quarkus-app\quarkus-run.jar
$env:APP_API_KEY = 'prod-key-abc123'
$env:QUARKUS_TLS_KEY_STORE_P12_PATH = "$env:TEMP\prod-keystore.p12"
$env:QUARKUS_TLS_KEY_STORE_P12_PASSWORD = 'prod-pw-123'
java -jar target\quarkus-app\quarkus-run.jar
```

```bash
./mvnw package -DskipTests
keytool -genkeypair -alias lab -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore /tmp/prod-keystore.p12 -storepass prod-pw-123 -dname "CN=localhost" -ext "SAN=dns:localhost" -validity 2
java -jar target/quarkus-app/quarkus-run.jar
APP_API_KEY=prod-key-abc123 QUARKUS_TLS_KEY_STORE_P12_PATH=/tmp/prod-keystore.p12 QUARKUS_TLS_KEY_STORE_P12_PASSWORD=prod-pw-123 java -jar target/quarkus-app/quarkus-run.jar
```

The first `java -jar` (nothing configured) fails with:

```text
SRCFG00014: The config property app.api-key is required but it could not be found in any config source
```

Try also with `APP_API_KEY` alone: it fails too, with `Cannot set quarkus.http.insecure-requests without enabling SSL.` The second `java -jar` of the blocks above (all three variables) starts (`Listening on: http://0.0.0.0:8080 and https://0.0.0.0:8443`). Call it:

```powershell
curl.exe -sk -o NUL -w "%{http_code}`n" -H "X-Api-Key: prod-key-abc123" https://localhost:8443/api/partner/data
curl.exe -sk -o NUL -w "%{http_code}`n" -H "X-Api-Key: dev-only-key" https://localhost:8443/api/partner/data
```

```bash
curl -sk -o /dev/null -w "%{http_code}\n" -H "X-Api-Key: prod-key-abc123" https://localhost:8443/api/partner/data
curl -sk -o /dev/null -w "%{http_code}\n" -H "X-Api-Key: dev-only-key" https://localhost:8443/api/partner/data
```

```text
200
401
```

Finally, secrets from a file (the way a platform mounts a secret). Create `secrets/application.properties` **outside the repository** with the three lines, and point `SMALLRYE_CONFIG_LOCATIONS` at it with a `file:` URI:

(On Windows write the keystore path with forward slashes, for example `C:/Users/you/AppData/Local/Temp/prod-keystore.p12`.)

```text
app.api-key=from-file-xyz
quarkus.tls.key-store.p12.path=/tmp/prod-keystore.p12
quarkus.tls.key-store.p12.password=prod-pw-123
```

```powershell
$env:SMALLRYE_CONFIG_LOCATIONS = 'file:///C:/secrets'
java -jar target\quarkus-app\quarkus-run.jar
```

```bash
SMALLRYE_CONFIG_LOCATIONS=file:///etc/lab-secrets java -jar target/quarkus-app/quarkus-run.jar
```

> [!TIP]
> Clean the environment variables of your PowerShell session when you finish (`Remove-Item Env:APP_API_KEY` and so on), and delete the files you created outside the repository.

## 💼 In real projects
- **Often TLS ends before your application.** An ingress, a load balancer or a service mesh holds the certificate and talks plain HTTP to the pod. Then the application needs the forwarded-headers settings (`quarkus.http.proxy.*`) so that redirects and absolute URLs are right. Check the HTTP reference for your platform before deciding where to terminate.
- **Certificates expire.** Automate issuing and renewal (cert-manager, ACME, the platform's certificate service). The TLS registry can reload a changed key store on a period (`quarkus.tls.reload-period`) so a renewal does not need a restart. Know the expiry date of every certificate you own.
- **HSTS is a one-way door.** After a browser sees `Strict-Transport-Security` it refuses plain HTTP to that domain for the whole `max-age`. Start with minutes, raise it when you are sure, and think before `includeSubDomains` or preloading.
- **`Content-Security-Policy: default-src 'none'`** suits a JSON API. A site that serves HTML needs a policy written for the resources it loads; start in report-only mode.
- **Removing a secret from the file does not remove it from history.** Every clone still has it. A committed secret is a **leaked** secret: rotate it first, clean up second. Add a scanner (gitleaks, GitHub secret scanning) to the pipeline; this lab's scanner test knows your property names, the pipeline scanner knows the shape of tokens.
- **The jar is a copy of your repository's `src/main/resources`.** Anyone with the jar or the image has `application.properties`. Throw-away `%dev`/`%test` values are fine there, real ones are not.
- **Where secrets come from:** environment variables are easy but visible to anything that can read the process environment; a mounted file from the platform's secret store (Kubernetes Secret, a vault agent) is better; a vault the application reads at runtime is better still when you have one. The code does not change in any of them: it asks for `app.api-key`.
- **Never log or return a secret.** A fingerprint (like `/api/info`) lets operators check *which* key is deployed without exposing it. Compare secrets in constant time (`MessageDigest.isEqual`), as `PartnerResource` does.
- **Fail fast.** An application that cannot find its secret must refuse to start (task 4), not start and fail on the first request.

## 🚀 Stretch goal
A legacy partner can only speak TLS 1.2. Allow it for the service, look at which test turns red and why that is the right alarm, and explain what you gave up. Then replace the PKCS12 keystore of the `test` profile by PEM files (the generator in `LabCertificates` can produce them; look at the key store settings for PEM in the TLS registry guide).

## 💡 Hints
<details><summary>Hint 1 (direction)</summary>

Everything is `application.properties`. Task 1: the family `quarkus.tls.key-store.p12.*` with a profile prefix (`%test.`, `%dev.`), and one `quarkus.http.insecure-requests` value. Task 2: `quarkus.http.header."<Header-Name>".value`. Task 3: delete one line, add two profile-prefixed lines.
</details>

<details><summary>Hint 2 (more concrete)</summary>

`%test.quarkus.tls.key-store.p12.path=target/certs/lab-keystore.p12`, `%test.quarkus.tls.key-store.p12.password=lab-pw`, the same for `%dev.` with the dev file and `dev-only-pw`, `quarkus.http.insecure-requests=redirect`, `quarkus.http.header."X-Frame-Options".value=DENY`, `%dev.app.api-key=dev-only-key`, `%test.app.api-key=test-only-key`.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1 and Java 21.

**Task 1.**
- (a) **The application does not start.** In the jar: `Cannot set quarkus.http.insecure-requests without enabling SSL.` In the tests, the same failure shows as `Failed to start quarkus`.
- (b) **No.** The default of `quarkus.tls.protocols` is **`TLSv1.3` only**. That is why `refusesTls12` needs no configuration: it is a test of the default. With `quarkus.tls.protocols=TLSv1.3,TLSv1.2` the TLS 1.2 handshake succeeds and that test turns red.
- `negotiatesTls13` needs no configuration either once HTTPS works. The two tests that do need it are `answersOverHttps` and `plainHttpIsRedirectedToHttps`.
- `insecure-requests` values: with `redirect`, plain HTTP answers **301** with `Location: https://localhost:8444/api/hello` (the test port); without the property HTTP answers 200 normally (the redirect test fails); with `disabled` the HTTP port is closed (`Connection refused`).
- A client that does not have the certificate in its truststore gets an `SSLException` (curl: exit code 60, no data).

**Task 2.**
- A 404 carries the headers too (`errorResponsesCarryThemToo`).
- Removing `Cache-Control` turns 3 tests red; removing HSTS turns 1 red. For the partner endpoint `no-store` matters because a shared proxy or the browser must not keep a response that was only meant for a key holder.

**Task 3.**
- Deleting the line and adding nothing: the application **does not start** (`SRCFG00014`), so the whole suite fails at boot. That is the fail-fast behavior you want in production.
- `X-Api-Key: test-only-key ` with a trailing space is a **200**: HTTP strips the whitespace around a header value, so the server sees the exact key. (Do not write a test that expects 401 for it.)
- `AppConfig` printed with `toString()` shows `lab.melostudy.AppConfig$$CMImpl@...`, not the values: logging the mapping object does not leak the key (checked; it is still a good habit to never log it).

**Task 4.**
- Nothing configured: `SRCFG00014: The config property app.api-key is required but it could not be found in any config source`. Only the key: `Cannot set quarkus.http.insecure-requests without enabling SSL.` Both fail at **startup**.
- With the three environment variables: started on `http://0.0.0.0:8080` and `https://0.0.0.0:8443`; `/api/info` showed a fingerprint of the production key; the six headers were present; HTTP answered 301 to `https://localhost:8443/api/hello`; the right key gave 200, `dev-only-key` gave **401** (the `%dev` lines are in the jar but are not used by the `prod` profile); a client limited to TLS 1.2 could not connect; `curl` without `-k` failed with exit code 60.
- `SMALLRYE_CONFIG_LOCATIONS` works with a `file:` URI of the file or of its directory (`file:///C:/...`, `file:///etc/...`), and the secrets came from there. A plain Windows path fails: `SRCFG00033: Scheme C not supported`.
- Inside the jar, `application.properties` contained exactly the lines of the repository, including `%dev.quarkus.tls.key-store.p12.password=dev-only-pw`, `%dev.app.api-key=dev-only-key` and `%test.app.api-key=test-only-key`. A production secret left in the file would be there too.
</details>

## 🎤 Self-assessment
1. What does a TLS certificate prove, and why is a self-signed one fine for tests and wrong for production? What does `curl -k` skip?
2. Plain HTTP can be `enabled`, `redirect` or `disabled`. When would you pick each? Why is `redirect` not a protection against an attacker on the network on the very first request, and what does HSTS add?
3. Your application is behind an ingress that terminates TLS. Which parts of this lab still apply and which do not? What would break if the application did not know about the forwarded protocol?
4. A developer committed an API key and deleted it in the next commit. Is the problem solved? What do you do, in which order?
5. The jar contains the `%dev` and `%test` values. Why is that acceptable, and what rule would you give the team about what may appear in those profiles? How do secrets get to the production process, and why must the application fail at startup if one is missing?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
