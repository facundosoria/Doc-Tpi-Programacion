# Gitflow de `llm-service`

Este repositorio desarrolla `llm-service` en tres fases y 19 sprints de dos semanas. Se trabaja con un backlog y cinco parejas efectivas; las ramas representan integración, no equipos separados.

## Ramas

| Rama | Origen | Destino | Uso |
|---|---|---|---|
| `main` | inicial | — | Código estable y releases demostrables. Protegida. |
| `develop` | `main` una vez | `release/*` | Integración continua del sprint. Protegida. |
| `feature/<sprint>/<id>-<slug>` | `develop` | `develop` | Una historia o paquete pequeño. |
| `release/vX.Y` | `develop` | `main` y `develop` | Cierre de sprint/fase; solo fixes y documentación. |
| `hotfix/<slug>` | `main` | `main` y `develop` | Incidente crítico de una versión publicada. |

Ejemplos:

```text
feature/s01/llm-s01-h03-golden-set-api
feature/s05/llm-s05-tutor-anti-fuga
feature/s06/llm-s06-outbox-score
feature/s14/llm-s14-ingesta-pdf
hotfix/s06-dedupe-score
release/v0.1-f1-mvp-academico
```

No usar nombres de otro proyecto ni ramas por pareja; toda rama debe identificar el sprint y la historia o tarea técnica.

## Flujo

```text
main ← release/vX.Y ← develop ← feature/sNN/<id>-<slug>
  ↑                         ↑
  └──── hotfix/<slug> ──────┘
```

1. Tech Lead crea y protege `develop` desde `main`.
2. Planning asigna sprint, historia, pareja, dependencia y aceptación.
3. La feature nace de `develop`.
4. Se implementa en orden: contrato, dominio/migración, caso de uso, adaptadores, seguridad/resiliencia, observabilidad, pruebas y evidencia.
5. PR a `develop`; nunca push directo.
6. Si la Review y DoD pasan, se crea `release/vX.Y`, se prueba, se mergea a `main`, se etiqueta y se sincroniza a `develop`.
7. Un hotfix nace de `main`, se prueba, mergea a ambas ramas y aumenta patch.

## Reglas de merge

- CI verde, `git diff --check`, pruebas, Docker/demo y evidencia.
- Un reviewer de la pareja y el consumidor afectado si cambia contrato, evento o datos.
- Cambios incompatibles OpenAPI/AsyncAPI requieren adenda/versionado aprobado.
- Una migración Flyway aplicada no se edita: se agrega otra.
- Prohibidos secretos, `.env`, prompts en logs, datos académicos, push directo y force-push en ramas protegidas.
- Features se integran con squash y se eliminan después.

## Versionado y fases

| Entrega | Tag sugerido |
|---|---|
| S1–S10 · F1 académico | `v0.1` |
| S11–S13 · F2 moderación | `v0.2` |
| S14–S19 · F3 RAG/personalización | `v1.0` |
| Corrección urgente | `vX.Y.Z` |

El detalle operativo está en [Plan de ejecución/05-flujo-diario-y-git.md](../Plan%20de%20ejecucion/05-flujo-diario-y-git.md), el [playbook](../Plan%20de%20ejecucion/06-playbook-de-construccion.md) y el [backlog](../Plan%20de%20ejecucion/07-backlog-ejecutable-sprints.md).
