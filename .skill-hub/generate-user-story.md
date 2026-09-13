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
version: 4
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

**Output language.** These instructions are written in English, but the fiche they produce is written **entirely in Spanish**. Read the wiki template page (`template-hist-usuario`) before writing the first fiche and copy every heading and every checklist label from it verbatim — never translate, reword or merge them. No English label may reach the produced document (`As / I want / So that`, `Given / When / Then`).

### Structure of each story

1. **Title** — `Sxx-Hyy — <title in plain words>`. Taiga title: `GXX — TITLE`.
2. **Metadata** — a **bullet list** (`- **Label:** value`), **never a table**: Taiga's reader renders a header-less markdown table as raw pipe text, one `| Label | value |` line after another. Six items: Work group (epic), Pair in charge, Depends on, Estimated work (hours), Type, Product owner.
3. **Description** — the template's three labelled bullets: a real role in plain words (never "the system"), the user action wanted (not a technical solution), the real benefit obtained.
4. **Notes** — the template's **seven** labelled checkboxes, one to one and in the template's own wording: business rules, validations, mandatory data, performance (times, volume, limits), security (roles, permissions, sensitive data), accessibility (WCAG / keyboard / screen readers), and the catch-all *other* label. Never drop, merge or reorder a label; one that does not apply is filled in with the template's "not applicable" wording.
5. **Acceptance criteria** — the template's criteria list: `CA1`, `CA2`, `CA3`, each a measurable and objective condition, plus the optional `Extras` checkbox. At least two of them state the case that must fail.
6. **BDD** — the template's scenario section under a bare `## BDD` heading. Minimum three scenarios: `1` expected path + at least `2` that must fail (a method rule, not spelled out in the heading). Each scenario is a numbered block carrying the template's three given / when / then bullets, plus an optional continuation bullet. Every expected outcome is observable (screen, message, stored data); each continuation bullet inherits the step before it.
7. **Prototype** — the template's **four** fields, never omitted: screenshots, design-file URL, component-library URL, mock API / Swagger. A field that does not apply is filled in with the template's "not applicable" wording.
8. **Estimate / Priority** — as bullets, not a table. Open with a **"quick format"** that makes both scales explicit: **Points (Fibonacci): [1/2/3/5/8/13]** and **Priority (MoSCoW / numeric): [Must/Should/Could/Won't] or [1..5]**. If Planning Poker has not happened, leave the brackets with the scale and mark the value pending — **never invent it**; points are assigned in Sprint 0 against the canonical story. MoSCoW in words: must = indispensable, should = desirable, could = can be postponed, won't = out of this sprint.
9. **Dependencies / Impacts** — the template's **five** items, never omitted: services involved, affected modules, other teams / approvals, data / migration impact, risks and mitigation (optional). This is the **last section**: the fiche closes here.
10. **Tasks** — the fiche has **no Tasks section** and no inline pointer to one. The SMART task breakdown is a separate document produced by [[generate-task]] (which takes the story with its acceptance criteria and BDD) and lives in `docs/tareas/`; Taiga carries the sub-tasks natively. The story ↔ tasks link lives in the folder READMEs and the pipeline, not in the fiche body.

### Classification (INVEST · V)

If the role in the description is a real one that perceives the result → **value story**, full template + points. If the statement is "as the team / as the platform we want <something technical>" and no user perceives the result → **internal task**: the same long format is allowed to trace the scenarios, but it is loaded in the backlog as a task, without value points.

### Cutting stories

One role, one action, one observable result. If the title needs "and / or / also", it is two stories. Split when there are two action verbs, two roles, one deliverable part without the other, or more than 6-7 scenarios.

### Document wrapper

The stories file opens with: what it is / what it is NOT (a "source of truth" table pointing IDs, hours and dependencies to the backlog), a short glossary of repeated domain terms, an "Before the fiches" section that situates the reader plus the sprint demo in one sentence, and an index. It closes with a "In summary" list. Stories may be grouped **by sprint** (`docs/historias/sXX.md`) or **by epic** (`docs/historias/ep-0X.md`); when grouped by epic, the sprint view (index + demo) lives in `docs/sprints/`. The file is a **presentation format**, not a planning source: if a datum disagrees with the backlog, the backlog wins.
