---
name: backend-service-integration
description: >-
  Integrate a 2026-P4-BE microservice with other platform services without bypassing
  ownership or contracts. Use when adding or changing HTTP APIs, API Gateway routes,
  events, service-to-service dependencies, OpenAPI, or AsyncAPI.
---

# Backend service integration

Apply this skill in a repository under `2026-P4-BE` when a change crosses a service
boundary. First identify the owner of the API, event, identity data, database, Gateway,
or deployment concern being touched.

## Choose the correct integration channel

- For synchronous HTTP contracts, load and follow `contratos-api-gateway` before editing
  OpenAPI, endpoint behavior, an inter-team contract, or a contract addendum.
- For asynchronous events, load and follow `contratos-kafka` before defining, producing,
  consuming, or changing an event or AsyncAPI.
- Do not use direct database access to another service, a hardcoded local URL, or an
  undocumented endpoint as an integration shortcut.

The API Gateway, event bus, and system compose are platform-owned. A service team may
document what it needs from them, but does not change their contracts or configuration
without the responsible team's explicit authorization.

## Contract-first workflow

1. Read the local contract and the consumer/producer agreement before implementation.
2. Confirm that each path, request field, response field, event field, role, and error
   is implemented or agreed; freeze missing information with the owning team instead of
   guessing.
3. Preserve backward compatibility. A breaking HTTP or event change requires approved
   versioning and coordinated consumer migration.
4. Test successful, validation, authorization, and dependency-failure behavior. Use a
   documented mock only as evidence for the contract; do not claim an integration demo
   passed when it requires a live dependency.

## Traceability and safe operation

Propagate the platform's required correlation and identity context as defined by the
relevant contract skill. Do not log secrets, tokens, sensitive academic data, prompts,
or expected answers. Record each external dependency, owner, pending decision, and test
evidence in the PR.
