# 24 — Convención obligatoria de cobertura de código

> Esta convención aplica a todo código de producto de `llm-service` y de sus interfaces Angular. Es requisito de aceptación de cada Pull Request y de cada Sprint Review.

## Regla

| Área | Herramienta de referencia | Métrica obligatoria | Mínimo |
|---|---|---:|---:|
| Backend Java | JaCoCo | Instrucciones cubiertas | **95%** |
| Frontend Angular | Vitest con proveedor V8 o Istanbul | Sentencias cubiertas | **95%** |

El frontend no expone una métrica JVM de instrucciones. Por eso se usa la cobertura de **sentencias** como equivalente verificable de instrucciones ejecutables.

La medición se realiza sobre todo el código de producción de cada aplicación, no sólo sobre los archivos modificados por el PR. Una cobertura inferior al umbral bloquea el merge y no permite declarar terminado el sprint.

## Alcance de la medición

- Backend: `llm-service/src/main/java/**`.
- Frontend: `llm-workbench/src/**/*.ts` y, cuando se incorpore, el frontend Angular compartido.
- Se excluye únicamente código generado automáticamente por una herramienta. La exclusión requiere que el archivo o patrón esté documentado en la configuración de cobertura y que el PR indique su motivo.
- No se excluyen controllers, servicios, repositorios, guards, interceptores, componentes, configuraciones ni ramas de manejo de errores sólo para aumentar la cifra.

## Ejecución y evidencia

El pipeline debe ejecutar las pruebas y publicar los reportes HTML/XML de cobertura. Debe fallar si alguno de los dos umbrales queda por debajo de 95%.

| Aplicación | Comando esperado | Evidencia |
|---|---|---|
| Backend | `mvn test` con JaCoCo configurado | `target/site/jacoco/index.html` y `jacoco.xml` |
| Frontend | `npm test -- --coverage` | reporte V8/Istanbul y resumen en consola/CI |

El PR debe informar el porcentaje de backend y frontend, enlazar ambos reportes y describir cualquier exclusión aprobada. La Review valida el reporte generado por CI; no acepta porcentajes declarados manualmente.

## Casos que deben cubrirse

Además de alcanzar el porcentaje mínimo, las pruebas deben cubrir los recorridos relevantes de la historia: caso exitoso, validación de entrada, autorización, idempotencia, fallo de dependencia y recuperación cuando corresponda. El porcentaje nunca sustituye las pruebas de contrato, integración, seguridad ni demo exigidas por el playbook.

## Incumplimiento

Si el umbral no se cumple, la historia vuelve a estado en progreso. No se reduce el mínimo por urgencia, y un bloqueo de infraestructura se registra conforme al workflow diario con evidencia y responsable.
