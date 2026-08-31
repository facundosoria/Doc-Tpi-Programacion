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
```

La primera corrida construye la imagen (unos minutos). Después arranca en un
segundo, porque el tag de la imagen es el hash del `Dockerfile`: solo se
reconstruye cuando alguien lo cambia.

El resumen queda en `.qa/resumen.md`, que es **el mismo Markdown** que GitHub
renderiza en la página del run. Si querés ver antes de pushear lo que va a
mostrar el CI, abrí ese archivo.

## Dónde está el control

El gate **no bloquea el push**: no hay hook de git. Corre cuando vos querés, y el
workflow corre solo al pushear.

| Momento | Qué pasa |
|---|---|
| Antes de pushear | `./qa.sh` — tu red de seguridad, voluntaria |
| Al pushear a tu rama | El workflow corre y marca el commit |
| Al mergear el PR a `dev` | Branch protection bloquea el merge si está en rojo |

Por eso se trabaja con ramas y pull requests: **el workflow corre después del
push**, así que si pusheás derecho a `dev`, el código roto ya entró.

## Los niveles

Todo se configura en [`config/checks.yml`](config/checks.yml):

- **`bloquea`** — el hallazgo hace fallar la corrida
- **`avisa`** — se reporta y la corrida sigue
- **`arregla`** — solo formato: corrige en tu working tree en vez de protestar
- **`off`** — no se ejecuta

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
existen `docs/` o `ms-evaluacion-llm/` (eso vive en la config). Si algún día otro
equipo lo necesita, extraerlo es mover la carpeta.

## Alcance: solo lo que tu cambio rompe

La deuda vieja no bloquea a nadie; se paga cuando se toca el archivo.

Con una excepción deliberada: **links y referencias corren sobre todo el repo**,
comparando contra el merge-base. Si renombrás un título en tu documento, rompés un
ancla en un documento que no tocaste — y ese es tu cambio, aunque el archivo roto
no aparezca en tu diff. Lo que ya estaba roto se informa sin frenar.

## Pendientes

- `owned-paths.txt` incluye todo `docs/`. Cuando entre contenido de otros equipos
  al repo, **no** lo agregues: lo que no está listado nunca bloquea.
- Falta crear la rama `dev` y configurarle branch protection.
- El runner self-hosted necesita acceso a Docker, y eso equivale a acceso root en
  ese host. Conviene una máquina dedicada.
- ArchUnit, cuando existan los 8 módulos: convierte las reglas de arquitectura de
  [`docs/02`](../../docs/02-arquitectura-y-stack.md) en tests que fallan.
