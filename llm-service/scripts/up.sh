#!/usr/bin/env bash
# Levanta la combinación correcta de archivos compose.*.yaml para un perfil dado, para no tener
# que recordar qué -f va con qué. Uso:
#
#   scripts/up.sh <perfil> [perfil...] [-- <args extra de "docker compose up">]
#
# Perfiles de la familia local (combinables entre sí, todos parten de compose.yaml):
#   local       compose.yaml solo (backend + Postgres + Kafka local, sin frontend ni mocks)
#   workbench   + compose.workbench.yaml (frontend Angular con hot reload, gateway-mock, courses-mock)
#   debug       + compose.debug.yaml (publica 5432/8086/8087 al host)
#   groq        + compose.groq.yaml (reenvía GROQ_API_KEY: usa el proveedor real, no el fake)
#
# Perfiles de despliegue en la mesh Tailscale (excluyentes entre sí, cada uno su propio stack):
#   mesh        compose.mesh.yaml (build local, se une a la mesh)
#   server      compose.server.yaml (pull de imagen publicada, para correr en un servidor)
#   shadow      compose.shadow.yaml (instancia sombra aislada, project "llm-shadow")
#
# Ejemplos:
#   scripts/up.sh workbench
#   scripts/up.sh workbench debug
#   scripts/up.sh workbench debug groq -- -d
#   scripts/up.sh mesh
#   scripts/up.sh shadow -- -d
set -euo pipefail
cd "$(dirname "$0")/.."

usage() { sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'; }

if [[ $# -eq 0 || "$1" == "-h" || "$1" == "--help" ]]; then
  usage
  exit "${1:+0}"
fi

profiles=()
extra_args=()
seen_separator=false
for arg in "$@"; do
  if [[ "$arg" == "--" ]]; then
    seen_separator=true
    continue
  fi
  if $seen_separator; then
    extra_args+=("$arg")
  else
    profiles+=("$arg")
  fi
done

local_files=()
mesh_profile=""
project_name=""

has_local=false
for p in "${profiles[@]}"; do
  case "$p" in
    local)
      has_local=true
      ;;
    workbench)
      has_local=true
      local_files+=("compose.workbench.yaml")
      ;;
    debug)
      has_local=true
      local_files+=("compose.debug.yaml")
      ;;
    groq)
      has_local=true
      local_files+=("compose.groq.yaml")
      ;;
    mesh|server|shadow)
      if [[ -n "$mesh_profile" ]]; then
        echo "Error: '$p' no se puede combinar con '$mesh_profile' (son stacks completos separados, no overlays)." >&2
        exit 1
      fi
      mesh_profile="$p"
      ;;
    *)
      echo "Error: perfil desconocido '$p'." >&2
      usage
      exit 1
      ;;
  esac
done

if $has_local && [[ -n "$mesh_profile" ]]; then
  echo "Error: los perfiles locales (local/workbench/debug/groq) no se combinan con '$mesh_profile'." >&2
  exit 1
fi

if $has_local; then
  # compose.yaml exige LLM_CREDENTIALS_MASTER_KEY sin default (services.llm-service.environment)
  # y la red externa "tpi-platform" (networks.tpi-platform.external: true).
  if [[ -z "${LLM_CREDENTIALS_MASTER_KEY:-}" ]] && ! grep -q '^LLM_CREDENTIALS_MASTER_KEY=' .env 2>/dev/null; then
    echo "Error: falta LLM_CREDENTIALS_MASTER_KEY (variable de entorno o en llm-service/.env)." >&2
    echo "Ver .env.example." >&2
    exit 1
  fi
  if ! docker network inspect tpi-platform >/dev/null 2>&1; then
    echo "Creando la red externa 'tpi-platform' (solo hace falta una vez por máquina)..."
    docker network create tpi-platform
  fi

  files=(-f compose.yaml)
  for f in "${local_files[@]+"${local_files[@]}"}"; do
    files+=(-f "$f")
  done
  echo "+ docker compose ${files[*]} up --build ${extra_args[*]:-}"
  exec docker compose "${files[@]}" up --build "${extra_args[@]+"${extra_args[@]}"}"
fi

case "$mesh_profile" in
  mesh)
    for v in TS_AUTHKEY LLM_CREDENTIALS_MASTER_KEY; do
      if [[ -z "${!v:-}" ]] && ! grep -q "^${v}=" .env 2>/dev/null; then
        echo "Error: falta $v (variable de entorno o en llm-service/.env)." >&2
        exit 1
      fi
    done
    [[ -f resolv.conf ]] || echo "Aviso: no se encontró llm-service/resolv.conf (lo pide compose.mesh.yaml)." >&2
    echo "Aviso: no correr junto con el perfil 'server' — ambos registran la misma instancia 'llm-service' en Eureka." >&2
    echo "+ docker compose -f compose.mesh.yaml up --build ${extra_args[*]:-}"
    exec docker compose -f compose.mesh.yaml up --build "${extra_args[@]+"${extra_args[@]}"}"
    ;;
  server)
    for v in TS_AUTHKEY LLM_CREDENTIALS_MASTER_KEY; do
      if [[ -z "${!v:-}" ]] && ! grep -q "^${v}=" .env 2>/dev/null; then
        echo "Error: falta $v (variable de entorno o en llm-service/.env)." >&2
        exit 1
      fi
    done
    [[ -f resolv.conf ]] || echo "Aviso: no se encontró llm-service/resolv.conf (lo pide compose.server.yaml)." >&2
    echo "Aviso: no correr junto con el perfil 'mesh' — ambos registran la misma instancia 'llm-service' en Eureka." >&2
    echo "+ docker compose -f compose.server.yaml pull && ... up -d"
    docker compose -f compose.server.yaml pull
    exec docker compose -f compose.server.yaml up "${extra_args[@]+"${extra_args[@]}"}"
    ;;
  shadow)
    if [[ -z "${TS_AUTHKEY_SHADOW:-}${TS_AUTHKEY:-}" ]] && ! grep -qE '^TS_AUTHKEY(_SHADOW)?=' .env 2>/dev/null; then
      echo "Error: falta TS_AUTHKEY_SHADOW (o TS_AUTHKEY) (variable de entorno o en llm-service/.env)." >&2
      exit 1
    fi
    if [[ -z "${LLM_CREDENTIALS_MASTER_KEY:-}" ]] && ! grep -q '^LLM_CREDENTIALS_MASTER_KEY=' .env 2>/dev/null; then
      echo "Error: falta LLM_CREDENTIALS_MASTER_KEY (variable de entorno o en llm-service/.env)." >&2
      exit 1
    fi
    project_name="llm-shadow"
    echo "+ docker compose -p $project_name -f compose.shadow.yaml up --build ${extra_args[*]:-}"
    exec docker compose -p "$project_name" -f compose.shadow.yaml up --build "${extra_args[@]+"${extra_args[@]}"}"
    ;;
esac
