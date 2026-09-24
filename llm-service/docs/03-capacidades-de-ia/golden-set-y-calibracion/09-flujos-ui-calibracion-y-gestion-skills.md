# Módulo de gestión de skills y flujos de UI de calibración

> **Estado:** vigente para diseño e implementación (directivas de Producto).
> **Decisiones:** D-142 a D-153. D-154 (metadatos sin nueva versión) se documenta en
> [05 — Skills: catálogo y formato §4.1](05-skills-catalogo-y-formato.md#41-metadatos-de-skill-sin-nueva-versión-de-contenido-d-154)
> por pertenecer temáticamente al versionado de skills; se referencia acá por completitud.

## 1. Módulo autónomo de gestión de skills (D-142)

Skills tiene una **vista y catálogo propios**, separados de las pantallas de desafíos y de la
selección durante una calibración. El módulo centraliza: catálogo, búsqueda, filtros, favoritos,
carga, versiones, visibilidad, clonado, seguridad y vistas de uso. Los selectores de calibración
**reutilizan** ese catálogo, pero no lo duplican ni se convierten en el lugar principal para
gestionar skills (ver [05 — Skills: catálogo y formato §5](05-skills-catalogo-y-formato.md#5-gestión-completa-y-trazabilidad-por-desafío-d-05)
para el detalle funcional de esta vista, definido por D-05).

## 2. Historial de uso de skills por desafío (D-143)

Dentro del módulo Skills, la vista de uso muestra tanto los **desafíos con configuración efectiva
actual** como el **historial de calibraciones anteriores** que utilizaron cada skill. La consulta de
usos distingue asociación **activa** e **histórica**, versión exacta de skill, curso, desafío y
versión de calibración. El acceso se limita a los cursos autorizados del docente o a Administración,
y la información se vincula a los snapshots para auditoría.

## 3. Formulario de calibración: selección unificada de recursos (D-144, D-145, D-146)

### 3.1 Selección unificada dentro de cada calibración (D-144)

Cada formulario de calibración reúne su recurso objetivo, el Golden Set, el paquete de rúbricas y una
o más skills. El global elige un **curso**; el específico elige un **curso ya calibrado** y luego su
**desafío**.

La interfaz compone la configuración mediante listas o modales desplegables según corresponda, y
muestra el **perfil resultante** antes de iniciar la corrida. El backend recibe una única solicitud
tipada con las versiones exactas seleccionadas, valida permisos, seguridad, compatibilidad y
presupuesto de contexto, y **materializa su snapshot inmutable** (ver D-160 en
[08 — Máquina de estados](08-maquina-de-estados-corridas-y-activaciones.md#compilador-determinista-del-perfil-efectivo-d-160)).

### 3.2 Flujos separados para curso y desafío (D-145)

La calibración global de curso y la subcalibración de desafío son **flujos separados**. Para acceder a
las calibraciones de desafíos, el docente primero selecciona un curso que ya esté calibrado (coherente
con la precondición D-07, ver
[06 — Subcalibración jerárquica §1](06-subcalibracion-jerarquica.md#1-precondición-no-hay-subcalibración-sin-calibración-global-aprobada-d-07)).

La interfaz presenta entradas separadas para ambos flujos. La subcalibración filtra y muestra sólo los
desafíos asociados al curso global activo y aprobado que estén registrados en `llm-service` como
requeridos de calibración, evitando seleccionar desafíos de otro curso o iniciar una calibración
específica sin su baseline válida.

### 3.3 Una corrida por desafío (D-146)

Cada corrida de subcalibración corresponde a un **único desafío**; no se calibran varios desafíos en
una misma corrida. La configuración efectiva, Golden Set, rúbrica, skills, métricas y resultado quedan
vinculados inequívocamente a un solo `challengeId`. Esto evita mezclar contextos académicos y conserva
explicabilidad por desafío, aun cuando se ejecuten corridas independientes en paralelo.

## 4. Borradores de calibración (D-147 a D-149)

### 4.1 Borradores persistentes (D-147)

El docente puede **guardar un borrador incompleto** de calibración global o de subcalibración y
retomarlo más adelante sin iniciar una corrida. El borrador conserva sus selecciones y validaciones
parciales, pero **no invoca al proveedor** ni altera la calibración activa. Sólo al iniciar una corrida
se exigen todos los recursos obligatorios, permisos, seguridad, compatibilidad y presupuesto de
contexto; la corrida resultante crea su snapshot inmutable.

### 4.2 Privacidad de borradores (D-148)

Los docentes autorizados **distintos del creador no pueden ver ni editar** sus borradores de
calibración. El borrador se autoriza por su creador y permanece fuera de las vistas compartidas del
curso. Una vez iniciada la corrida, su resultado y posterior versión entran en el historial compartido
según las autorizaciones del curso (ver D-139 en
[08 — Máquina de estados §6.1](08-maquina-de-estados-corridas-y-activaciones.md#61-lectura-compartida-del-historial-por-curso-autorizado-d-139));
no se exponen configuraciones incompletas a otros docentes.

### 4.3 Consulta administrativa de borradores privados (D-149)

Administración puede consultar borradores privados para fines de **auditoría y soporte**. Este acceso
es de **solo lectura**, queda auditado con actor y motivo, y **no habilita** a Administración a
editar, iniciar, publicar o exponer el borrador a otros docentes. La privacidad entre docentes del
curso se mantiene intacta.

## 5. Autorización de curso y su efecto sobre skills y borradores (D-150, D-151)

### 5.1 Autorización gobernada por Cursos (D-150)

`courses-service` gobierna las autorizaciones de docentes por curso. Si un docente pierde esa
autorización, **pierde el acceso** a borradores, historial y operaciones de calibración de ese curso.
`llm-service` **no administra ni replica membresías**: en cada consulta o mutación protegida, aplica la
identidad y autorización vigente que llegan por el API Gateway y los contratos de Cursos. El historial
se conserva para auditoría bajo acceso administrativo, pero no queda accesible para quien ya no esté
autorizado en el curso.

### 5.2 Skills privadas restringidas por autorización de curso (D-151)

Al perder autorización sobre un curso, un docente **no puede gestionar** sus skills privadas asociadas
a ese curso: permanecen sólo para auditoría administrativa. `llm-service` aplica la autorización
vigente provista por Cursos al listar, abrir o modificar una skill privada vinculada al curso. No
administra el alcance ni la membresía; sólo protege sus propios datos y operaciones frente a la
autorización delegada.

## 6. Carga de skill desde el formulario de calibración (D-152)

Si el docente carga una **nueva skill desde un formulario de calibración** y ésta supera la
sanitización, se agrega **automáticamente** a la selección actual. La carga conserva el flujo
síncrono de seguridad (ver
[04 — Seguridad y gobierno de skills](../../04-seguridad-datos-y-cumplimiento/04-seguridad-y-gobierno-de-skills.md))
y crea la versión de skill **antes** de incorporarla al borrador de calibración. Si la validación
falla, no se añade ni se inicia la corrida; el docente recibe el feedback seguro correspondiente y
puede elegir otra skill o corregir la carga.

## 7. Estimación visible de presupuesto de contexto (D-153)

El formulario de calibración muestra **en tiempo real** la estimación de tokens del perfil
seleccionado y advierte antes de iniciar si supera el límite. Cada cambio de rúbrica, Golden Set o
skills actualiza la estimación y explica los componentes de mayor consumo.

La interfaz **orienta** al docente, pero el **backend y el AI Gateway conservan la validación
autoritativa final** inmediatamente antes de la invocación (coherente con D-49 y D-160, ver
[06 — Subcalibración jerárquica §6](06-subcalibracion-jerarquica.md#6-modelo-híbrido-coursebaseline--challengeoverlay-d-49)
y [08 — Máquina de estados](08-maquina-de-estados-corridas-y-activaciones.md#compilador-determinista-del-perfil-efectivo-d-160)).
Esta estimación de interfaz es un apoyo de UX; nunca reemplaza el control preventivo de cuota y
contexto descripto en D-28 (fuera de este rango).

## Trazabilidad de esta sección

| Decisión | Tema |
|---|---|
| D-142 | Módulo autónomo de gestión de skills, reutilizado por los selectores de calibración. |
| D-143 | Historial de uso de skills por desafío (activo e histórico). |
| D-144 | Selección unificada de recursos por formulario de calibración. |
| D-145 | Flujos separados para calibración de curso y de desafío. |
| D-146 | Una corrida de subcalibración por desafío. |
| D-147 | Borradores persistentes de calibración. |
| D-148 | Privacidad de borradores entre docentes. |
| D-149 | Consulta administrativa de solo lectura sobre borradores privados. |
| D-150 | Autorización de curso gobernada por Cursos; pérdida de acceso a borradores/historial. |
| D-151 | Skills privadas restringidas por pérdida de autorización de curso. |
| D-152 | Carga de skill desde el formulario de calibración, incorporación automática tras sanitización. |
| D-153 | Estimación visible de presupuesto de contexto, con validación autoritativa en backend. |
