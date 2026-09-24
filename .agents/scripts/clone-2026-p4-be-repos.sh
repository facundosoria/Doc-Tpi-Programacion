#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <destination-directory>" >&2
  exit 64
fi

destination_root=$1

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) is required and must be authenticated." >&2
  exit 69
fi

if ! command -v git >/dev/null 2>&1; then
  echo "git is required." >&2
  exit 69
fi

if [[ -e "$destination_root" && ! -d "$destination_root" ]]; then
  echo "Destination exists but is not a directory: $destination_root" >&2
  exit 73
fi

mkdir -p "$destination_root"

gh repo list 2026-P4-BE --limit 100 --json name,sshUrl --jq '.[] | [.name, .sshUrl] | @tsv' |
while IFS=$'\t' read -r repository_name repository_url; do
  repository_path="$destination_root/$repository_name"

  if [[ -e "$repository_path" ]]; then
    echo "Skipping existing path: $repository_path"
    continue
  fi

  echo "Cloning $repository_name"
  git clone "$repository_url" "$repository_path"
done
