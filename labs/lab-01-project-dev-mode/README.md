# 🧩 Lab 01 - Project and dev mode

| ⏱️ Duration | 🚀 Quarkus | ☕ Java | 🧪 Type |
|---|---|---|---|
| 1.5 h | 3.40.1.1 LTS | 21 | A (starter + solution) |

## 📍 Where to run commands
Open a terminal **inside this lab's `starter/` folder** and check where you are before running anything:

```powershell
Get-Location
```

```bash
pwd
```

> [!NOTE]
> Commands are shown in PowerShell first (`.\mvnw.cmd`) and Bash second (`./mvnw`). In Git Bash, `./mvnw` may fail because of `JAVA_HOME`; use the system `mvn` instead (for example `mvn quarkus:dev`).

## 🎯 Why it matters
Daily work in Quarkus revolves around **dev mode**: you edit, save, refresh and see the change without restarting anything. Mastering it, together with the **Dev UI** and extension management, is the foundation for everything else.

## 🧠 Concepts
- Structure of a Quarkus project (`pom.xml`, platform BOM, `src/main`, `src/test`, `application.properties`)
- Dev mode and live reload (`quarkus:dev`)
- Continuous testing (press `r` in the dev mode console)
- Dev UI (`http://localhost:8080/q/dev-ui/`)
- Extensions: how they are added and what changes in `pom.xml`
- `@QuarkusTest` + RestAssured

