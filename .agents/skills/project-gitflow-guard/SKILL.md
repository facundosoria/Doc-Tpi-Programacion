---
name: project-gitflow-guard
description: >-
  Enforce the project-wide GitFlow before creating branches, committing, pushing,
  merging, or opening pull requests in 2026-PIV-TPI-FE or any microservice in the
  2026-P4-BE organization. Use for Git operations regardless of component or stack.
---

# Project GitFlow guard

This skill governs Git operations for the whole TPI project. It is deliberately not a
frontend skill: apply it in `2026-PIV-TPI-FE` and every microservice repository owned
by the `2026-P4-BE` organization, independent of its language or framework.

## Repository preflight

Before a Git mutation, locate the repository root and inspect `origin`, the current
branch, `git status --short`, and the relevant remote branches. This skill applies when
`origin` identifies one of these official repositories:

- `origin` is `git@github.com:2026-P4-FE/2026-PIV-TPI-FE.git` and the root contains
  `package.json` plus `src/app/features/`; or
- `origin` is an SSH or HTTPS GitHub URL under `2026-P4-BE/`, with a non-empty
  repository name. Examples include `git@github.com:2026-P4-BE/tpi-llm.git` and
  `https://github.com/2026-P4-BE/<microservicio>.git`.

For a backend repository, inspect its own workflow files, hooks, and contributor guide
for additional checks. Those repository-specific controls supplement this common
GitFlow; they do not replace it.

If the checkout cannot be identified, do not apply this project's branch rules to it.
If the user requested a protected operation, report the unidentified context and stop
until the target repository is explicit.

Never discard work to make the preflight pass: do not use `git reset --hard`,
`git clean -fd`, or `git push --force`.

## Branches and destinations

The daily integration branch is `develop`. Start work from an up-to-date local
`develop` (`git fetch origin`, then `git pull --ff-only origin develop`).

Use these project-wide branch forms:

| Work | Origin | Branch | PR target |
|---|---|---|---|
| New theme work | `develop` | `feature/tema-XX-descripcion-corta` | `develop` |
| Theme defect | `develop` | `fix/tema-XX-descripcion-corta` | `develop` |
| Shared platform work | `develop` | `feature/plataforma-descripcion-corta` | `develop` |
| Shared platform defect | `develop` | `fix/plataforma-descripcion-corta` | `develop` |
| Published-version preparation | `develop` | `release/<version>` | `main` |
| Critical published defect | `main` | `hotfix/<descripcion>` | `main` |

`XX` is two digits from `01` through `12`; the description is lowercase kebab-case.
Use the `plataforma` form only for shared infrastructure such as the API Gateway or
system compose, not to avoid naming the owning theme. Examples:
`feature/tema-07-tutor-streaming`, `fix/tema-07-score-deduplication`, and
`feature/plataforma-gateway-auth-filter`.

`release/*` and `hotfix/*` are administrator-owned. Do not create them, open their PR,
or merge them unless the user explicitly delegates that release operation. A completed
release or hotfix must be synchronized back into `develop`; the frontend release
workflow may do this through its generated `backport/*` branch. Tags are created only
from the published `main` result, never from a feature or `develop`.

Do not push directly to `main` or `develop`. Do not locally merge a feature into a
protected branch. Open a PR instead. A normal feature/fix PR always targets `develop`.

## Commits

Before committing, inspect the staged diff. Keep one logical change per commit and do
not stage generated files, secrets, local environment files, or unrelated changes.

Require an English Conventional Commit subject in this form:

```text
<type>(<optional-scope>): <imperative description>
```

Allowed types are `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`, `perf`, and
`style`. Examples:

```text
feat(tema-07): add tutor feedback panel
fix(tema-07): prevent duplicated score event
```

Reject vague subjects such as `changes`, `update`, `final`, or `fix stuff`. Never add
AI attribution or `Co-Authored-By` trailers unless the user expressly asks for them.

## Push, PR, and merge preflight

Before a push, re-check the branch form, the remote, the working tree, and the commits
that will be published. Discover the repository's documented quality gate from its
contributor guide, workflows, hooks, task runner, or package manifest, then run it
before declaring the branch ready. Do not assume a language, build tool, test command,
or coverage rule from another component. If the repository does not document a quality
gate, report that absence and do not claim the branch is ready for review.

Before opening a PR, inspect the repository's PR template and complete it truthfully.
Use a draft PR until checks are green. Include the theme, intent, test evidence, risks,
and visual evidence when the change affects UI.

Before merging, confirm that the PR target matches the table, CI is green, required
review is present, and no protected-file guard failed. Do not bypass branch protections,
hooks, CI, CODEOWNERS, or reviews.

## Existing enforcement

The repositories already implement branch checks, hooks, CI, and protected-file guards.
They are the technical authority. This skill is the agent's early decision gate: it
must not edit, disable, or work around those controls.
