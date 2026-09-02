# Gate de calidad

Verifica la documentación y el código antes de que lleguen a `dev` o `main`.

## Usarlo

Lo único que necesitás instalado es **Docker**. El JDK, Maven y las herramientas
van adentro de la imagen.

```bash
./qa.sh                          # verifica lo que cambiaste
./qa.sh --all --perfil completo  # exactamente lo que corre el CI
./qa.sh --only tests             # una sola etapa
./qa.sh --self-test              # verifica el gate, no el repo
./qa.sh --remoto                 # lo mismo, pero ejecutado en el server
```

### `--remoto`: correrlo en el server en vez de tu máquina

Manda tu working tree al server por SSH, corre el gate allá y te trae el mismo
resumen. **No toca git ni el push**: viaja lo que tenés ahora, incluido lo que
todavía no commiteaste.

```bash
export QA_REMOTO=usuario@servidor
export QA_REMOTO_PUERTO=2222      # opcional, default 22
./qa.sh --remoto
```

Sirve para dos cosas:

- **Tu máquina no puede correr Docker**, o no querés prenderlo. `--remoto` es lo
  primero que hace `qa.sh`, antes de tocar Docker, justamente para eso.
- **El día que el repo no sea nuestro.** Dar de alta un runner self-hosted necesita
  permisos de administrador sobre el repositorio, y en un repo compartido con otros
  equipos ese runner le daría ejecución en el server a cualquiera que abra un pull
  request. `--remoto` no necesita permiso de nadie en GitHub, y expone el server
  solo a quien tenga SSH.

Cada persona tiene su directorio y su volumen de Maven en el server, así que seis
corriendo a la vez no se pisan. Medido sobre este repo: **9 segundos** en perfil
rápido, **63 segundos** en completo, transferencia incluida.

La primera corrida construye la imagen (unos minutos). Después arranca en un
segundo, porque el tag de la imagen es el hash del `Dockerfile`: solo se
reconstruye cuando alguien lo cambia.

El resumen queda en `.qa/resumen.md`, que es **el mismo Markdown** que GitHub
renderiza en la página del run. Si querés ver antes de pushear lo que va a
mostrar el CI, abrí ese archivo.

## Dónde está el control

El gate **no bloquea el push**: no hay hook de git. Corre cuando vos querés, y el
workflow corre solo al pushear.

| Momento | Qué pasa | Perfil |
|---|---|---|
| Antes de pushear | `./qa.sh` — tu red de seguridad, voluntaria | `rapido` |
| Al pushear a tu rama | El workflow corre y marca el commit | `rapido` |
| Al abrir el PR a `dev` | La corrida cara, sobre todo el repo | `completo` |
| Al mergear el PR | Branch protection bloquea el merge si está en rojo | — |

El perfil lo elige el workflow según el evento, no la persona. Cada push tiene que
ser barato o el gate deja de ser una red y pasa a ser un embudo: `rapido` son 120
segundos sobre lo que cambiaste. Los 600 segundos sobre todo el repo se pagan una
sola vez, al abrir el PR, que es donde branch protection decide si entra o no.

Un push directo a `dev` o `main` también va en `completo`: ahí no hay PR que lo
cubra, y además sobre la propia `main` el merge-base con `origin/main` da vacío,
así que `rapido` no miraría ni un archivo.

Por eso se trabaja con ramas y pull requests: **el workflow corre después del
push**, así que si pusheás derecho a `dev`, el código roto ya entró.

## Los niveles

Todo se configura en [`config/checks.yml`](config/checks.yml):

- **`bloquea`** — el hallazgo hace fallar la corrida
- **`avisa`** — se reporta y la corrida sigue
- **`arregla`** — solo formato: corrige en tu working tree en vez de protestar
- **`off`** — no se ejecuta

> ### `arregla` no existe en el CI
>
> El runner clona, corre y descarta el working tree. Un chequeo en `arregla` allá
> corregiría archivos que nadie va a ver, y la corrida quedaría en verde sin que
> el arreglo llegue nunca a la rama: ni protesta ni arregla. Es exactamente el
> tipo de falla silenciosa que el gate existe para evitar.
>
> Por eso `checks.yml` tiene `degradacion_ci`, que convierte `arregla` en
> `bloquea` cuando la variable `CI` está definida. En tu máquina no cambia nada:
> `./qa.sh` te sigue formateando el Java en vez de protestar.

> ### Ninguna regla nace en `bloquea`, nace en `avisa`
>
> Poné la regla nueva en `avisa`, mirá una o dos semanas qué encuentra sobre
> trabajo real, y subila recién cuando demuestre que los hallazgos son ciertos.
>
> El riesgo real no es que se filtre un typo. Es que a la tercera semana todos
> pusheen con el gate desactivado.

## Agregar un chequeo

1. Entrada en `config/checks.yml`, en `avisa`.
2. Adaptador en `lib/orquestar.py` que traduzca la salida de la herramienta a un
   hallazgo.