## 📚 Documentation
- [Getting Started](https://quarkus.io/guides/getting-started)
- [SmallRye Health](https://quarkus.io/guides/smallrye-health)
- [Quarkus CLI](https://quarkus.io/guides/cli-tooling)
- [Dev UI](https://quarkus.io/guides/dev-ui)

> [!NOTE]
> The public guides show the latest version (currently 3.40). Your project uses 3.40.1.1 (LTS), so they match.

## 🧭 Scenario
`starter/` is a freshly generated project (`rest` extension) with a single endpoint, `GET /hello`. Three tests fail. Your job is to turn them green **while working in dev mode**, not in a compile-and-run cycle.

## ✅ Tasks

### Task 0 - Start dev mode and explore
**Goal:** get comfortable with the dev mode console and the Dev UI.

- [ ] Start the project in dev mode from `starter/`:

```powershell
.\mvnw.cmd quarkus:dev
```

```bash
./mvnw quarkus:dev
```

- [ ] Open `http://localhost:8080/hello` and the Dev UI at `http://localhost:8080/q/dev-ui/`. Answer from what you see there:
  - Which extensions are installed?
  - Where are configuration properties edited?
  - Where do you see the exposed REST endpoints?

### Task 1 - Live coding
**Goal:** change behavior without restarting. **Validated by:** `GreetingResourceTest`.

- [ ] With the app running, change the text of `GET /hello` to `Hello from Quarkus`. Refresh the browser; do **not** restart.

### Task 2 - A new endpoint
- [ ] Create `GET /hello/{name}` that responds `Hello, {name}!`.

### Task 3 - Add an extension without touching `pom.xml`
- [ ] Add the `smallrye-health` extension **without editing `pom.xml` by hand**. Check that `/q/health` responds `UP`.

### Task 4 - Continuous testing
- [ ] In the dev mode console press `r` to run the tests continuously and watch them turn green.

### Task 5 - Run outside dev mode
**Goal:** run what is actually deployed. Stop dev mode first (`q` or `Ctrl+C`).

🔮 **Predict first:** which profile will be active? Will `/hello`, `/q/health` and `/q/dev-ui/` respond?

```powershell
.\mvnw.cmd package -DskipTests
java -jar target\quarkus-app\quarkus-run.jar
```

```bash
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

> [!WARNING]
> What you saw in dev mode is not what runs in production. Before you read the answer, check which of the endpoints you used in the previous tasks still respond.

- [ ] Look inside `target/quarkus-app/`: what would you have to copy to a server to deploy?

**Success criteria:** the 3 tests in `GreetingResourceTest` pass and you have run the jar from Task 5.

```powershell
.\mvnw.cmd verify
```

```bash
mvn verify
```

## 👀 What you should see
- On startup, the console prints `Profile dev activated. Live Coding activated.` and the list of `Installed features`.
- After Task 3, `smallrye-health` shows up in `Installed features` without restarting the app.
- `GET /q/health` returns something like:

```json
{"status":"UP","checks":[]}
```

## 💼 In real projects
- **Dev mode is for development only:** it is never deployed. In the jar the profile is `prod`, and the Dev UI (`/q/dev-ui/`) does not exist (it returns 404), but `/q/health` does.
- **You deploy the whole `target/quarkus-app/` folder** (`quarkus-run.jar` plus `lib/`, `app/` and `quarkus/`), not just the executable `.jar`. It is normally copied into a container image (lab 33).
- **Health endpoints are the orchestrator's probes:** in Kubernetes, `/q/health/live` and `/q/health/ready` are usually the *liveness* and *readiness probes*.
- **Do not pin extension versions by hand:** `pom.xml` imports a platform BOM that manages them. Upgrading Quarkus means changing a single version (`quarkus.platform.version`).
- **Use the wrapper (`mvnw`)** so the whole team and CI build with the same Maven version.
- **Continuous testing** (`r`) replaces the "change, build, run tests" loop: use it whenever you work with tests.

## 🚀 Stretch goal
Create your own liveness `HealthCheck` that reports `UP` with an extra data item (`withData`) and find it at `/q/health/live`. Then look at what changed in the Dev UI.

## 💡 Hints
<details><summary>Hint 1 (direction)</summary>

For Task 3 think about the CLI command that manages extensions, or the equivalent Maven plugin goal. Look at what changes in `pom.xml` afterwards.
</details>

<details><summary>Hint 2 (more concrete)</summary>

- Extension (with the Quarkus CLI):

```bash
quarkus extension add smallrye-health
```

- Extension (with Maven):

```powershell
.\mvnw.cmd quarkus:add-extension "-Dextensions=smallrye-health"
```

```bash
./mvnw quarkus:add-extension -Dextensions=smallrye-health
```

- Path parameter: `@Path("/{name}")` + `@PathParam("name")`.
- If dev mode is already running, you do not need to restart it after adding the extension.
</details>

## ✔️ Verified answers
<details><summary>Open only after finishing the tasks</summary>

Everything here was checked by running the packaged jar of `solution/` on Quarkus 3.40.1.1.

**Task 5 - what runs outside dev mode.** The startup log says `Profile prod activated.` (no `Live Coding`), and the installed features are `cdi, rest, smallrye-context-propagation, smallrye-health, vertx`. Status codes of the endpoints:

| Endpoint (in the jar) | Status |
|---|---|
| `/hello` | 200 |
| `/hello/Ana` | 200 |
| `/q/health` and `/q/health/live` | 200 |
| `/q/dev-ui/` | **404** |

`/q/health` answers `{"status": "UP", "checks": []}` (the layout of the JSON may differ: it is pretty-printed). The Dev UI, which you used in dev mode, does not exist in the jar.

**What to deploy.** `target/quarkus-app/` contains `quarkus-run.jar`, `lib/`, `app/` and `quarkus/` (plus `quarkus-app-dependencies.txt`). The whole folder is the deployable unit: copying only `quarkus-run.jar` somewhere else and running it fails with `ClassNotFoundException: io.quarkus.bootstrap.runner.QuarkusEntryPoint`.
</details>

## 🎤 Self-assessment
Answer without looking at the code; you should be able to explain each one in an interview.

1. What is the difference between dev mode and running the jar? Name at least three things that change.
2. Why is the version of `quarkus-rest` not declared in `pom.xml`?
3. What does continuous testing do, and how is it different from running `mvn test` yourself?
4. You are asked to make Kubernetes restart the pod if the application hangs. Which endpoint would you use and which extension does it come from?
5. What would you copy to a server to deploy the application?

The complete solution is in `solution/`.

## 🔁 Retrospective
- What did I understand about the dev mode cycle?
- What was hard?
- What should I reinforce?
