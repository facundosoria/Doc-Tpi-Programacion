# Product Owner — pendientes

> Fuente completa: [18 §4.8](../../18-contratos-inter-equipos.md#48-product-owner),
> [08 Parte B](../../08-decisiones-y-pendientes.md) (P-04, P-05, P-09, P-10, P-11, C-1, C-2),
> [17 §7.3](../../17-mapa-de-integracion.md#73-quién-nos-bloquea-y-a-quién-bloqueamos) (N6).

## 🔴 Crítico — responsable y fecha del golden set (P-04 / C-1)

**El punto más urgente de toda la lista.** Es un hito de calendario académico, no técnico: sin
calibración aprobada ningún curso pasa de borrador a activo, y no hay override de ADMIN
(RF-IA-36). Lo que hay que pedir, concreto: tiempo de un docente para revisar y puntuar las
cinco dimensiones de cada conversación, con la versión publicada terminada **3 semanas antes
del inicio del período lectivo**.

## 🔴 I-15 — quién construye la pantalla del golden set

Cuatro documentos le dan cuatro dueños distintos. Cruzado con
[`tema-12-backoffice-admin/pendientes.md`](../tema-12-backoffice-admin/pendientes.md) y
[`frontend-angular/pendientes.md`](../frontend-angular/pendientes.md). Si el PO no puede
resolver esto y el responsable del golden set en la misma sesión, priorizar el **responsable
del contenido** (P-04) — la pantalla sin contenido no sirve; el contenido sin pantalla se puede
cargar a mano una vez.

## 🟡 I-06 — techo de cuota diaria (RF-IA-22)

Tres números circulando: 15 (decisión), 10 (inventario), 8 (presupuesto). El presupuesto del
cuatrimestre depende de cuál se confirme. Recomendación de doc 08 (P-05): 15 mensajes/desafío,
60/día, para empezar y calibrar con datos reales.

## 🟡 C-2 — ¿el free tier puede tocar datos de alumnos?

Consulta legal, no técnica — define el modelo de costos entero. Recomendación: free tier solo
para demo/desarrollo con datos sintéticos; producción, solo con política de retención
verificada por escrito.

## Decisiones de producto puntuales (doc 04)

- **P-02** — fail-open vs. fail-closed del moderador de chat si el servicio no está disponible.
- **P-09 / P-10 / P-11** — si la respuesta del agente `@mención` también se modera, si sus
  menciones cuentan contra los límites de cuota, y cuántos agentes puede haber (Fase 3, baja
  prioridad hoy).
- Dónde corta el umbral entre severidad baja y media del moderador — se afina con datos reales.
