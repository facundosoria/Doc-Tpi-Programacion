# Auditoría con 4 agentes en paralelo — remanentes de `docsV2`/`docsV3`/históricos — 2026-09-22

## Decisión anterior

Después de varias rondas de limpieza en `docs/` (renombre `docsV3`→`docs`, `demo`→`lab`, retiro de
contratos históricos, reubicación de `Plan_skills_calibracion.md`), no había verificación
independiente de que no quedaran remanentes fuera de lo ya revisado manualmente.

## Motivo

Se lanzaron 4 agentes en paralelo, cada uno con una porción de `llm-service/` de tamaño similar
(por cantidad de archivos), para buscar enlaces rotos y menciones de texto plano a `docsV2`,
`docsV3`, la carpeta `demo/` vieja, y los archivos de contratos históricos eliminados —
independiente del trabajo ya hecho, para no confiar solo en la propia verificación.

## Regla vigente

Los 4 agentes reportaron 19 hallazgos reales (más el mismo enlace roto preexistente y ajeno de
`ep-01/h08.md`, ya conocido de rondas anteriores), todos corregidos:

- `AGENTS.md:247` — link roto a `docsV2/...` → corregido a `docs/...`.
- `scripts/probar-bot-evaluador.sh`, `scripts/probar-bot-tutor.sh` — comentario con ruta
  `docsV2/...` → corregido.
- 4 comentarios Javadoc (`AttemptEvaluationService.java`, `OutputAntiLeakGuard.java`,
  `KafkaTopics.java`, `ArchitectureTest.java`) — `docsV2` → `docs`.
- `llm-workbench/README.md:113` — link roto a `docs/estado-implementacion/ep-09/` → corregido a
  `docs/06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-09/README.md`.
- `compose.yaml` — 6 comentarios con numeración plana vieja (`docs/39-...`, `docs/12 §1/§3`,
  `docs/06 Parte 2`, `docs/25`, `docs/estado-implementacion/ep-09/ingesta.md`) → corregidos a las
  rutas reales vigentes (`02-arquitectura-y-plataforma/05-servicios-docker.md`,
  `03-capacidades-de-ia/rag-e-ingesta/01-almacenamiento-e-ingesta.md`,
  `06-operacion-calidad-y-pruebas/03-matriz-de-pruebas-de-infraestructura.md`,
  `06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-09/ingesta.md`).
- `contracts/MOCK.md:46` — `demo/courses/expectations.json` → `lab/courses/expectations.json`.
- `03-capacidades-de-ia/golden-set-y-calibracion/plan-skills-calibracion.md` — 5 menciones a
  `docsV2`: 2 eran rutas de archivo reales → corregidas a `docs/`; 3 eran prosa comparando contra
  la terminología vigente en ese momento → se dejaron con la aclaración explícita "(entonces
  `docsV2`)" en vez de borrar el contraste histórico.
- `08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/presentaciones/mapa-conceptual-interactivo.html` —
  56 ocurrencias de `llm-service/docsV3/` dentro de un array JS (`repoPath`) → reemplazadas por
  `llm-service/docs/`.

Los agentes también confirmaron explícitamente qué **no** era un hallazgo (menciones legítimas en
`registro/`, enlaces a carpetas sin extensión, la palabra "demo" usada como sustantivo común, los 7
enlaces `file:///d:/...` de `02-spike-rag-multifuente.md` ya señalados en una ronda anterior).

## Documentos corregidos o agregados

**Actualizados:** `AGENTS.md`, `scripts/probar-bot-evaluador.sh`, `scripts/probar-bot-tutor.sh`,
4 archivos `.java` en `app/`, `llm-workbench/README.md`, `compose.yaml`, `docs/contracts/MOCK.md`,
`docs/03-capacidades-de-ia/golden-set-y-calibracion/plan-skills-calibracion.md`,
`docs/08-preguntas-investigacion-y-sincronizaciones/04-investigacion-y-material/presentaciones/mapa-conceptual-interactivo.html`.

## Evidencia de prueba

2991 enlaces relativos de `docs/` verificados después de la corrección: 0 rotos nuevos (el único
roto es el mismo preexistente y ajeno de siempre). Barrido adicional de `docsV2`/`docsV3` en todo
`llm-service/` (`.md`, `.java`, `.sh`, `.yaml`): las únicas menciones restantes están en
`docs/registro/` (registros de cambios, legítimos) y en las 2 notas aclaratorias de
`plan-skills-calibracion.md`.
