---
name: frontend-feature-boundary
description: >-
  Keep Angular frontend work within its assigned feature slice and away from protected
  project configuration in 2026-PIV-TPI-FE. Use before editing, creating, moving, or
  reviewing frontend feature files.
---

# Frontend feature boundary

Apply this skill only in the official frontend repository, identified by either
`git@github.com:2026-P4-FE/2026-PIV-TPI-FE.git` or
`https://github.com/2026-P4-FE/2026-PIV-TPI-FE(.git)`, and by the presence of
`src/app/features/`.

## Establish the allowed slice

Before editing, identify the feature root named by the task under
`src/app/features/<feature>/`. The branch theme is not enough to infer that directory:
the repository uses semantic names such as `ai-tutor`, `courses`, and `marketplace`.

If the task does not state the feature root, inspect the requested files or ask for the
assigned root before making changes. Do not guess a mapping from `tema-XX` to a folder.

For a normal team task, all application changes must stay under that one feature root,
including its `pages/`, `ui/`, `data-access/`, and routes file. A feature page owns
orchestration; presentational components belong in `ui/`; HTTP services, models, and
SignalStore state belong in `data-access/`.

## Protected boundaries

Do not modify these paths for an ordinary feature task:

- `package.json`, `package-lock.json`, `angular.json`, `tsconfig*.json`;
- lint, formatter, editor, Git, or npm configuration;
- `.github/**` and `.husky/**`;
- global application configuration or another team's feature root.

The CI guard protects these paths for non-administrators. If the request genuinely
requires one, state the exact file and reason, then wait for explicit administrator
authorization; do not fold it into the feature change.

`proxy.conf.json` is an exception only for a local-service task handled under
`frontend-local-service-integration`; change only the route entries that belong to the
assigned theme.

## Frontend invariants

- Use standalone Angular components, native control flow (`@if`, `@for`, `@switch`),
  signals, `inject()`, and `ChangeDetectionStrategy.OnPush` for visual components.
- Use English for source identifiers, comments, types, filenames, and JSDoc. User-facing
  text may be Spanish when the product requires it.
- Do not add `NgModule`, legacy structural directives, legacy input/output decorators,
  constructor injection, `BehaviorSubject` for local UI state, `zone.js`, hardcoded API
  URLs, `console.log`, or auth tokens in `localStorage`.

Before PR, inspect `git diff --name-only origin/develop...HEAD`. If it shows an
unassigned feature, a protected path, or a global change, stop and separate the work.
