# 🚦 Production checklist - clinic appointments

This is the list a team would go through before the first real deployment. Every row says **what the repository already does** (and which test or run proves it) and **what is deliberately not done**. A "no" here is a decision, not an oversight: the project is a study showcase, and pretending it is production-ready would be the worst thing it could teach.

Legend: ✅ done and verified here · 🟨 done in the lab version, needs a real counterpart · ⬜ not done on purpose

## 1. Configuration: what the operator must provide
Nothing secret is in the image or in Git. The image reads its configuration from the environment; Kubernetes hands it over from a ConfigMap and a Secret (`quarkus.kubernetes.env.configmaps=clinic-config`, `env.secrets=clinic-secret`).

| Variable | Kind | What it is | If it is missing |
|---|---|---|---|
| `QUARKUS_DATASOURCE_JDBC_URL` | ConfigMap | the database | the application cannot start (no datasource) |
| `QUARKUS_DATASOURCE_USERNAME`, `QUARKUS_DATASOURCE_PASSWORD` | **Secret** | the database account | same |
| `NOTIFICATIONS_API_KEY` | **Secret** | the key of the notification provider | **exit code 1 at startup**, `SRCFG00014: The config property notifications.api-key is required` (verified on the jar) |
| `QUARKUS_REST_CLIENT_NOTIFICATIONS_URL` | ConfigMap | where the provider is | it points at `http://localhost:9090`: reminders fail with a connection error (and are retried), nothing else breaks |
| `MP_JWT_VERIFY_PUBLICKEY_LOCATION`, `MP_JWT_VERIFY_ISSUER`, `MP_JWT_VERIFY_AUDIENCES` | ConfigMap | how tokens are verified | the defaults point at the **throw-away public key inside the jar**: see section 2 |
| `CLINIC_TIME_ZONE`, `CLINIC_SLOT_MINUTES`, `CLINIC_HORIZON_DAYS`, `CLINIC_REMINDERS_*` | ConfigMap | the clinic and the reminders | defaults; a wrong zone stops the startup |

## 2. Security
| | Status | Notes |
|---|---|---|
| Every endpoint has a rule, the default is closed | ✅ | `deny-unannotated-endpoints`; `SecurityTest.anEndpointNobodyAnnotatedIsClosedEvenToReception` |
| Ownership: somebody else's appointment is a 404 | ✅ | `SecurityTest` (a test compares the body with a missing id) |
| Tokens: signature, expiry, issuer and audience | ✅ | five ways to be untrustworthy, five 401s |
| The signing key | ⬜ | The repository carries a **throw-away key pair** (the public key in the jar, the private one in `src/test/resources`). A real deployment verifies tokens with the public key (or the JWKS URL) of the identity provider and rotates it. Anybody who has this repository can mint a valid token for this build. |
| Who issues tokens | ⬜ | There is no login. Lab 21 shows the Keycloak/OIDC variant. |
| HTTPS | ⬜ | The service speaks HTTP; TLS ends at the ingress or the mesh (lab 22). |
| Rate limiting, abuse protection | ⬜ | Belongs to the gateway or ingress. |
| Audit trail (who cancelled what) | ⬜ | `updated_at` and `version` exist; a history table like P1's is the next step. |
| Personal data | 🟨 | Emails are in the database and in the message to the provider, **never in a log line or a metric tag** (`TracingAndLogsTest`, `MetricsTest`). No retention policy, no erasure on request: both are needed before real patients. |
| The container | 🟨 | Runs as the non-root user of the UBI image (lab 33). No image scan, no digest pinning. |

## 3. Data
| | Status | Notes |
|---|---|---|
| The schema belongs to Flyway; Hibernate only validates | ✅ | `schema-management.strategy=validate` |
| **Migrations run once, before the rollout, as a Job** | ✅ | The Kubernetes extension generates `clinic-flyway-init` and the pod waits for it. Verified in a cluster. **A finished Job cannot be applied again**: delete it first (README, "Deploy"). |
| Overlaps and invariants live in the database | ✅ | exclusion constraints, checks, unique indexes (`SchemaTest`) |
| Backups, point-in-time recovery, high availability | ⬜ | The lab database is a Deployment with an `emptyDir`. Use a managed service or an operator. |
| Migrations compatible with the previous version | ⬜ | A rolling update runs the OLD code against the NEW schema for a while: add columns first, remove them in a later release (expand and contract). Nothing enforces it here. |
| Connection pool sizing | 🟨 | Agroal defaults. Size it from replicas x pool <= database connections. |

