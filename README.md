# Quarkus notebooks
Supersonic Subatomic Java

## Materials
- Self study
- Udemy courses by Antonio Goncalves: 
  - [Starting with Quarkus](https://www.udemy.com/course/quarkus-starting-with-quarkus)
  - [Building Microservices with Quarkus](https://www.udemy.com/course/quarkus-building-microservices-with-quarkus/)
  - [Accessing Relational Databases with Quarkus](https://www.udemy.com/course/quarkus-accessing-relational-databases-with-quarkus/)

## Repository layout
| Folder | Content |
|---|---|
| `00-getting-started`, `01-rest-book`, `02-persistence` | Projects from the first two Udemy courses (closed) |
| `03-microservices` | Third Udemy course, followed at its own pace |
| [`labs`](labs/SYLLABUS.md) | Guided, up-to-date Quarkus labs (starter + solution). Rules for AI agents in [`labs/AGENTS.md`](labs/AGENTS.md) |

## Resources
- http://code.quarkus.io/

## General Commands
- Create basic demo project
```shell script
mvn io.quarkus.platform:quarkus-maven-plugin:3.39.5:create "-DprojectGroupId=lab.melostudy" "-DprojectArtifactId=getting-started" "-Dextensions='rest'"
```

