---
name: backend-service-boundary
description: >-
  Keep changes scoped to one 2026-P4-BE microservice, its owned data, and its existing
  architecture. Use before editing, creating, moving, or reviewing backend service code,
  configuration, migrations, or tests.
---

# Backend service boundary

Apply this skill in any official repository whose `origin` is an SSH or HTTPS GitHub URL
under `2026-P4-BE/`. A microservice owns its repository, runtime configuration, schema,
and published contracts; it does not own another service's source code, database, or
deployment configuration.

## Scope the change

Before editing, identify the requested capability and inspect the existing package
layout, test layout, `pom.xml`, `.compose/`, and repository workflow. Follow the local
structure rather than moving unrelated code into a new architecture.

Keep a service task inside its repository and its owned packages. Do not repair another
microservice, the API Gateway, or `tpi-system-compose` as incidental work. Report a
cross-service dependency with its owner and required contract instead.

## Protected and high-risk areas

Do not alter build files, CI workflows, Docker/compose configuration, quality rules,
dependencies, or production configuration as part of an ordinary feature unless the
task expressly requires it. Explain the exact file and impact before making such a
change.

Treat database migrations and externally visible contracts as ownership boundaries:

- never change another service's database or migration;
- do not alter an already-applied migration; add a new compatible migration when one is
  explicitly approved;
- do not invent tables, fields, endpoints, events, roles, or ownership to unblock work.

## Implementation invariants

The current service repositories use Java 21, Spring Boot, Maven, tests, Checkstyle,
and PMD. Preserve the project's existing language level and quality configuration.
Keep production code under `src/main` and tests under `src/test`; add focused tests next
to the behavior changed. Do not delete, disable, or weaken tests or quality rules to
make a gate pass.

Before PR, inspect the diff against `origin/develop...HEAD`. Separate unrelated build,
infrastructure, generated, or cross-service changes before review.
