#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT/deploy/docker/docker-compose.yml"
ENV_FILE="$ROOT/.env"
ENV_EXAMPLE="$ROOT/.env.example"
ACTION="${1:-up}"

if [[ $# -gt 0 ]]; then
  shift
fi

SKIP_BUILD=0
FOLLOW=0
SERVICES=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build) SKIP_BUILD=1 ;;
    --follow|-f) FOLLOW=1 ;;
    *) SERVICES+=("$1") ;;
  esac
  shift
done

case "$ACTION" in
  init|validate|build|up|down|restart|status|logs) ;;
  *)
    echo "Uso: $0 {init|validate|build|up|down|restart|status|logs} [--skip-build] [--follow] [servicos...]" >&2
    exit 2
    ;;
esac

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

build_images() {
  compose build migrate login-server game-server
}

start_stack() {
  if [[ "$SKIP_BUILD" -eq 0 ]]; then
    build_images
  fi
  compose up -d --wait --remove-orphans
  compose ps
}

cd "$ROOT"

if [[ "$ACTION" == "init" ]]; then
  if [[ -f "$ENV_FILE" ]]; then
    echo ".env ja existe; nenhuma alteracao foi feita."
  else
    cp "$ENV_EXAMPLE" "$ENV_FILE"
    echo ".env criado a partir de .env.example. Ajuste senhas e identidade do GameServer antes de usar um ambiente compartilhado."
  fi
  exit 0
fi

command -v docker >/dev/null 2>&1 || { echo "Docker nao foi encontrado no PATH." >&2; exit 1; }
docker info --format '{{.ServerVersion}}' >/dev/null
[[ -f "$ENV_FILE" ]] || { echo ".env nao encontrado. Execute './StartL2NewEra.sh init'." >&2; exit 1; }

case "$ACTION" in
  validate)
    compose config --quiet
    ./gradlew --no-daemon --no-parallel checkRuntimeScripts
    ;;
  build)
    compose config --quiet
    build_images
    ;;
  up)
    compose config --quiet
    start_stack
    ;;
  down)
    compose down --remove-orphans
    ;;
  restart)
    compose down --remove-orphans
    start_stack
    ;;
  status)
    compose ps
    ;;
  logs)
    LOG_ARGS=(logs --tail 200)
    [[ "$FOLLOW" -eq 1 ]] && LOG_ARGS+=(--follow)
    if [[ ${#SERVICES[@]} -eq 0 ]]; then
      SERVICES=(migrate login-server game-server)
    fi
    compose "${LOG_ARGS[@]}" "${SERVICES[@]}"
    ;;
esac
