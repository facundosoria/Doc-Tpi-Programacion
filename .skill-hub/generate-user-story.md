---
slug: generate-user-story
title: Generate user story
description: 'How this org writes user stories: Taiga template structure, plain language by default, one
  happy path plus two failing scenarios, points estimated in Sprint 0.'
when_to_use: writing user stories, sprint backlog fiches, historias de usuario (HU), turning a sprint
  recipe into stories, BDD acceptance scenarios, splitting an epic into stories
stack: shared
type: convention
owning_team: LLM
version: 1
tags:
- agile
- bdd
- documentation
- invest
- planning
- taiga
- user-stories
---

## Rule

Write every user story with the **official Taiga HU template structure**, in **plain language by default** (as if explaining it to someone who never programmed). A jargon-heavy variant is produced only on request.

### Structure of each story

1. **Title** — `Sxx-Hyy — <title in plain words>`. Taiga title: `GXX — TITLE`.
2. **Metadata table** (6 rows): Work group (epic), Pair in charge, Depends on, Estimated work (hours), Type, Product owner.
3. **Description** — As / I want / So that. `As` = a real role in plain words (never "the system"); `I want` = a user action, not a technical solution; `So that` = a real benefit.
4. **Notes** — labelled *Business rules*, *How it is checked*, *What it must include*, *Timing/volume*, *Security*, *Accessibility*. Says what must hold, never how to code it.
5. **Acceptance criteria** — concrete and verifiable; at least **two** labelled "(case that must fail)".
6. **BDD** — header "What is tested:"; **1 expected path + at least 2 that must fail**. Each scenario: Title, Given, When, Then (+ optional And). Every `Then` is observable (screen, message, stored data). Each `And` inherits the step before it.
7. **Prototype** — low-fidelity sketch of the main screens, or "not applicable" + reason.
8. **Estimate / Priority** — as bullets, not a table. Story points are **assigned in Sprint 0 with Planning Poker against the canonical story** — never invented here. Priority: must / should / could.
9. **Dependencies / Impacts** — parts involved, other teams, data impact, risks.
10. **Tasks (technical steps)** — table `# | Task | h`, no As/I-want/So-that, each ≤ 1 working day, summing the estimated work.

### Classification (INVEST · V)

If `As` is a real role that perceives the result → **value story**, full template + points. If the statement is "As the team / As the platform, I want <something technical>" and no user perceives the result → **internal task**: same long format is allowed to trace the scenarios, but it is loaded in the backlog as a task, without value points.

### Cutting stories

One role, one action, one observable result. If the title needs "and / or / also", it is two stories. Split when there are two action verbs, two roles, one deliverable part without the other, or more than 6-7 scenarios.

### Document wrapper

The stories file opens with: what it is / what it is NOT (a "source of truth" table pointing IDs, hours and dependencies to the backlog), a short glossary of repeated domain terms, an "Before the fiches" section that situates the reader plus the sprint demo in one sentence, and an index. It closes with a "In summary" list. The file is a **presentation format**, not a planning source: if a datum disagrees with the backlog, the backlog wins.
