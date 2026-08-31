# Front del CI

Página de solo lectura que muestra las corridas del gate: quién las disparó, cuáles
están en cola, cuáles corren en paralelo y en qué etapa va cada una.

## Qué hace y qué no

**No ejecuta nada.** La cola, los workers y la ejecución los pone GitHub Actions
sobre el runner self-hosted. Esto solo lee la API, normaliza la respuesta y la
sirve. Si el front se cae, el CI sigue funcionando igual.

Esa separación es la razón de que esto sean ~400 líneas y no un proyecto de semanas.
La alternativa era construir un CI propio —cola, workers, base de datos, historial—
y se descartó por eso.

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

## Cómo se conecta sin tocar el front

`servidor.py` **traduce** la respuesta de GitHub a un formato propio, y la página
consume solo ese formato:

```text
API de Actions  ->  servidor.py  ->  { corridas: [...] }  ->  index.html
datos-prueba.json  ->             ->  el mismo formato   ->
```

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

- Los contadores de hallazgos (`bloquea` / `avisa`) vienen en cero desde la API:
  GitHub no los expone. Salen del step summary, que hay que parsear o publicar como
  artifact del workflow.
- El detalle del fallo (`detalle_fallo`) hoy solo existe en los datos de prueba, por
  el mismo motivo.
- Falta el repo oficial y el runner para que haya corridas reales que mostrar.
