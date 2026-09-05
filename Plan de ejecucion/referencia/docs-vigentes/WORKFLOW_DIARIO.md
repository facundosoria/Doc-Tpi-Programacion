# Workflow diario de `llm-service`

Esta rutina aplica a las cinco parejas efectivas. Las dos sincronizaciones semanales duran 45 minutos; Planning, refinamientos, Review y Retro consumen la capacidad definida en el plan.

## Inicio

```bash
git fetch --all --prune
git status
git branch --show-current
```

En `develop`:

```bash
git checkout develop
git pull --ff-only origin develop
```

En una feature propia:

```bash
git fetch origin
git rebase origin/develop
```

No uses `git pull origin develop` dentro de una feature ni trabajes directamente en ramas protegidas.

## Tomar una tarea

1. Leer el sprint en el [backlog ejecutable](../Plan%20de%20ejecucion/07-backlog-ejecutable-sprints.md).
2. Comprobar requisitos previos, contrato, fuente normativa y dependencia externa.
3. Verificar DoR en el [playbook](../Plan%20de%20ejecucion/06-playbook-de-construccion.md).
4. Registrar responsable, horas, riesgo y dependencia en la plantilla del sprint.
5. Mover la tarea a `En progreso` y crear `feature/sNN/<id>-<slug>`.

## Implementar

- Seguir contrato → dominio/migración → aplicación → adaptadores → seguridad/resiliencia → observabilidad → pruebas → demo.
- Propagar `traceparent` y `X-Request-Id`; respetar `Idempotency-Key` y `eventId`.
- No guardar solución esperada en prompts, logs, respuestas ni fixtures.
- No inventar endpoints, eventos, campos, roles o datos de otro servicio.
- No editar migraciones aplicadas ni añadir fallback automático de modelo.
- Comunicar bloqueos el mismo día con dueño, contrato, fecha y alternativa.

```bash
git add -p
git commit -m "feat(tutor): aplicar guardia anti-fuga — LLM-S05"
git push -u origin feature/s05/llm-s05-tutor-anti-fuga
```

Usar Conventional Commits: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`, `ci`.

## Ceremonias

| Ceremonia | Salida |
|---|---|
| Planning, 120 min | Objetivo, tareas, capacidad, gates y demo. |
| Sincronización ×2/semana, 45 min | Avance, próximo paso y bloqueo. |
| Refinamiento ×2, 30 min | Historias futuras listas y dependencias. |
| Coordinación ×2, 30 min | Compromisos con servicios consumidores. |
| Review/demo, 90 min | Incremento funcional y evidencia. |
| Retro, 60 min | Mejora accionable con dueño. |

## Pre-PR

```bash
git diff origin/develop...HEAD
git diff --check
./mvnw test
docker compose config
```

Comprobar contrato, migraciones desde base vacía, autorización, ownership, idempotencia, fallos, secretos y demo reproducible. El PR indica sprint/historia, resultado, contratos, migraciones, pruebas y comando de demo.

## Cierre

Actualizar tablero, horas y bloqueos; integrar features terminadas a `develop`; crear release solo si la demo y DoD pasan. El trabajo incompleto vuelve al backlog y se reestima.

## Anexo APB — explicaciones para quien recién empieza

Esta sección explica qué hace cada comando y qué decisión tomar. Si un comando devuelve un error, no lo repitas a ciegas: conserva la salida, informa el bloqueo y consulta al referente de la pareja.

| Comando | Qué hace | Cuándo usarlo |
|---|---|---|
| `git status` | Muestra archivos modificados, preparados y sin seguimiento. | Antes y después de cada tarea o cambio de rama. |
| `git fetch --all --prune` | Actualiza referencias remotas; no cambia tus archivos. | Al comenzar el día y antes de actualizar una feature. |
| `git pull --ff-only origin develop` | Actualiza `develop` sin crear merge automático. | Solo estando en `develop`. |
| `git rebase origin/develop` | Coloca tus commits encima del último `develop`. | En tu feature, antes de PR o cuando quedó atrasada. |
| `git diff origin/develop...HEAD` | Muestra exactamente lo que tu PR agrega. | Self-review antes de abrir PR. |
| `git diff --check` | Detecta espacios y errores de formato en el diff. | Siempre antes de PR. |
| `git add -p` | Permite elegir partes relacionadas para el próximo commit. | Para mantener commits pequeños. |
| `git log --oneline --decorate -10` | Resume los últimos commits y ramas. | Cuando no sabés dónde estás parado. |

### Crear una feature, explicado

La feature es una copia de trabajo aislada. Los cambios no afectan `develop` hasta que un PR sea revisado y mergeado:

`git checkout develop` cambia a la rama de integración; `git pull --ff-only origin develop` trae su versión actual; `git checkout -b feature/s05/llm-s05-tutor-anti-fuga` crea tu rama propia.

### Qué hacer ante conflictos de rebase

1. `git status` indica los archivos en conflicto.
2. Abrí cada archivo y conservá la combinación correcta, respetando contrato y cambios recientes.
3. Ejecutá las pruebas relevantes.
4. Marcá cada archivo resuelto con `git add <archivo>`.
5. Continuá con `git rebase --continue`.
6. Si no podés resolverlo con seguridad, usá `git rebase --abort`: vuelve al estado anterior del rebase y no borra tus commits.

### Conceptos mínimos

- **Working tree:** archivos que editaste pero todavía no están en un commit.
- **Commit:** cambio lógico guardado localmente.
- **Push:** publica tus commits en tu rama remota; no integra nada por sí solo.
- **Pull Request:** propuesta revisable para integrar una feature a `develop`.
- **Merge:** incorporación aprobada de una rama a otra.
- **Rebase:** actualización de la base de tu rama; no reemplaza la revisión.
- **DoR:** condiciones para poder comenzar una historia.
- **DoD:** condiciones para declarar terminada una historia.

Nunca uses `git reset --hard`, `git clean -fd` o `git push --force` para “arreglar” un problema sin autorización: pueden eliminar trabajo o evidencias.
