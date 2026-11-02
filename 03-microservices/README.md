# 03 - Building Microservices with Quarkus

Udemy course: [Building Microservices with Quarkus](https://www.udemy.com/course/quarkus-building-microservices-with-quarkus/) (Antonio Goncalves).

This lane is **independent from `labs/`**: you move at the pace of the course, without AI by default.

## Conventions

- One Maven project per service, in its own folder: `03-<NN>-<name>/` (for example `03-01-catalog/`).
- Same as `00`, `01` and `02`: independent projects with their own `mvnw` and Quarkus BOM. There is **no parent POM**.
- [`pom.xml`](pom.xml) is an **aggregator only**: it lets you build every service at once and import them together in the IDE. Add a `<module>` line for each new service.
- Base package: `lab.melostudy`.
- Reference version: Quarkus `3.40.1.1` (LTS, supported until 2027-09-30), Java 21. The course may use a different version; write the differences down in [`NOTES.md`](NOTES.md).
- Shared infrastructure (database, broker, etc.): a `docker-compose.yml` will be added to this folder when it is needed.
- Commits: `feat(lab_3): <description>`, one commit per module.

## Modules

| # | Module | Folder | Status |
|---|--------|--------|--------|
| - | _Fill in with the course syllabus_ | | |

## If I need help from an AI assistant

Optional. Ask it to act as a tutor: explain the why and give hints before code, or compare what the course teaches with the current Quarkus documentation.
For guided work with AI support use [`../labs`](../labs/SYLLABUS.md) (its rules are in [`../labs/AGENTS.md`](../labs/AGENTS.md)).
