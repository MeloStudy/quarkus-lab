# 🌐 Lab 15 - HTTP for production

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
> **Docker (or Podman) must be running.** The tests start a real PostgreSQL in a container (Dev Services), as in labs 08 to 12.

## 🎯 Why it matters
A REST endpoint that "works" in a demo breaks in production in four ways that have nothing to do with business logic. Two people edit the same document and the second save silently erases the first (**lost update**). A front-end served from another origin cannot call the API (**CORS**). Every client downloads the same unchanged document a thousand times a day (**no caching validators**). A file upload is stored under the name and type the client announced, with whatever size (**untrusted input**). HTTP already has the vocabulary for all of this: validators (`ETag`), preconditions (`If-Match`, `If-None-Match`), status codes (`304`, `412`, `428`, `413`, `415`), `Content-Disposition`, `Sunset`. This lab uses it.

## 🧠 Concepts
- **ETag:** an opaque identifier of "this version of this representation". Here it is the `@Version` of the entity (lab 10 used the same number for pessimistic vs optimistic locking; now it crosses the wire).
- **`If-None-Match` (reads):** "send it only if it is not the version I already have" → `304 Not Modified`, no body.
- **`If-Match` (writes):** "apply this change only if the current version is still the one I read" → `412 Precondition Failed` otherwise. Requiring it makes lost updates impossible; answering `428 Precondition Required` when it is missing teaches clients to send it.
- **CORS:** a browser rule, enforced by the browser with the help of response headers. The server decides which origins may call it, with which methods and headers, and which response headers JavaScript may read.
- **Multipart upload:** the file arrives as a temp file plus metadata the client controls (name, content type, size). None of that is a fact; it is a claim.
- **Versioning and deprecation:** a new representation next to the old one, and headers that tell old clients when it ends.

| Status | Meaning here |
|---|---|
| `304` | The client's cached copy is still valid |
| `412` | `If-Match` no longer matches: someone changed it first |
| `428` | The write did not say which version it is based on |
| `413` | The body is bigger than the limit |
| `415` | The file's content type is not accepted |