## 4. Reliability
| | Status | Notes |
|---|---|---|
| More than one replica is safe | ✅ | reminders are claimed with `FOR UPDATE SKIP LOCKED` and a lease; `ReminderResilienceTest.severalInstancesPickingAtOnce...` runs four pickers at once. The lab cluster runs ONE replica to spare memory. |
| A provider outage does not take the clinic down | ✅ | readiness stays UP; booking works with the provider dead (verified on the jar and in `HealthTest`) |
| Each reminder goes out once | ✅ | idempotency key + a provider that counts; the lost-answer case is a test |
| Probes | ✅ | liveness, readiness and startup on the management port, generated by the extension |
| Requests and limits | 🟨 | 192 Mi / 100 m requested, 384 Mi limit, no CPU limit (lab 35). Measure under real load before copying them. |
| PodDisruptionBudget, autoscaling, anti-affinity | ⬜ | |
| Graceful shutdown with a request in flight | 🟨 | Quarkus stops on SIGTERM (lab 33); by design a reminder claimed by a pod that dies is picked again after its lease (2 minutes); that path and a rolling update are NOT tested. |
| The reminder poller on many instances | ✅ | no leader election needed (lab 13's caveat is answered by the claim) |

## 5. Observability
| | Status | Notes |
|---|---|---|
| Health and metrics on a separate port | ✅ | 9000, not exposed by the Service |
| Metrics with bounded tags | ✅ | `MetricsTest.noTagCarriesAnIdAnEmailOrATime` |
| Logs as JSON with the ids of the work | ✅ | `reminderId`, `appointmentId`, `trace_id`, `span_id` |
| Traces | 🟨 | One `reminder.send` span with the provider call as its child. **No exporter endpoint is configured** (`quarkus.otel.exporter.otlp.endpoint`): spans are created and thrown away until a collector exists. |
| Alerts | ⬜ | Proposed below; they need a Prometheus and an on-call. |

Alerts worth writing first (on the metrics that exist):

| Alert | Expression (idea) | Why |
|---|---|---|
| Reminders gave up | `clinic_reminders_failed > 0` for 10 min | a person must look: a bad address or a long outage |
| Reminders are late | `clinic_reminders_overdue_seconds > 600` | the job is stuck, or the provider is down and the breaker keeps it open |
| The breaker is open for a long time | readiness data `circuit = OPEN` for 15 min (or `clinic_notifications_call_seconds_count{outcome="breaker_open"}` growing) | the provider is down |
| Many refusals of one kind | `rate(clinic_requests_rejected_total{code="SLOT_TAKEN"}[5m])` far above normal | a client is hammering one slot, or the availability endpoint disagrees with the booking rules |
| Errors | the 5xx ratio of the HTTP server metrics | the classic |

## 6. Delivery
| | Status | Notes |
|---|---|---|
| A pipeline that builds, tests and can build the image | ✅ | `.github/workflows/project-3-ci.yml`, **manual on purpose**, one job, read-only token, pinned actions, runner `ubuntu-24.04` |
| It runs on every pull request and blocks the merge | ⬜ | The opposite of what a team wants; here it would cost minutes on every commit of a study repository. |
| The image goes to a registry, tagged by commit, deployed by the pipeline | ⬜ | Nothing is pushed anywhere (`quarkus.container-image.push=false` in the pipeline). |
| Environments (staging, production), approvals | ⬜ | |
| Rollback | 🟨 | `kubectl rollout undo` works for the Deployment (lab 35); the schema does not roll back with it. |

## 7. Product limits (not bugs)
- One clinic and one time zone. Working hours that change do not move appointments that were already booked.
- No waiting list, no recurring appointments, no practitioner leave, no reminder opt-out, no second channel.
- The reminder is addressed to the patient's email; a real SMS would also need a phone number and consent.
- Appointments cannot be changed by the practitioner (only completed or marked no-show).
- `/api/reminders` is not paged.

## 8. Things that surprised the builder (and will surprise the next one)
- `kubectl apply` of the generated manifest **fails the second time**, because the finished Job is immutable. The deploy procedure deletes it first. (Verified: even the same version fails.)
- The extension generates a `ServiceMonitor` when the Prometheus registry is present; a cluster without the Prometheus operator refuses the whole file. It is switched off here.
- The probes of a service with a management interface point at **port 9000**; the extension knew, and the Service does not expose that port on purpose.
