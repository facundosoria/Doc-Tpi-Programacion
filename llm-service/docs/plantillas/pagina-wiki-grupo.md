# Plantilla — Página de la Wiki de Taiga por grupo

> Copiar este contenido al crear una página nueva en la Wiki del proyecto
> «Plataforma de Aprendizaje Gamificado de Programación». Reglas y checklists de cada
> diagrama en [27 · Guía de la Wiki de Taiga](../27-guia-wiki-taiga.md) (§4).
>
> Reemplazar `GXX` por el número de grupo asignado por la cátedra. Borrar los textos de
> ejemplo antes de publicar.

---

# GXX - [Título del Tema]

*(Ejemplo: `G03 - Gestión de Clientes`)*

## Descripción del tema u objeto

Explicar brevemente el propósito o alcance de este módulo, funcionalidad o componente.
Debe quedar claro **qué hace**, **para qué sirve** y **qué partes del sistema involucra**.

> *Ejemplo:* Este módulo gestiona las operaciones de alta, baja y modificación de
> clientes, incluyendo validaciones de datos y vinculación con el sistema de pedidos.

## Referencia a Historia(s) de Usuario (HU)

Listar las HU asociadas. Cada una debe estar documentada en el backlog de Taiga y
enlazada con su **enlace permanente** para asegurar la trazabilidad.

> *Ejemplo:*
> - [HU-03 – Registrar nuevo cliente](): como empleado, quiero registrar un nuevo cliente para poder asociarle pedidos.
> - [HU-07 – Modificar datos del cliente](): como usuario, quiero modificar los datos de un cliente existente para mantener la información actualizada.

## Diagramas requeridos

Elaborar en este orden (checklist de cada uno: [27 · Guía de la Wiki §4](../27-guia-wiki-taiga.md)):

1. **DER** (Entidad–Relación)
2. **BPMN** (Proceso de Negocio)
3. **Flujograma** (opcional, si aplica)
4. **Clases**
5. **Estados**
6. **Secuencias**
7. **Microservicios**

Mantener todos los diagramas actualizados ante cualquier modificación del sistema o del
código fuente.

### Enlace(s) al diagrama en Draw.io

Enlaces directos a cada diagrama, **con permiso editable**, organizados en la carpeta
compartida del grupo.

> *Ejemplo:* [Enlace al diagrama – Gestión de Clientes](https://app.diagrams.net/)

### Explicación de los diagramas

Interpretación breve de cada diagrama. Debe responder:

- ¿Qué muestra?
- ¿Qué decisiones o relaciones se destacan?
- ¿Cómo se vincula con el resto del sistema?

> *Ejemplo:* En el DER se representan las entidades Cliente y Pedido, vinculadas por una
> relación uno a muchos. Se destacan las claves primarias (`cliente_id`, `pedido_id`) y
> las foráneas que aseguran la integridad referencial.

## Observaciones y notas técnicas

*(Opcional pero recomendable.)* Decisiones técnicas, dependencias o particularidades del módulo.

> *Ejemplo:* Este módulo se comunica con el servicio de autenticación mediante API REST.
> El modelo Cliente implementa validaciones de formato de correo y de duplicados.

## Documentación de endpoints

Repetir el bloque por cada endpoint. En Tema 07 las rutas privadas van bajo
`/api/llm/**` y la correlación usa `traceparent` + `X-Request-Id`
([00 · Fuentes de verdad](../00-fuentes-de-verdad-y-convenciones.md)); el `/api/v1/…`
del ejemplo es genérico.

---

**Controller:** `EjemploController`

**Acción:** `Registrar ejemplo (createEjemplo)`
**Método:** `POST` · `GET` · `PUT` · `PATCH` · `DELETE`
**URL:** `/api/v1/ejemplo/wiki/{orderId}/endpoints`
**Descripción:** …

**Request**

*Path variables* (solo si contiene):
- `orderId`: identificador de la orden (ejemplo: `ORD-982374`)

```json
{
  "example": "request"
}
```

**Response**

```json
{
  "example": "response"
}
```

---

## Recomendaciones finales

- Redactar con lenguaje técnico, claro y uniforme.
- Diagramas legibles y consistentes con la implementación real.
- Actualizar esta página cuando el módulo cambie.
- No duplicar información entre grupos: vincular mediante enlaces internos.
- Respetar el formato y la estructura de esta plantilla.

> **Objetivo final:** que cualquier persona externa (docente, compañero o revisor) pueda
> entender qué hace el módulo, cómo funciona y cómo se integra al sistema **sin leer el
> código**.
