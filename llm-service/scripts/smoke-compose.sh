#!/usr/bin/env bash
set -euo pipefail

project_name="llm-s1-smoke"
cleanup() { docker compose -p "$project_name" down --volumes --remove-orphans; }
trap cleanup EXIT

mvn -q -DskipTests package
docker compose -p "$project_name" up --build --wait
docker compose -p "$project_name" exec -T llm-service wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"'
echo "Docker Compose S1 smoke test: OK"
