---
name: project-quality-gate
description: >-
  Run and interpret the required local quality gate before a push, pull request, or
  completion claim in 2026-PIV-TPI-FE or any repository in 2026-P4-BE. Routes checks
  from the repository's actual build and CI configuration without assuming a stack.
---

# Project quality gate

Use this skill before a push, PR, merge request, or statement that a project change is
ready. It applies to the official frontend repository and every repository in
`2026-P4-BE`; select commands from the verified repository root.

## Common preflight

1. Confirm the repository with `origin`: the official frontend URL (SSH or HTTPS), or
   an SSH/HTTPS GitHub URL under `2026-P4-BE/`.
2. Inspect `git status --short` and `git diff --check`.
3. Review the diff against `origin/develop...HEAD` for a normal feature/fix PR, or the
   appropriate target branch for an administrator-authorized release/hotfix.
4. Do not claim success if any command fails, is skipped, or is not available. Preserve
   the output and report the failing check.

## Frontend: `2026-PIV-TPI-FE`

Run:

```bash
npm run verify
```

This invokes Angular linting, Stylelint, Prettier checking, and unit tests. Do not run
`ng build` locally just to satisfy this skill: the repository's `AGENTS.md` reserves the
production build for CI. Do not bypass the existing pre-commit or pre-push hooks.

For UI changes, also apply `frontend-ui-kit-compliance`; for local service flows, test
the relevant success and failure path, with `npm run mock:auth` when a session is used.

## Backend microservices: `2026-P4-BE`

Run:

```bash
mvn -B verify --file pom.xml
```

All currently cloned Spring Boot microservices in the organization use this Maven
quality gate for tests, Checkstyle, and PMD. If a future backend repository does not
have `pom.xml`, inspect its workflow, hooks, and documented task runner, then execute
its stated gate instead. If no quality gate exists, report that absence and do not claim
the change is ready. If a check needs a running dependency, report that prerequisite
rather than weakening or skipping it.

## Completion and PR evidence

Record the commands run and their outcome in the PR template. Include functional test
steps, risk/mitigation, and screenshots or recording for visual changes. Never change
hooks, CI workflows, linters, tests, or protected configuration merely to turn a failed
gate green. Fix only the requested, in-scope code or report the blocker.
