# `llm-service-http-contract` en el Skill Hub — estado vigente

**Estado (verificado 2026-09-20): publicado como v4, con el adjunto `../llm-service.openapi.yaml`**
(37.919 bytes — ver [`../skillhub/README.md`](README.md) para el hash). El PDF de estándar Kafka
no toca HTTP, así que esta es la última revisión relevante.

**Por qué:** el YAML adjunto en el hub pesa 42.714 bytes y no coincide con ninguna versión del repo (van de
29 a 38 KB), así que no se pudo comprobar qué dice del tutor; no hay forma de descargarlo sin la API key del MCP.

**`description`:** Canonical OpenAPI contract for synchronous communication with llm-service, including the tutor contract for practice-service.

**`when_to_use`:** Use when integrating practice-service, courses-service or admin-service with llm-service over HTTP; the AI tutor, its token and scope, Idempotency-Key, and the unavailable state.

**`tags`:** gateway, http, llm-service, microservices, openapi, practice-service, tutor, contracts

**`rationale`:** The attached OpenAPI (42,714 bytes) matches no version in the repo history, so its tutor section could not be checked. Replaced it with the current repo file, whose tutor request, response and error codes were aligned with the running service and verified with the test bot (status codes, idempotency, forced unavailable), and added the tutor rules practice-service needs. The real model was exercised only in a short manual run.

**`content`:**

````markdown
## Rule

Use the attached OpenAPI document as the canonical HTTP contract for synchronous integrations with llm-service. Route every request through the API Gateway and follow its authentication, correlation, idempotency and Problem Details rules.

## Tutor (practice-service)

`POST /api/llm/tutor/interactions`. The test bot and the real model share this exact contract: only the content of the answers changes, never their shape.

- **Token.** `client_credentials` with `audience: llm-service` and scope `llm.tutor.interact`. The Gateway adds the `X-*` identity headers: never send or trust them yourself.
- **Headers.** `Authorization: Bearer <token>` and `Idempotency-Key` (a new UUID for every student message).
- **Request.** Required: `attemptId`, `challengeId`, `courseCohortId`, `learnerId` (UUIDs), `message` (not blank), `riskLevel` (`low`, `medium`, `high`). Optional: `conversacionId` (groups turns) and `expectedSolution` (used in memory by the output guard only: never store, log or return it). Unknown fields are ignored.
- **Response `200`.** `message`, `state` (`completed`, `blocked`, `unavailable`), `conversacionId`. Any model failure (timeout, invalid answer, provider down, budget exhausted) returns `200` with `state: unavailable` and a fixed notice. `blocked` is reserved and not produced today, but handle it.
- **Errors** (`application/problem+json`). `401` wrong service or scope; `403` missing delegated identity; `409` same key while the first request is still running; `422` invalid body, or a key reused with a different body. Treat any other 4xx or 5xx as a failure.
- **Idempotency.** Same key and same body return the same response without a new model call. An `unavailable` response is stored under its key, so retry with a **new** key.
- **Timeout.** Worst case is about 25 s (three attempts of 8 s): use a 30 s client timeout.

## Reasoning

Turning every model failure into `200 unavailable` gives the student something to see and keeps the idempotency key from being stuck in progress. Open point: whether the Gateway adds the delegated user for a pure service token; a `403` with a valid token means it does not, so confirm it with the Gateway team. Other endpoints in the file serve courses-service and admin-service. Full guide for the practice-service team: `llm-service/docs/contracts/equipos/llm-service-contrato-para-desafios-practicos.md`.
````
