---
slug: writing-skills
title: Writing skills for the catalogue
description: Field-by-field guide to propose_skill and the wording rules every catalogue entry follows.
when_to_use: 'Use when writing or proposing a catalogue skill: what each propose_skill field means and
  the house rules for wording title, description, when_to_use, content and rationale.'
stack: shared
type: reference
owning_team: platform
version: 1
tags:
- conventions
- meta
- propose
- writing
---

## Rule

A catalogue entry is only useful if another agent can find it and follow it without guessing. Write every field for that reader.

## The fields of propose_skill

- **title** (required) - a short noun phrase naming the topic, 3-120 chars. Not a sentence.
- **description** (required) - one sentence, 10-200 chars. What the entry covers, in plain terms. Do not just repeat the title.
- **when_to_use** (required) - 10-200 chars, written FOR A MACHINE, not a person. Name the situations and the words someone would use to describe the task, e.g. "Use when doing X; also when Y or Z." This string is what decides whether search returns your entry, so spend the most effort here.
- **stack** (required) - one of `angular`, `java`, `shared`, `infra`. Use `shared` for anything not tied to one runtime.
- **type** - one of `skill` (how something is done), `convention` (a rule to respect), `reference` (data). Defaults to `convention`.
- **content** (required) - Markdown, min 40 chars, starting with `## Rule`. State the rule first, then the reasoning and any examples. Keep it short: an agent reads this every time.
- **from_query** (required) - the exact search that returned nothing and led you here. Used to track catalogue gaps.
- **rationale** (required, min 10 chars) - what you based the rule on: the surrounding codebase, a related entry, or general practice because there was nothing to infer from. Be honest; an admin reads this to decide how much to trust the entry.
- **tags** - up to 12 short strings for extra matching.
- **slug** - derived from the title if omitted. Only set it to override.

## Wording rules

- Write in English. The search index stems English and agents query in English; an entry in another language is effectively invisible.
- One rule per entry. If it needs two rules that apply in different situations, it is probably two entries.
- Prefer the imperative: "Always ...", "Never ...", "Name it ...".
- Only propose when search_skills returned nothing. If something similar exists, the call is refused and returns the existing entry - follow that one.
