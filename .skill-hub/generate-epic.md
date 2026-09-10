---
slug: generate-epic
title: Generate epic
description: 'Structure of a product epic fiche: Objective, Assumptions and Constraints, epic-level acceptance
  criteria, Dependencies. Epics are not estimated and not committed to a sprint.'
when_to_use: writing a product epic, building the EP-01..EP-NN catalogue, epic-level acceptance criteria,
  deciding when an epic is closed, turning product scope into epics
stack: shared
type: convention
owning_team: LLM
version: 1
tags:
- backlog
- epics
---

## Rule

Write every epic with the official Taiga Epic template. An epic is a large capability that does **not** fit in one sprint; it groups stories by *what they are for in the product*. Epics are **not estimated** in points and **not committed** to a sprint: an epic closes when **all its stories** meet the Definition of Done.

### Header block

Open with a quote block holding a `| Field | Value |` table: Lead pair/team, Sprints it contributes to, Requirements (orientative, `RF-*`/`PAR-*`), Initial stories, "Closes when". Then `# [GXX] — EP-0X: <NAME>`.

### Body sections

1. **Objective** — 1-2 lines: what business/user value the epic delivers, in observable terms. Never a technical solution.
2. **Assumptions and Constraints** — Assumptions = what is taken as true for the epic to make sense. Constraints = legal / technical / academic limits on *how* it may be solved.
3. **Epic-level Acceptance Criteria** — the observable close of the **whole set**: the minimum stories enable a named end-to-end flow; initial KPIs (or `to be defined`); no critical regressions in the areas touched; observability where applicable; usage and operations docs published.
4. **Dependencies / Impacts** — services/APIs, affected modules, other teams, data/migration impact, feature flags.

### What NOT to do

No `As / I want / So that`, no BDD scenarios, no story points, no "committed in Sxx" inside the fiche. Do not invent KPIs, requirements or dependencies. An epic is never renumbered; a dropped number is not reused.

### Catalogue

One file per epic (`epicas/ep-01.md`, ...). Also a `README.md` catalogue table: Epic, Fiche, Name, Owner, Sprints, Requirements. The catalogue is the source of truth; each fiche subordinates to it.
