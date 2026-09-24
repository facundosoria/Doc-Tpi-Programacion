# Evidencia de finalización y verificación adversarial (secciones 7 y 8 de la guía fuente)

> Contenido conservado verbatim de `docs/17-guia-organizacion-documentacion-harness-engineering.md`.

## 7. La finalización requiere evidencia

Que un agente diga "terminado" no demuestra que la tarea esté terminada. Es solo otra salida del modelo.

La finalización debe decidirse a partir de cambios observables en el entorno.

| Afirmación | Evidencia exigible |
|---|---|
| "El bug está corregido" | la prueba que fallaba ahora pasa |
| "La página funciona" | el flujo del navegador se completó |
| "La migración es segura" | la simulación y el rollback pasan |
| "El informe es correcto" | los valores coinciden con los datos fuente |
| "La tarea está completa" | todas las verificaciones de aceptación pasan |
| "La documentación está organizada" | estructura, enlaces, índices, metadatos y referencias pasan sus controles |

El harness debe ejecutar primero las comprobaciones deterministas más baratas:

```text
sintaxis
  -> tipos / esquema
      -> pruebas enfocadas
          -> pruebas de integración
              -> revisión visual o semántica
                  -> aprobación humana
```

No debe utilizarse otro modelo cuando un compilador, esquema, checksum, consulta o prueba puede responder la pregunta. Los modelos deben utilizarse para la ambigüedad; el código, para la mecánica. Un modelo puede proponer que la tarea terminó; solo el entorno puede probarlo.

### Evidencia para documentación

El agente debe comprobar, según corresponda:

- sintaxis Markdown y front matter;
- enlaces internos y anclas;
- rutas existentes;
- ausencia de referencias a nombres antiguos;
- unicidad de títulos o identificadores;
- presencia de propietario, alcance, estado y fuente de verdad;
- coherencia entre índices y archivos reales;
- coincidencia entre contratos y documentos de implementación;
- consistencia de términos del glosario;
- ausencia de duplicados que compitan como fuente de verdad;
- renderizado legible si el formato se publica como PDF, HTML u otro medio.

---

## 8. La verificación debe atacar el resultado

El trabajador y el evaluador no deben compartir el mismo objetivo.

- El trabajador intenta producir la solución más fuerte.
- El evaluador intenta encontrar el motivo por el cual debe rechazarse.

```text
trabajador
  -> produce candidato

verificador
  -> comprueba el contrato
  -> busca casos faltantes
  -> prueba afirmaciones sin respaldo
  -> intenta romper el resultado

si sobrevive
  -> aceptar

si falla
  -> devolver evidencia enfocada
```

La asimetría importa. Si se le pide al mismo agente, en el mismo contexto, que "revise dos veces su trabajo", suele conservar las suposiciones que produjeron el error.

Una etapa de verificación útil debe tener:

- una rúbrica explícita de rechazo;
- acceso al artefacto producido;
- acceso al contrato de aceptación;
- herramientas independientes o contexto fresco cuando sea necesario;
- permiso para rechazar sin reparar.

Verificar no es pedir una segunda opinión. Es intentar refutar el resultado.

### Rúbrica de rechazo documental

El verificador debe rechazar si encuentra, entre otros casos:

- una afirmación que no tiene fuente o evidencia;
- dos documentos que compiten como fuente de verdad sin una regla de precedencia;
- un enlace roto o una ruta inexistente;
- una sección ubicada en una categoría que no corresponde a su propósito;
- un documento que mezcla tutorial, how-to, referencia y explicación sin límites claros;
- información importante omitida al reorganizar;
- un cambio de contenido presentado como simple movimiento;
- una decisión vigente alterada sin ADR o autorización;
- un índice incompleto;
- un documento que no permite determinar propietario, estado o fecha de vigencia;
- un recibo de cambios que declara verificaciones no ejecutadas.
