#!/usr/bin/env bash
# Baja la combinación correcta de archivos compose.*.yaml para un perfil dado — contraparte de
# scripts/up.sh. Usa los MISMOS nombres de perfil, así no hay que recordar qué -f corresponde a
# qué stack ni qué -p (project name) usó cada uno al levantarlo.
#
#   scripts/down.sh <perfil> [perfil...] [-- <args extra de "docker compose down">]
#
# Perfiles: local, workbench, debug, groq (combinables, familia compose.yaml)
#           mesh, server, shadow (excluyentes entre sí, stacks propios)
#
# Ejemplos:
#   scripts/down.sh workbench debug
#   scripts/down.sh shadow -- --volumes
set -euo pipefail
cd "$(dirname "$0")/.."

usage() { sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'; }

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
has_local=false
for p in "${profiles[@]}"; do
  case "$p" in
    local) has_local=true ;;
    workbench) has_local=true; local_files+=("compose.workbench.yaml") ;;
    debug) has_local=true; local_files+=("compose.debug.yaml") ;;
    groq) has_local=true; local_files+=("compose.groq.yaml") ;;
    mesh|server|shadow)
      if [[ -n "$mesh_profile" ]]; then
        echo "Error: '$p' no se puede combinar con '$mesh_profile'." >&2
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
  echo "Error: los perfiles locales no se combinan con '$mesh_profile'." >&2
  exit 1
fi

if $has_local; then
  files=(-f compose.yaml)
  for f in "${local_files[@]+"${local_files[@]}"}"; do
    files+=(-f "$f")
  done
  echo "+ docker compose ${files[*]} down ${extra_args[*]:-}"
  exec docker compose "${files[@]}" down "${extra_args[@]+"${extra_args[@]}"}"
fi

case "$mesh_profile" in
  mesh)
    echo "+ docker compose -f compose.mesh.yaml down ${extra_args[*]:-}"
    exec docker compose -f compose.mesh.yaml down "${extra_args[@]+"${extra_args[@]}"}"
    ;;
  server)
    echo "+ docker compose -f compose.server.yaml down ${extra_args[*]:-}"
    exec docker compose -f compose.server.yaml down "${extra_args[@]+"${extra_args[@]}"}"
    ;;
  shadow)
    echo "+ docker compose -p llm-shadow -f compose.shadow.yaml down ${extra_args[*]:-}"
    exec docker compose -p llm-shadow -f compose.shadow.yaml down "${extra_args[@]+"${extra_args[@]}"}"
    ;;
esac
