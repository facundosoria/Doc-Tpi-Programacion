# Front del CI

Página de solo lectura que muestra las corridas del gate: quién las disparó, cuáles
están en cola, cuáles corren en paralelo y en qué etapa va cada una.

## Qué hace y qué no

**No ejecuta nada.** La cola, los workers y la ejecución los pone GitHub Actions
sobre el runner self-hosted. Esto solo lee la API, normaliza la respuesta y la
sirve. Si el front se cae, el CI sigue funcionando igual.

Esa separación es la razón de que esto sean ~700 líneas y no un proyecto de semanas.
La alternativa era construir un CI propio —cola, workers, base de datos, historial—
y se descartó por eso.

## Las dos fuentes

La página muestra dos listas separadas, y vienen de lugares distintos:

| Lista | De dónde sale | Qué incluye |
|---|---|---|
| **En curso / Historial** | la API de Actions | lo que se disparó con un push o un pull request |
| **Corridas en el server** | el buzón, un directorio del server | lo que se corrió con `./qa.sh --remoto`, y también las corridas del runner |

**El detalle por etapa de las dos listas sale del buzón.** A la API se le pide solo el
listado de corridas: un pedido por ventana de cache, en vez de uno por corrida. Con 12
corridas y un cache de 15 s eso pasó de ~3120 pedidos por hora a ~240, contra un límite
de 5000 — y de paso el detalle es mejor, porque son las 13 etapas del gate y no los
tres steps del workflow.

El buzón existe por dos motivos. Una corrida `--remoto` **no pasó por GitHub**, así
que la API no sabe que ocurrió. Y de las que sí conoce, GitHub expone los tres steps
del workflow —checkout, elegir perfil, verificar—, no las 13 etapas del gate: para
comparar una corrida tuya contra una de CI etapa por etapa hace falta el detalle, y
ese detalle solo lo tiene el gate.

Quien lo escribe es `reportar.py`, cuando la variable `QA_SPOOL` apunta a un
directorio (ver [`tools/qa/README.md`](../qa/README.md)). Un JSON por corrida, con
las 13 etapas, incluidas las que **no** se ejecutaron.

Se reescribe en cada cambio de etapa, siempre sobre el mismo archivo, así que la
corrida **se ve avanzar en vivo**: el motor ya emitía un evento por etapa con
`flush`, solo faltaba no esperar al final para volcarlo. Un registro con
`terminado: false` que hace más de 600 s que no se mueve se muestra como cancelada.

Las dos fuentes son independientes a propósito: el buzón se lee aunque no haya token
y aunque la API de GitHub esté caída.

## Levantarlo

```bash
docker compose -f tools/ci-front/docker-compose.yml up -d
```

Queda en el puerto **8099**. No usa el 80 ni el 8088 a propósito: en el server del
equipo esos los ocupa `codemon_front`.

## Conectarlo a las corridas reales

Sin configuración sirve **datos de prueba** y lo dice en pantalla. Para leer las
corridas de verdad hacen falta dos variables:

```bash
CI_REPO=facundosoria/Doc-Tpi-Programacion
CI_TOKEN=<token con permiso de lectura de Actions>
CI_RUNNERS=2
```

`CI_RUNNERS` es cuántos runners self-hosted hay instalados. **Ese número es el que
decide si las corridas van en paralelo o hacen fila:** con uno, seis personas se
encolan de a una; con tres, corren de a tres. El front lo muestra, no lo controla.

El token va con el permiso mínimo — solo lectura de Actions. No necesita escribir
nada, igual que el runner no necesita escribir en el repo.

El buzón se configura aparte, y tiene default:

```bash
CI_BUZON_HOST=/opt/TP-Pipelines/corridas   # qué directorio del server se monta
CI_BUZON_MAX=12                            # cuántas corridas muestra
```

El compose lo monta **de solo lectura** en `/buzon`, y `CI_BUZON` ya apunta ahí
adentro del contenedor: por eso la ruta del host se puede mover sin tocar el código.
De solo lectura a propósito — el front no tiene por qué poder borrar el historial de
nadie.

Si el directorio no existe, la lista sale vacía con un cartel que explica cómo se
llena. No es un error: en una máquina que no es el server, no hay buzón.

## Cómo se conecta sin tocar el front

`servidor.py` **traduce** la respuesta de GitHub a un formato propio, y la página
consume solo ese formato:

```text
API de Actions     ->                ->  { corridas: [...] }     ->
el buzón del gate  ->  servidor.py   ->  { corridas_qa: [...] }  ->  index.html
datos-prueba.json  ->                ->  el mismo formato        ->
```

Las dos listas usan la **misma** forma de corrida, así que la página las dibuja con
el mismo componente y no sabe de dónde salió cada una.

Por eso los datos de prueba y los reales son indistinguibles para la página, y
conectar la API no toca una línea del front. Es el mismo patrón que usa el gate con
sus eventos JSON.

## Dos cosas antes de exponerlo

**No tiene autenticación.** Hoy muestra datos de prueba, así que no hay nada que
proteger. Cuando lo conectes a la API real va a mostrar nombres de rama, mensajes de
commit y quién trabajó en qué. Si eso no puede ser público, ponelo detrás del nginx
que ya corre ahí, o restringí el puerto por firewall.

**No lee del socket de Docker.** A diferencia del runner, este contenedor corre con
un usuario sin privilegios y no puede tocar nada del host. Es una pieza mucho menos
sensible que el runner, y conviene que siga siéndolo.

## Estructura

```text
servidor.py         lee la API, normaliza y sirve. Solo biblioteca estándar
datos-prueba.json   el escenario de prueba, en el formato ya normalizado
static/index.html   la página. Refresca sola, y solo redibuja si algo cambió
Dockerfile          python:3.13-alpine, usuario sin privilegios
docker-compose.yml
```

## Pendiente

- El detalle del fallo (`detalle_fallo`) hoy solo existe en los datos de prueba. El
  registro del buzón trae la cuenta de hallazgos por etapa, no el hallazgo en sí.
- Una corrida de GitHub que no dejó registro en el buzón —una anterior a que esto
  existiera, o una que murió antes de escribir— se muestra sin detalle de etapas.
  La API no lo puede suplir: expone los steps del workflow, y las 13 etapas del
  gate viven todas adentro de uno.
- Falta instalar el runner para que haya corridas reales que mostrar. El
  procedimiento está en [`tools/qa/README.md`](../qa/README.md), sección
  "Puesta en marcha en el server".
- `CI_RUNNERS` se configura a mano y no se valida: si no coincide con cuántos
  runners hay instalados, la página miente sobre la capacidad y no hay forma de
  notarlo desde acá.