## 📚 Documentation
- [Quarkus - CORS filter](https://quarkus.io/guides/security-cors)
- [Quarkus - Writing REST services](https://quarkus.io/guides/rest) (multipart, `Request`, returning files)
- [Quarkus - HTTP reference](https://quarkus.io/guides/http-reference) (limits, uploads directory)
- [RFC 9110 - HTTP semantics: conditional requests](https://www.rfc-editor.org/rfc/rfc9110.html)
- [RFC 6585 - 428 Precondition Required](https://www.rfc-editor.org/rfc/rfc6585)
- [RFC 8594 - The Sunset header](https://www.rfc-editor.org/rfc/rfc8594)
- [MDN - CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS) and [MDN - Conditional requests](https://developer.mozilla.org/en-US/docs/Web/HTTP/Conditional_requests)

## 🧭 Scenario
`starter/` is a small documents API under `/api/v1/documents`: create, read, replace, and attach files. Documents have a JPA `@Version` that nothing uses yet. Run the tests: most are red, and each one names a way this API is not ready for production. The tests talk plain HTTP and look at headers and status codes only.

You change `DocumentResource`, add what the tasks ask, and edit `application.properties`. The file `TestSupport` (in `src/test`) is a helper; do not edit it.

```powershell
.\mvnw.cmd test
```

```bash
./mvnw test
```

## ✅ Tasks

### Task 1 - Validators on reads
**Goal:** every representation of a document carries an `ETag`, and a client can ask "only if changed". **Validated by:** `ConditionalGetTest`.

- [ ] `POST` (201) and `GET` answer with `ETag` derived from the document's `version`.
- [ ] `GET` with `If-None-Match: <current ETag>` answers `304` with no body (and still the `ETag`). With another value, `200` with the body.
- [ ] Use the JAX-RS `Request` (`@Context`) and its precondition evaluation instead of parsing headers by hand.

🔮 **Predict first:** a document is updated and its version goes from 0 to 1. What happens to a client that sends `If-None-Match` with the old ETag?

```powershell
.\mvnw.cmd test "-Dtest=ConditionalGetTest"
```

### Task 2 - Lost updates are an HTTP error
**Goal:** a write must name the version it changes. **Validated by:** `ConcurrencyTest`.

- [ ] `PUT` without `If-Match` → `428`.
- [ ] `PUT` with an `If-Match` that is not the current ETag → `412`, nothing is written, and the response carries the **current** `ETag` so the client can refetch and retry.
- [ ] `PUT` with the current ETag → `200` with the new ETag.
- [ ] Eight clients send the same `PUT` at the same time with the same ETag: exactly one gets `200` and the other seven get `412`. Not `500`.

> [!WARNING]
> Checking `If-Match` before writing is not enough for the last bullet: two requests can both pass the check and then race to write. Run `ofManySimultaneousWritesWithTheSameEtagExactlyOneWins` several times and read what the losers get.

🔮 **Predict first:** the check passes for two requests at once. What does Hibernate do with the second `UPDATE`, and what does the client see if nobody handles it?

```powershell
.\mvnw.cmd test "-Dtest=ConcurrencyTest"
```

### Task 3 - CORS for the front-end
**Goal:** a single known front-end (`http://localhost:3000`) can use the API from a browser. **Validated by:** `CorsTest`.

- [ ] Allow only that origin, only the methods the front-end uses (`GET`, `POST`, `PUT`) and only the request headers it needs (`Content-Type`, `If-Match`, `If-None-Match`).
- [ ] Let the browser read `ETag`, `Location` and `Content-Disposition` from responses.
- [ ] Cache preflight answers for an hour.

> [!WARNING]
> `ETag` is not one of the response headers that JavaScript can read by default. A front-end that cannot read it cannot send `If-Match`: the API "works" in `curl` and is unusable from the browser.

🔮 **Predict first:** you enable the CORS filter but list no origins. What does the front-end get? And what does `curl` (no `Origin` header) get?

```powershell
.\mvnw.cmd test "-Dtest=CorsTest"
```

### Task 4 - Files in and out
**Goal:** uploads and downloads that do not trust the client. **Validated by:** `UploadTest`.

- [ ] A file goes in (`multipart/form-data`, part name `file`) and comes out byte for byte, with `Content-Type`, `Content-Length` and `Content-Disposition: attachment; filename="..."`.
- [ ] The file is stored under a **name you generate**, never under the client's. The client's name is kept as metadata, reduced to a plain, safe file name (it ends up in a response header).
- [ ] Only `application/pdf`, `image/png` and `text/plain` are accepted; anything else is `415` and nothing is stored. A request without the `file` part is `400`.
- [ ] Bodies bigger than 1 MB are refused (configuration, not code) and nothing is stored.
- [ ] An attachment is only reachable through the document it belongs to.

> [!WARNING]
> Run the download test against the starter and look at **what the bytes are**. The starter "works" and the body is not the file.

🔮 **Predict first:** two users upload a file called `report.pdf`. What does the starter do?

```powershell
.\mvnw.cmd test "-Dtest=UploadTest"
```

### Task 5 - Version 2 and a goodbye note for version 1
**Goal:** a new representation next to the old one, and notice for the old one. **Validated by:** `VersioningTest`.

- [ ] `GET /api/v2/documents/{id}` returns `{id, name, content}` (the field is called `name` now; `DocumentV2` is given). It has an `ETag` like v1, and the same document gives the same ETag in both versions.
- [ ] Every response under `/api/v1/` (including errors) carries `Deprecation: true` and `Sunset: <date>`; v2 carries neither. Do it once, outside the resources, with the end date in configuration.

```powershell
.\mvnw.cmd test "-Dtest=VersioningTest"
```

### Task 6 - Outside the tests: look at it with `curl`
**Goal:** see the headers a client really sees. No test validates this task.

```powershell
.\mvnw.cmd quarkus:dev
```

```bash
./mvnw quarkus:dev
```

In another terminal (single quotes keep the double quotes of the ETag in PowerShell):

```powershell
curl.exe -i -X POST -H "Content-Type: application/json" -d '{\"title\":\"Plan\",\"content\":\"v1\"}' http://localhost:8080/api/v1/documents
curl.exe -i -H 'If-None-Match: "0"' http://localhost:8080/api/v1/documents/1
curl.exe -i -X PUT -H "Content-Type: application/json" -d '{\"title\":\"Plan\",\"content\":\"v2\"}' http://localhost:8080/api/v1/documents/1
curl.exe -i -X PUT -H "Content-Type: application/json" -H 'If-Match: "0"' -d '{\"title\":\"Plan\",\"content\":\"v2\"}' http://localhost:8080/api/v1/documents/1
curl.exe -i -X PUT -H "Content-Type: application/json" -H 'If-Match: "0"' -d '{\"title\":\"Plan\",\"content\":\"v3\"}' http://localhost:8080/api/v1/documents/1
curl.exe -i -X OPTIONS -H "Origin: http://localhost:3000" -H "Access-Control-Request-Method: PUT" -H "Access-Control-Request-Headers: if-match" http://localhost:8080/api/v1/documents/1
```

```bash
curl -i -X POST -H "Content-Type: application/json" -d '{"title":"Plan","content":"v1"}' http://localhost:8080/api/v1/documents
curl -i -H 'If-None-Match: "0"' http://localhost:8080/api/v1/documents/1
curl -i -X PUT -H "Content-Type: application/json" -d '{"title":"Plan","content":"v2"}' http://localhost:8080/api/v1/documents/1
curl -i -X PUT -H "Content-Type: application/json" -H 'If-Match: "0"' -d '{"title":"Plan","content":"v2"}' http://localhost:8080/api/v1/documents/1
curl -i -X PUT -H "Content-Type: application/json" -H 'If-Match: "0"' -d '{"title":"Plan","content":"v3"}' http://localhost:8080/api/v1/documents/1
curl -i -X OPTIONS -H "Origin: http://localhost:3000" -H "Access-Control-Request-Method: PUT" -H "Access-Control-Request-Headers: if-match" http://localhost:8080/api/v1/documents/1
```

- [ ] Create a 2 MB file and upload it. What does `curl` report?

```powershell
$fs = [IO.File]::Create("$env:TEMP\big.bin"); $fs.SetLength(2MB); $fs.Close()
curl.exe -i -F "file=@$env:TEMP\big.bin;type=text/plain" http://localhost:8080/api/v1/documents/1/attachments
```

```bash
head -c 2097152 /dev/zero > /tmp/big.bin
curl -i -F "file=@/tmp/big.bin;type=text/plain" http://localhost:8080/api/v1/documents/1/attachments
```

**Success criteria:** `.\mvnw.cmd verify` inside `starter/` ends green.

## 👀 What you should see
The sequence of Task 6, one line per request:

```text
201 Created              ETag: "0"   Deprecation: true   Sunset: Wed, 31 Dec 2027 23:59:59 GMT
304 Not Modified         ETag: "0"
428 Precondition Required
200 OK                   ETag: "1"
412 Precondition Failed  ETag: "1"
200 OK  (preflight)      access-control-allow-origin: http://localhost:3000
                         access-control-allow-methods: GET,POST,PUT
413 Request Entity Too Large   (the 2 MB upload)
```

## 💼 In real projects
- **`If-Match` + `428` is the cheapest correct answer to concurrent editing.** The alternative, "last write wins", is a bug that only shows with two users. Optimistic locking in the database (`@Version`) and `If-Match` over HTTP are the same idea at two layers: use both, the first for correctness under races, the second to tell the client.
- **ETags and caches:** `ETag` + `Cache-Control` let browsers and CDNs revalidate with `If-None-Match` and save the body. A *weak* ETag (`W/"..."`) says "semantically equivalent"; RFC 9110 says `If-Match` must use the strong comparison, so use strong ETags for anything a client may send back in `If-Match`. Quarkus accepted both in the tests of this lab, but do not rely on that.
- **CORS is not security for the API.** It protects the user's browser from other sites; `curl` ignores it. Never `*` with credentials, never reflect any `Origin`, list production origins explicitly (per environment, from configuration), and keep `Vary: Origin` (on by default) when a CDN is in front.
- **Preflight cost:** every cross-origin `PUT` with a custom header costs an extra `OPTIONS` round trip unless `access-control-max-age` lets the browser cache it.
- **Uploads are the most attacked feature of most APIs.** Never trust name, type or size. Generate the stored name; allow-list the types (and, for anything dangerous, check the content, not the header); cap the size at the HTTP layer; store outside the web root; serve with `Content-Disposition: attachment` and `X-Content-Type-Options: nosniff`; scan files when the data is shared between users.
- **Large files do not belong in the database or in memory.** Stream to disk or to object storage (S3 and friends, often with a pre-signed URL so the bytes never touch your service).
- **Temp files:** the uploads directory (`quarkus.http.body.uploads-directory`) fills up if a request dies halfway; monitor it, and run it on a volume sized for the limit times the concurrency.
- **Versioning:** a version in the path (`/api/v1`) is simple, visible and cache friendly; headers or media types keep URLs stable but are harder to test and cache. Whatever you choose: additive changes need no new version, breaking ones do, and each retired version needs a date (`Sunset`) and a way to know who still calls it (log the `User-Agent`/client id of v1 calls).
- **Rate limiting and timeouts** are the other half of "HTTP in production": a gateway or a proxy in front usually owns them, but the API must answer `429`/`503` with `Retry-After` in a way clients can obey.

## 🚀 Stretch goal
Make `GET /api/v1/documents/{id}` cacheable by the browser with `Cache-Control: max-age=60, must-revalidate`, and write a test that shows the header. Then think about what a shared cache (CDN) would do with a response that has an `Authorization` header or a `Vary: Origin`. No hints.

## 💡 Hints
<details><summary>Hint 1 (task 1 and 2)</summary>

`jakarta.ws.rs.core.Request#evaluatePreconditions(EntityTag)` returns a `ResponseBuilder` when a precondition fails (304 for a read, 412 for a write) and `null` when the request may proceed. That `null` also covers "no precondition header at all": for writes, that case is yours to handle.
</details>

<details><summary>Hint 2 (task 2, the race)</summary>

Two transactions read version 0. Both pass your check. Only one `UPDATE ... WHERE version = 0` matches a row. Force the `UPDATE` to run inside the method (`flush()`), and look for the exception type that Hibernate raises for the loser (`jakarta.persistence`). A `@ServerExceptionMapper` turns it into the status that means "the precondition is no longer true".
</details>

<details><summary>Hint 3 (task 3)</summary>

Everything is `quarkus.http.cors.*`: `enabled`, `origins`, `methods`, `headers`, `exposed-headers`, `access-control-max-age`.
</details>

<details><summary>Hint 4 (task 4)</summary>

`FileStorage` already generates names and moves temp files. The download needs a media type on the **method** that cannot be mistaken for JSON, and the real `Content-Type` as a header.
</details>

<details><summary>Hint 5 (task 5)</summary>

A `ContainerResponseFilter` (`@Provider`) sees every response, errors included; `request.getUriInfo().getPath()` tells it which version was called.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running it on Quarkus 3.40.1.1.

**Task 1.**
- `Request.evaluatePreconditions(tag)` answers `304` (with the `ETag`, no body) when `If-None-Match` has the current tag or `*`, and returns `null` otherwise. An `If-None-Match` with an old tag gets `200` and the new body: the validator is the version, and the version changed.

**Task 2.**
- `evaluatePreconditions` returns `null` when the request has **no** `If-Match` at all, so on its own it lets `PUT` through. The `428` is a separate check.
- The `412` produced by `evaluatePreconditions` already contains the current `ETag`.
- Without a mapper for `OptimisticLockException`, the simultaneous writers did not all get `412`: in three runs, 7, 3 and 7 of the 7 losers got a different status (the unmapped exception ends as an error response of the server, not as a precondition failure). The requests that read the document after the winner committed are rejected by your own check (`412`); the ones that read it before pass the check and are rejected by the database. Mapping `OptimisticLockException` to `412` makes the answer the same in both cases, and `flush()` inside the method makes the exception happen where a mapper can see it, not later at commit.
- Quarkus also accepted an `If-Match` without quotes (`If-Match: 0`) and weak tags (`W/"0"`) in the tests. HTTP requires quoted strong tags for `If-Match`; send them that way.

**Task 3.**
- With `quarkus.http.cors.enabled=true` and **no** `origins`, the front-end gets `403` on the preflight and on the real request (`403` on the `POST`). A request without an `Origin` header is not affected at all.
- A disallowed origin gets `403` even for a plain `GET`.
- The preflight for `DELETE` is answered `200`, but `Access-Control-Allow-Methods` lists only `GET,POST,PUT`: it is the browser that refuses to send the `DELETE`. Same for a request header that is not in the list.
- Without `exposed-headers` the response has no `Access-Control-Expose-Headers`, and the test shows the `ETag` is invisible to a browser.
- A CORS response also carries `Vary: origin`, and `access-control-allow-credentials: true`.

**Task 4.**
- The starter's download has `@Produces(MediaType.WILDCARD)`. A wildcard includes JSON, so the JSON writer is chosen for the `File`, and the body is the **JSON string of the file's path on the server** (`"D:\\...\\test-storage\\..."`, 86 bytes instead of 300 KB). That leaks the server's directory layout, and the client never gets the file. With `@Produces(MediaType.APPLICATION_OCTET_STREAM)` on the method and the real type in the `Content-Type` header, the file streams with the right `Content-Length`.
- The `../evil.txt` upload did **not** escape the storage directory in the starter: Quarkus REST already reduces the client's file name to its last segment (`../evil.txt`, `..\evil2.txt` and `/tmp/abs.txt` arrived as `evil.txt`, `evil2.txt`, `abs.txt`). The starter's real defect is different: the client still chooses the name on disk, so a second upload called `report.txt` **overwrites** the first (the test shows `second` where `first` was expected).
- A file name with a quote in it (`a"; filename="b.exe`) made the starter answer `500` (on Windows that name is not a legal file name); on another system the file would be stored and a header built from the name would be broken or injectable. The solution keeps `[A-Za-z0-9._ -]` and replaces the rest.
- A 2 MB body with `quarkus.http.limits.max-body-size=1M`: `curl` receives `413 Request Entity Too Large`. A Java HTTP client (RestAssured) may instead see the connection reset while it is still writing the body and raise an exception; the test accepts both, because both mean "refused".

**Task 5.**
- `request.getUriInfo().getPath()` starts with `/` in the filter (`/api/v1/...`), and the filter also runs for `404` and other error responses.
</details>

## 🎤 Self-assessment
1. What problem do `ETag` + `If-Match` solve, and what is the difference between `412` and `428`?
2. Two requests pass the `If-Match` check at the same moment. What guarantees that only one of them writes, and what status does the other get?
3. Why can `curl` call an API that the browser cannot? What does CORS actually protect?
4. You get an upload with `filename="../../etc/passwd"`, `Content-Type: image/png` and 3 GB. Which of those three values do you trust, and how do you handle each?
5. You must change a field name in a public API. Describe how you do it without breaking existing clients and how you retire the old shape.

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand?
- What was hard?
- What should I reinforce?
