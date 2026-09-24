---
slug: building-the-practice-service-tutor-client-and-score-consumer
title: Building the practice-service tutor client and score consumer
description: 'How to build the practice-service side of the llm-service integration: token client, tutor client, ATTEMPT_CLOSED publisher and score consumer, tested first against the test bot.'
when_to_use: connecting practice-service to llm-service; integrating the AI tutor or the evaluator; testing against the fake bot; debugging a 403 or unavailable from the tutor; building the score consumer
stack: shared
type: skill
version: 1
tags:
- contracts
- llm-service
- practice-service
- integration
- tutor
- evaluator
---

<!-- SUBIDA AL HUB el 2026-09-20 con propose_skill (publicada v3 con la guía adjunta). -->
<!-- REVISIÓN v4 ENVIADA Y ACEPTADA el 2026-09-20: pasa los eventos al estándar Kafka del PDF KAFKA.pdf (ADR-020) y adjunta la guía v2 (52.121 bytes, sha256 25a18b11…). El texto vigente en el hub es el que muestra get_skill. -->

## Rule

Build the practice-service side of the llm-service integration from the published contracts, test it against the test bot first, and never decide the open points yourself: leave them configurable and report them.

1. **Read the contracts first:** [[llm-service-http-contract]] and [[llm-service-kafka-contract]], and follow [[backend-service-integration]] for the general contract-first workflow. Never invent fields, routes, topics or codes; ask when something is missing. The source of truth is the guide `llm-service/docs/contracts/equipos/llm-service-contrato-para-desafios-practicos.md` (in Spanish): when the YAML attached to the hub contracts differs, follow the guide.
2. **Build in this order.**
   - Configuration: gateway URL, client id, secret from an environment variable, topic names (provisional: the notifications group assigns them, groups cannot create topics), `KAFKA_BOOTSTRAP` (default `event-bus:29092`), group id = the service name.
   - Token client: `client_credentials` with `audience: llm-service` and scope `llm.tutor.interact`, cached until it expires.
   - Tutor client: `POST /api/llm/tutor/interactions` through the API Gateway with a new `Idempotency-Key` per message and a 30 s timeout. Handle `completed`, `unavailable` and `blocked`, and 401, 403, 409 and 422. Any model failure comes back as `200` with `state: unavailable`, and that response is stored under its key, so retry with a new key.
   - `ATTEMPT_CLOSED` publisher: five-field envelope (`eventId`, `eventType`, `timestamp`, `producer`, `payload`), no `eventVersion`; `timestamp` as ISO-8601 text (Spring's `JsonSerializer` writes an `Instant` as a number); the whole `transcript`, an outbox, and the same `eventId` when a send is retried.
   - Score consumer: its own group id; the topic mixes two payloads, so consume `Event<?>` (or the JSON body) and branch on `eventType` (`SCORE_CALCULATED`, `SCORE_DEFERRED`); with Spring's `JsonDeserializer` set `spring.json.use.type.headers=false` and a default type (llm-service sends no `__TypeId__` header); dedupe by `eventId`, keep the latest event per `attemptId`, and forward the score to the challenges engine (llm-service never grants XP).
3. **Test each case against the bot.** A normal message gives `200` and `completed`; the same key and body gives an identical response; the same key with another body gives `422`; a wrong scope gives `401`; no delegated identity gives `403`; an invalid `riskLevel` gives `422`; a valid `ATTEMPT_CLOSED` gives one `SCORE_CALCULATED` keyed by cohort; a repeated `eventId` gives a single score; an invalid event produces no score and is kept in llm-service's `event_dead_letter` table (there is no dead-letter topic). The bot never returns `unavailable`: ask the llm-service team to force it. Its text is a template, so check the shape, not the content.
4. **Never** call the service directly (except in local tests), send `X-*` headers, or log tokens, secrets or `expectedSolution`.
5. **Report at the end** what was built, which tests pass and against which environment, and which open points stayed configurable: topic names, `producer` and `timestamp` form, `expectedSolution`, the source of the IDE events and the "tutor unavailable" screen.

## Reasoning

The contract is the same for the test bot and the real model, so passing these tests against the bot is what protects the switch to the real one. Known risks to mention: a `403` with a valid token (the Gateway may not add the delegated user for a pure service token), the token endpoint route to confirm with users-service, and topic names that are still provisional until the notifications group assigns them. The guide assumes Java with Spring; translate it if the repo uses another stack.
