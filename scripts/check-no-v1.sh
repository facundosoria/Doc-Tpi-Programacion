#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

# Historical migrations and documentation are intentionally outside the production scan.
patterns=(
  'TeacherStore'
  'TeacherComponent'
  'GoldenSetApiService'
  'WorkbenchComponent'
  '/api/llm/golden-sets'
  'class GoldenSetController\\b'
  'class GoldenSetService\\b'
  'class GoldenSetRepository\\b'
  'llm-workbench\.teacher\.v1'
  'STORAGE_KEY'
)

if rg -n -e "$(IFS='|'; echo "${patterns[*]}")" \
  llm-workbench/src llm-service/src/main \
  --glob '!resources/db/migration/V1__schema_and_rubric.sql'; then
  echo 'Se detectaron referencias productivas prohibidas de V1.' >&2
  exit 1
fi
