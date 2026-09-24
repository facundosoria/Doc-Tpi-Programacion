# Política de riesgo y recuperación por clase de fallo (secciones 9 y 10 de la guía fuente)

> Contenido conservado verbatim de `docs/17-guia-organizacion-documentacion-harness-engineering.md`.

## 9. El modelo propone; la política autoriza

Algunas reglas nunca deben depender de que el modelo las recuerde:

```text
nunca publicar sin aprobación
nunca exponer un secreto
nunca escribir fuera del workspace
nunca superar el límite de gasto
nunca declarar una prueba aprobada si no se ejecutó
```

Estas no son sugerencias de prompt: son políticas. El diseño más seguro mantiene la política fuera del bucle de razonamiento.

La gradación de riesgo del artículo es:

```text
BAJO RIESGO
leer archivos, buscar, inspeccionar
  -> automático

CAMBIO REVERSIBLE
editar workspace, ejecutar pruebas
  -> automático con traza

EFECTO EXTERNO
enviar mensaje, desplegar, comprar
  -> aprobación explícita

IRREVERSIBLE O SENSIBLE
eliminar datos, rotar credenciales, publicar globalmente
  -> bloqueo fuerte o prohibición
```

Cuanto más fuerte la consecuencia, más fuerte debe ser la barrera. Autonomía no significa ausencia de control: significa poder operar libremente dentro de un límite claramente impuesto.

### Política documental

| Acción | Política predeterminada |
|---|---|
| leer, buscar, inventariar | automática |
| crear un archivo nuevo dentro del alcance | automática con traza |
| editar un documento reversible | automática con diff y comprobaciones |
| mover documentos | automática solo tras analizar enlaces y propiedad |
| cambiar una decisión o fuente de verdad | aprobación del responsable |
| eliminar información | prohibida o aprobación explícita y respaldo |
| publicar fuera del repositorio | aprobación explícita |
| tocar secretos o credenciales | prohibida |
| alterar configuración protegida | aprobación explícita |

---

## 10. La recuperación debe apuntar a la clase de fallo

La estrategia de recuperación más común es: "Algo falló. Intentá de nuevo". Eso no es recuperación: es repetición.

El harness debe clasificar el fallo antes de decidir la siguiente acción:

```text
timeout de herramienta
  -> reintentar con backoff

argumentos inválidos
  -> reparar la llamada

contexto faltante
  -> recuperar la fuente específica

prueba fallida
  -> inspeccionar el comportamiento que falló

permiso denegado
  -> pedir aprobación o elegir un camino seguro

requisitos contradictorios
  -> escalar a una persona

fallo repetido sin cambios
  -> detener el bucle
```

Un reintento debe cambiar por lo menos una condición relevante. De lo contrario, el sistema paga por reproducir el mismo fallo.

El bucle acotado de un agente es:

```text
observar
  -> decidir
      -> actuar
          -> medir
              -> aceptar
              -> reparar
              -> escalar
              -> detener
```

Todo bucle necesita un presupuesto:

- máximo de intentos;
- máximo de tiempo;
- máximo de gasto;
- máximo de alcance destructivo;
- condición de escalamiento.

Los agentes fiables saben continuar y también saben cuándo continuar dejó de ser racional.

### Clases de fallo para documentación

El agente debe registrar la clase, evidencia y respuesta:

| Clase | Ejemplo | Acción |
|---|---|---|
| `tool_timeout` | la búsqueda o renderizado excede el tiempo | reintentar con backoff y límite |
| `invalid_arguments` | ruta o patrón mal formado | corregir la llamada |
| `missing_context` | no se encontró la fuente de verdad | recuperar índice o escalar |
| `broken_reference` | enlace a archivo inexistente | reparar referencia o marcar bloqueo |
| `conflicting_sources` | dos documentos normativos difieren | no elegir arbitrariamente; escalar |
| `ownership_denied` | el archivo pertenece a otro equipo | pedir autorización |
| `verification_failure` | índice o comprobación no pasa | corregir causa y volver a verificar |
| `repeated_unchanged_failure` | tres intentos con igual resultado | detener y escalar |