3. Si el mensaje crudo no se entiende solo, entrada en `config/reglas.yml` con
   qué pasó y cómo se arregla.
4. **Fixture en `tests/fixtures/` y caso en `tests/esperado.yml`.**

El punto 4 no es opcional.

## Por qué el self-test no es un lujo

Son diez herramientas de terceros que se actualizan solas y cambian el formato de
su salida. El día que cspell cambie su JSON, el adaptador deja de parsear y ese
chequeo pasa a "no encontró nada" — **en verde, sin error, y nadie lo nota**.

Un gate que falla en silencio es peor que no tener gate, porque genera confianza
que no corresponde. `--self-test` afirma que cada fixture dispara su regla **y
ninguna otra**, así que detecta tanto un adaptador roto como una regla que se
volvió ruidosa.

## Estructura

```text
config/                 genérico: viaja con el motor si se extrae a otro repo
  checks.yml            la política: qué corre y qué frena
  reglas.yml            el diagnóstico: qué significa cada regla y cómo se arregla
  es-rioplatense.txt    voseo, tecnicismos y anglicismos que el diccionario es-es
                        no trae. No es vocabulario del proyecto: es un hueco del
                        diccionario que cualquier equipo argentino necesita igual
config/proyecto/        propio de ESTE repo: cada proyecto trae el suyo
  owned-paths.txt       qué es nuestro (un `!` adelante excluye)
  project-words.txt     dominio y herramientas
  refs-registry.yml     IDs válidos de RF-IA-* y PAR-*
lib/
  scope.py         diff + propiedad + ruteo: sobre qué archivos corre
  orquestar.py     el motor: ejecuta las etapas y emite eventos JSON
  reportar.py      eventos -> consola y Markdown
  check_refs.py    referencias colgadas, anclas rotas, documentos huérfanos
  diff_gate.py     doble corrida: bloquea solo lo que rompió este cambio
  selftest.py
tests/             fixtures y afirmaciones
```

Está escrito **autocontenido**: sin rutas absolutas, y sin que el código sepa que
existen `docs/` o `codigo-ejemplo/ms-evaluacion-llm/` (eso vive en la config). Si algún día otro
equipo lo necesita, extraerlo es mover la carpeta.

## Alcance: solo lo que tu cambio rompe

La deuda vieja no bloquea a nadie; se paga cuando se toca el archivo.

Con una excepción deliberada: **links y referencias corren sobre todo el repo**,
comparando contra el merge-base. Si renombrás un título en tu documento, rompés un
ancla en un documento que no tocaste — y ese es tu cambio, aunque el archivo roto
no aparezca en tu diff. Lo que ya estaba roto se informa sin frenar.

## Puesta en marcha en el server

Hecho el 2026-09-01, salvo la rama `dev` y el branch protection, que siguen
pendientes. La máquina es `mk-luisao-02`: Ubuntu 24.04, 4 cores, 15 GB.

Todo el CI vive en `/opt/TP-Pipelines/`:

```text
/opt/TP-Pipelines/
├── front/     el front del CI
├── runners/   runner-1, runner-2, runner-3
├── remoto/    un directorio por persona, para ./qa.sh --remoto
└── test/      un checkout de prueba
```

### 1. El runner self-hosted

Va en una máquina dedicada. **El runner necesita acceso a Docker, y estar en el
grupo `docker` equivale a ser root en ese host**: cualquiera que pueda disparar un
workflow puede ejecutar lo que quiera ahí. Por eso el repo tiene que seguir siendo
privado — un runner self-hosted en un repo público le entrega el server a
cualquiera que abra un pull request.

Que sea dedicada no es una recomendación de estilo. Ese server corría la
producción de otro proyecto, y bajarla fue condición para instalar el runner: un
runner en el grupo `docker` al lado de una base de producción le entrega esa base
a cualquiera que pueda pushear.

Hay tres runners, corriendo como servicio con el usuario `runner-qa`:

```bash
sudo useradd -m -G docker runner-qa
sudo mkdir -p /opt/TP-Pipelines/runners/runner-{1,2,3}
```

Los comandos exactos del alta los da GitHub, con un token de registro que vence a
la hora: **Settings → Actions → Runners → New self-hosted runner**. El mismo token
sirve para los tres. En cada directorio:

```bash
sudo -u runner-qa ./config.sh --unattended --replace \
  --url https://github.com/facundosoria/Doc-Tpi-Programacion \
  --token <el-token> --name tpi-qa-N --work _work
sudo ./svc.sh install runner-qa
sudo ./svc.sh start
```

Sin `--labels`: el workflow pide `runs-on: self-hosted`, así que los tres quedan
intercambiables y GitHub reparte la cola entre ellos.

