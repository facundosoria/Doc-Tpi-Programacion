# Flujo diario y Git de `llm-service`

Esta es la guía vigente del proyecto `llm-service`. Define el flujo Git y la rutina diaria para las tres fases, 19 sprints y cinco parejas efectivas. La estrategia detallada también está disponible en [GITFLOW](../docs/GITFLOW.md) y [workflow diario](../docs/WORKFLOW_DIARIO.md).

Si nunca trabajaste con Git, empezá por el [anexo APB](../docs/WORKFLOW_DIARIO.md#anexo-apb--explicaciones-para-quien-recién-empieza), que explica cada comando en lenguaje simple.

## 1. Ramas del proyecto

El detalle de cada regla está en [GITFLOW](../docs/GITFLOW.md). Esta sección resume el uso durante un sprint.

| Rama | Uso |
|---|---|
| `main` | Versión estable y demostrable. No recibe push directo. |
| `develop` | Integración continua. Debe compilar y pasar pruebas. |
| `feature/<sprint>/<historia>-<descripcion>` | Trabajo acotado de una historia. Nace de `develop`. |
| `release/vX.Y` | Preparación de demo/release del sprint. Solo correcciones de cierre. |
| `hotfix/<descripcion>` | Corrección urgente sobre `main`; vuelve también a `develop`. |

Ejemplos:

```text
feature/s01/llm-s01-h03-golden-set-api
feature/s05/llm-s05-h02-tutor-anti-fuga
feature/s14/llm-s14-h01-ingesta-pdf
hotfix/idempotencia-evaluacion
```

La convención vale desde que el repositorio remoto configure `develop`. Si aún no existe, el Tech Lead crea y protege esa rama antes de abrir features. No crearla cada integrante por separado.

## 2. Inicio del día

1. Abrí la plantilla del sprint y confirmá la historia asignada, sus dependencias y criterio de aceptación.
2. Mirá el tablero y comentarios de Pull Requests.
3. Confirmá tu rama y estado de trabajo:

```bash
git status
git branch --show-current
git fetch --all --prune
```

4. Si vas a comenzar una historia, actualizá `develop` y creá rama:

```bash
git checkout develop
git pull --ff-only origin develop
git checkout -b feature/s01/llm-s01-h03-golden-set-api
```

5. Si ya tenés una feature, actualizala antes de cambios grandes:

```bash
git fetch origin
git rebase origin/develop
```

Si el rebase tiene conflictos, resolvé solo los archivos que entiendas, ejecutá pruebas y continuá:

```bash
git add <archivo-resuelto>
git rebase --continue
```

Para abandonar el rebase sin perder la versión previa de la rama:

```bash
git rebase --abort
```

## 3. Durante el trabajo

- Leé contrato y requisito antes de crear endpoint/evento.
- Implementá con pruebas desde el inicio; no dejes integración para el último día.
- Hacé commits pequeños con Conventional Commits.
- Revisá tu diff antes de cada commit.
- No cambies una migración Flyway ya ejecutada: creá otra migración.
- No subas claves de proveedores, tokens, bases locales, `target/` o archivos `.env`.

Ejemplo:

```bash
git add -p
git commit -m "feat(golden-set): add versioned entry creation"
git push -u origin feature/s01/llm-s01-h03-golden-set-api
```

En las dos sincronizaciones semanales respondé: avance hacia el objetivo, próximo paso y bloqueo. Si depende de otro equipo, registrá contraparte, contrato, fecha y evidencia en la plantilla; no esperes al cierre del sprint.

## 4. Antes de abrir un Pull Request

1. Ejecutá los tests adecuados y verificá contrato si cambiaste una interfaz.
2. Confirmá que no agregaste secretos ni archivos generados.
3. Leé el diff completo.
4. Asegurá que la historia conserva los incrementos anteriores y puede demostrarse.
5. Pedí revisión al par y al consumidor si modificaste contrato, evento o datos compartidos.

Comandos útiles:

```bash
git diff origin/develop...HEAD
git diff --check
./mvnw test
```

Título sugerido:

```text
feat(golden-set): create versioned entries — LLM-S01-H03
```

Descripción mínima de PR:

```markdown
## Resultado
Qué puede hacer ahora el usuario.

## Historia
LLM-S01-H03

## Contratos e integración
OpenAPI / AsyncAPI / migración / servicio consumidor afectados.

## Pruebas
Comandos ejecutados y escenarios comprobados.
```

No hacer push directo a `main` o `develop`. Una feature se integra por PR aprobado y checks verdes. Las ramas release/hotfix también requieren revisión proporcional a su urgencia.

## 5. Cierre del día y del sprint

Al terminar el día, dejá trabajo subido o explícitamente guardado, actualizá estado de historia y registrá bloqueos. No uses un commit WIP en `develop` o `main`.

Al cierre del sprint:

1. Integrar historias terminadas en `develop` mediante PR.
2. Crear `release/vX.Y` desde `develop` solo si el entregable cumple Definition of Done.
3. Ejecutar smoke tests y la demo de Review.
4. Integrar release a `main`, etiquetar versión y sincronizar correcciones de vuelta a `develop`.
5. Completar Review, Retro y la plantilla de sprint; el trabajo pendiente se reestima, no se traslada gratis.

Antes de usar `git push --force-with-lease`, asegurate de que la rama es exclusivamente tuya y avisá a reviewers si existe un PR. Nunca uses `git push --force`.
