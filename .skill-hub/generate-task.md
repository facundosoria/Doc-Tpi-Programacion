---
slug: generate-task
title: Generate task
description: 'How this org breaks a user story into technical tasks: Taiga Task template, SMART wording,
  one step of one day or less, each task traced to an acceptance criterion or BDD scenario.'
when_to_use: breaking a user story into technical tasks, SMART tasks, desglosar una historia en tareas,
  tareas del sprint, Taiga task fiche, task-level definition of done, tracing tasks to acceptance criteria
stack: shared
type: convention
owning_team: LLM
version: 1
tags:
- agile
- definition-of-done
- planning
- smart
- taiga
- tasks
- traceability
---

## Rule

Break each user story into the **technical tasks** that build it, one **Taiga Task fiche** per task, worded with **SMART** (Specific, Measurable, Achievable, Relevant, Time-boxed). A task is internal team work: no `As / I want / So that`, no BDD, no story points, no MoSCoW, no INVEST — that all belongs to the story. In Taiga the task hangs off its user story (or off the sprint enabler task).

### Inputs required

The story with its **acceptance criteria and BDD scenarios** (without them a task cannot be traced or closed); the task ID prefix (`LLM-S01-H01-T01`); the story's hours reference from the plan / sprint recipe. Optional: the team's build order, the HTTP/event contracts the story touches, task dependencies. Never invent contracts, endpoints or hours — mark the unknown `(to confirm)`.

### Method

1. Re-read the story; underline in each AC and BDD scenario every distinct thing to build or test — each is a candidate task.
2. Group candidates by build step. Default order: contract & threat → domain & migration → use case → adapters → security & resilience → observability → E2E test → demo & evidence. Merge same-step candidates; split those mixing two steps.
3. One task = one step of **one effective day or less**. Split what does not fit; merge two sub-half-day candidates that share context.
4. Write each task with the Taiga Task template: **Title** (verb + concrete result), the five SMART letters made explicit, **Steps / scope** (2-5 sub-steps + what is out), **Done criterion** (observable, answered yes/no), **Estimate & dependencies** (hours, build-order step, `Depends on`, `Traces` to AC## and/or BDD scenario).
5. The task hours **sum** the story's reference. If they do not, revisit the cut — do not hand-tune numbers without explaining it.
6. Every AC and every BDD scenario is covered by at least one task, negatives included (usually a validation / security / idempotency task).

### Document

One file per grouping: `docs/tareas/ep-01.md` when grouped by epic, `docs/tareas/sXX.md` by sprint. It opens with a "what it is / method / source that wins (hours → plan; AC/BDD → the story)" block and, if it groups several stories, a `Story · Tasks · h` index. Each story is `## Sxx-Hyy — Title` with a pointer to its fiche; its tasks follow as `### T## — Title`. The document is a **presentation format**, not a planning source: if an hour disagrees with the plan, the plan wins.

### Pipeline

[[generate-epic]] → [[generate-user-story]] → **generate-task**. The story is produced by [[generate-user-story]] and closes with `> **Tasks:** [link] — N tasks, N h`; this skill fills that link.