**Cada runner necesita su propio repositorio Maven.** El volumen es único por
host, así que dos corridas concurrentes compartirían `/root/.m2` y se pisarían las
descargas. Por eso cada directorio lleva un `.env` que el servicio carga al
arrancar:

```bash
echo 'QA_M2_VOLUME=tpi-qa-m2-1' > /opt/TP-Pipelines/runners/runner-1/.env
```

**El buzón de corridas**, para que el front pueda mostrar lo que corrió en el server
—las del runner y las de `--remoto`— con el detalle de las 13 etapas:

```bash
sudo install -d -m 1777 /opt/TP-Pipelines/corridas
echo 'QA_SPOOL=/opt/TP-Pipelines/corridas' >> /opt/TP-Pipelines/runners/runner-1/.env
```

El directorio va con sticky bit, como `remoto/`. Si no existe, no se escribe nada y
el gate funciona igual: el registro es información para el front, no parte del
veredicto.

**Cuántos instalar.** Un runner corre una sola cosa por vez. Con uno, seis
personas pushean y hacen fila de a una. Con `rapido` en los push la carga media la
cubren dos; el que decide es el `completo` del PR, que ocupa un runner hasta 600
segundos: con dos, esa corrida deja uno solo para las otras cinco personas. Con
tres quedan dos libres, que es la capacidad de régimen. Por eso son tres.

### 2. La rama `dev` y el branch protection

**Es lo único que falta.** Es lo que hace que el gate frene algo: sin esto informa
y nada más.

```bash
git checkout main
git checkout -b dev
git push -u origin dev
```

Después, en **Settings → Branches → Add branch protection rule**:

- Branch name pattern: `dev`
- **Require a pull request before merging**
- **Require status checks to pass before merging** → elegí el check `qa`

El check `qa` recién aparece en esa lista después de que el workflow corrió al
menos una vez. Ya corrió, así que está disponible.

### 3. El front

Corre en `/opt/TP-Pipelines/front` y se ve en **`http://186.182.86.167`**, con usuario
`tpi`. La contraseña está en `CREDENCIALES.txt`, en esa misma carpeta.

El contenedor bindea solo `127.0.0.1:8099`; nginx hace de proxy con basic auth y es la
única puerta. Escucha en el 80 —el que el router reenvía desde internet— y también en
el 8088 de la LAN, para túneles SSH.

> **El 80 sale a internet a sabiendas.** La página es alcanzable por cualquiera que
> escanee esa IP, y como HTTP va en texto plano la contraseña del basic auth viaja sin
> cifrar. Se eligió así para poder mostrarlo sin depender de un dominio. Para cerrarlo
> hace falta un registro DNS apuntando al server y el 443 reenviado: con eso entra
> Let's Encrypt y queda con certificado válido.
>
> La alternativa cifrada, sin exponer nada, es un túnel:
> `ssh -N -L 8088:192.168.10.102:8088 -p 2222 usuario@186.182.86.167` y abrir
> `http://localhost:8088`.

Ver [`tools/ci-front/README.md`](../ci-front/README.md). Es de solo lectura y no
ejecuta nada: si se cae, el CI sigue funcionando igual. Acordate de `CI_RUNNERS`,
que tiene que coincidir con cuántos runners instalaste en el paso 1.

### Lo que aparece cuando el CI corre en Linux de verdad

Cuatro cosas que en Windows no se ven y rompieron las primeras corridas. Están
arregladas, pero conviene saber que existen:

- **El bit de ejecución.** `qa.sh` estaba en `100644`. Git Bash lo ignora, Linux
  no: `Permission denied` antes de empezar. Se arregla con
  `git update-index --chmod=+x`.
- **El contenedor escribe como root.** Deja `.qa/`, los `__pycache__` y los
  `target/` de Maven con dueño root dentro del working tree, y el checkout de la
  corrida siguiente no puede borrarlos: el CI se rompe solo cada dos corridas. Por
  eso `qa.sh` corre con `--user` en Linux.
- **`GITHUB_STEP_SUMMARY` es una ruta del host**, fuera de `/work`. Sin montarla,
  `reportar.py` moría al final y la corrida quedaba en rojo aunque el gate hubiera
  pasado sin un hallazgo.
- **El workspace del runner sobrevive entre corridas.** No es un contenedor
  limpio: lo que ensucies queda para la próxima.

## Pendientes

- `owned-paths.txt` incluye todo `docs/`. Cuando entre contenido de otros equipos
  al repo, **no** lo agregues: lo que no está listado nunca bloquea.
- Falta la rama `dev` y el branch protection del paso 2. Hasta entonces el gate
  informa pero no bloquea ningún merge.
- El front muestra datos de prueba hasta que se le cargue `CI_TOKEN` en
  `/opt/TP-Pipelines/front/.env`.
- ArchUnit, cuando existan los 8 módulos: convierte las reglas de arquitectura de
  [`docs/02`](../../docs/02-arquitectura-y-stack.md) en tests que fallan.
