# 🚀 Quarkus notebooks
Supersonic Subatomic Java

A personal study repository: courses already completed, a course in progress, and a set of **guided, up-to-date labs** (Quarkus 3.40 LTS, Java 21) built to reach job-level Quarkus skills.

## 🗂️ Repository layout
| Folder | Content | Status |
|---|---|---|
| `00-getting-started`, `01-rest-book`, `02-persistence` | Projects from the first two Udemy courses | Closed (kept as they are) |
| [`03-microservices`](03-microservices/README.md) | Third Udemy course, at its own pace, without AI by default | In progress |
| [`labs`](labs/SYLLABUS.md) | 30 guided labs: each one has a README, a `starter/` with failing tests and a `solution/` | Labs 01-10 built, being reviewed by the learner |

## 🧪 The labs in a nutshell
The [syllabus](labs/SYLLABUS.md) covers, in nine blocks: fundamentals (dev mode, configuration, CDI), REST (validation, OpenAPI, REST Client), persistence, testing, reactive and messaging, security, resilience and observability, delivery (containers, native, Kubernetes, CI) and a capstone project.

How a lab works:
1. Open the lab's `README.md` (scenario, tasks, hints, self-assessment).
2. Open a terminal **inside its `starter/` folder** and run the tests: they fail.
3. Make them pass, task by task. Predict before you run when the README asks you to.
4. Compare with `solution/` when you are done (it is visible on purpose).

```powershell
cd labs\lab-03-cdi\starter
.\mvnw.cmd test
```

```bash
cd labs/lab-03-cdi/starter
./mvnw test
```

## ✅ Prerequisites
- Java 21 (the Maven wrapper in each project downloads Maven itself).
- PowerShell on Windows or any Bash shell. In Git Bash `./mvnw` can fail because of `JAVA_HOME`; use PowerShell with `.\mvnw.cmd`.
- A container runtime (Docker or Podman) from lab 08 on, for Dev Services and container labs.

## 🤖 Working with AI agents
The labs are built and maintained with an AI coding agent, and the rules are written **tool-neutral** in [`AGENTS.md`](AGENTS.md) (repository map) and [`labs/AGENTS.md`](labs/AGENTS.md) (quality standard, README style, workflow, sources and safety). `CLAUDE.md` files only import them, so any other agent can use the same rules. Progress lives in the syllabus and in git history, so a new session can pick up from there.

`labs/verify.ps1` checks that every lab keeps its promise: green `solution`, failing `starter`.

```powershell
cd labs
.\verify.ps1
```

## 📚 Materials
- Self study
- Udemy courses by Antonio Goncalves:
  - [Starting with Quarkus](https://www.udemy.com/course/quarkus-starting-with-quarkus)
  - [Building Microservices with Quarkus](https://www.udemy.com/course/quarkus-building-microservices-with-quarkus/)
  - [Accessing Relational Databases with Quarkus](https://www.udemy.com/course/quarkus-accessing-relational-databases-with-quarkus/)

## 🔗 Resources
- [Quarkus guides](https://quarkus.io/guides/) (the reference used by the labs)
- [code.quarkus.io](https://code.quarkus.io/) (project generator)

## ⌨️ General commands
Create a basic project (version used by the labs: 3.40.1.1 LTS):

```powershell
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.1:create "-DprojectGroupId=lab.melostudy" "-DprojectArtifactId=getting-started" "-Dextensions=rest"
```

## 📝 Conventions
- Everything in the repository is written in English.
- Commits: `feat(lab_N): ...`, `docs(...)`, `chore(...)`. Code is pushed at milestones (a finished lab or block).
- Secrets and `.env` files are never committed.
