---
slug: api-gateway-contract-rules
title: API Gateway contract rules
description: 'Fixed rules a service''s synchronous HTTP contract must meet to pass the platform API Gateway:
  name levels, routing, identity from token, RFC 7807 errors, resources not RPC, pagination, versioning.'
when_to_use: writing or changing a service OpenAPI; HTTP endpoint design behind the gateway; public path
  prefix; M2M aud and scopes; Problem Details errors; pagination; resource vs RPC naming; API versioning
stack: shared
type: reference
owning_team: LLM
version: 3
tags:
- api-gateway
- contracts
- http
- openapi
- pagination
- rest
- rfc7807
- versioning
---

## Rule

This is a **reference to consult before writing or changing a service's OpenAPI**, not a generator. A service's synchronous HTTP contract must be shaped to pass the platform API Gateway. Correlation + idempotency headers are covered by [[request-correlation-across-http-and-kafka]]; the async counterpart is [[kafka-event-contract-rules]].

1. **Three name levels** — Git repo `tpi-<name>` (org only, not routed); Eureka / `spring.application.name` `<name>-service` (logical id); public Gateway prefix `/api/<name>/**`. The Gateway derives the prefix from `spring.application.name` (lowercase, drop `-service`).
2. **Routing** — the Gateway is the only door; nobody reaches the service directly. It does **not** rewrite the path (`/api/<n>/me` is forwarded as-is; the backend maps the full prefix). `/api/<n>/public/**` is open (`permitAll()` for documented routes only); `/api/<n>/**` requires a valid JWT at the Gateway, the service authorises by role/scope.
3. **M2M + identity** — scheme `serviceJwt` (`http` / `bearer` / `JWT`). The service validates `aud=<n>-service` and the operation's scope. Identity and tenancy (`courseCohortId`, `learnerId`, …) come from the token, **never** the body; if the client sends them they are ignored (document it). Missing/forged delegated identity → `403`.
4. **Errors — Problem Details (RFC 7807)** — one reusable `Problem` response; body has `type`, `title`, `status`, `detail`, plus a stable `codigo` field (typed, e.g. `cuota_agotada`), never a free string. `X-Request-Id` in header and body. Status map: `404` missing resource (never `500`), `400`/`422` bad input, `401` bad/absent token, `403` ownership/delegated identity, `409` state conflict, `429` quota, `503` provider down.
5. **Resources, not RPC** — plural nouns, hierarchy in the path, the verb is the HTTP method: `POST /evaluations/{id}/appeals`, not `POST /appealEvaluation`. Async op returns `202` + `Location` to `/jobs/{jobId}`.
6. **Pagination** — `page` from `0`; `size` `1..100` (out of range → `400`); explicit sort. The tenancy filter is always applied, not an optional param; a resource from another tenancy is absent and its detail gives `404`.
7. **Append-only academic data** — reference tables (golden set, entries, audit) reject destructive `UPDATE`/`DELETE` at the DB level; changing a rubric means a new version, not editing the current one.
8. **Versioning** — compatible changes (optional field, new endpoint) add to `v1`; incompatible → `v2`, `v1` never edited destructively. A cross-team field is frozen with its consumer before publishing, not added "just in case". Never expose the service without the Gateway; never modify another team's contract (Gateway, bus) — document what you need with owner and date.
