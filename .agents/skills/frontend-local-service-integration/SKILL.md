---
name: frontend-local-service-integration
description: >-
  Connect a 2026-PIV-TPI-FE feature to its assigned local microservice through the
  existing proxy and HttpGenericService. Use for /api/v1 endpoints, proxy targets,
  local authentication, or frontend data-access integration.
---

# Frontend local service integration

Apply this skill only in `2026-PIV-TPI-FE` after establishing the assigned feature
root with `frontend-feature-boundary`.

## HTTP boundaries

Inspect the existing feature slice and `src/app/core/http/http-generic.service.ts`
before adding data access. Use `environment.apiUrl` through `HttpGenericService`; do
not hardcode hosts, ports, IP addresses, or absolute URLs in services or components.

Use only endpoints that exist in the agreed contract. Requests remain under `/api/v1/**`.
Use `httpResource` or `HttpGenericService.resource()` for reactive queries and the
generic service's mutation methods for POST, PUT, PATCH, and DELETE. Keep HTTP code,
models, and state in the assigned feature's `data-access/` slice.

Do not invent endpoints, response fields, roles, or persistent mock data. If the needed
contract is absent, stop and report the required endpoint or field.

## Proxy rules

Read `proxy.conf.json` before changing it. It already maps `/api/v1` paths by theme and
has a Gateway fallback. For local work, adjust only the existing entries belonging to
the assigned theme to the local microservice port. Preserve the path key, `secure`,
`changeOrigin`, and fallback entries unless an administrator explicitly changes the
platform policy.

Do not solve local connectivity by adding CORS configuration, bypassing the proxy, or
requiring the full API Gateway to run.

## Authentication and evidence

For a session-dependent feature, start and test with `npm run mock:auth`. Never put
tokens or roles in `localStorage`; authentication uses the established cookie/header
mechanism.

Report the endpoint path, local target, test account or mock scenario when applicable,
and the observable success and failure behavior. Restore no shared proxy target unless
the user requests it: local proxy edits are normal working-tree changes and must be
reviewed before PR.
