---
slug: request-correlation-across-http-and-kafka
title: Request correlation across HTTP and Kafka
description: Every request and event carries traceparent and X-Request-Id; response echoes the same X-Request-Id;
  JSON bodies never carry a trace id field.
when_to_use: propagating a trace id, correlating logs across services, adding headers to an outbound call,
  publishing or consuming a Kafka event, debugging a cross-service request
stack: shared
type: convention
owning_team: LLM
version: 1
tags:
- http
- kafka
- observability
- tracing
---

## Rule

Every synchronous request and every Kafka event carries two correlation headers: `traceparent` (W3C trace context) and `X-Request-Id`. A service that receives a request propagates both to every outbound call and to every event it publishes. The response echoes back the same `X-Request-Id` it received. JSON payloads never carry a `trace_id` / `traceId` field — correlation lives in headers only, including Kafka message headers. Logs are structured and include `X-Request-Id` as a field so a single request can be followed across all services.
