#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -f "$ROOT/.env" ]]; then
    set -a
    . "$ROOT/.env"
    set +a
fi

COUNT=5
DUPLICATE=false
REMOVED=false
for arg in "$@"; do
    case "$arg" in
        --duplicate) DUPLICATE=true ;;
        --removed) REMOVED=true ;;
        *[!0-9]* | '') echo "Unknown argument: $arg" >&2; exit 1 ;;
        *) COUNT="$arg" ;;
    esac
done

API_URL="${API_URL:-http://localhost:8080}"
DELAY_SECONDS="${DELAY_SECONDS:-0}"
ACCOUNT="shawarma"

if [[ -z "${POSTER_APPLICATION_SECRET:-}" ]]; then
    echo "POSTER_APPLICATION_SECRET is not set: export it or copy .env.example to .env" >&2
    exit 1
fi

webhook_body() {
    local object_id=$1 action=$2 sent_at verify
    sent_at=$(date +%s)
    verify=$(printf '%s' "$ACCOUNT;transaction;$object_id;$action;$sent_at;$POSTER_APPLICATION_SECRET" \
        | md5sum | cut -d' ' -f1)
    printf '{"account":"%s","object":"transaction","object_id":%s,"action":"%s",' \
        "$ACCOUNT" "$object_id" "$action"
    printf '"time":"%s","verify":"%s"}' "$sent_at" "$verify"
}

send_webhook() {
    local label=$1 body=$2 status
    status=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$API_URL/api/webhooks/poster" \
        -H 'Content-Type: application/json' \
        -d "$body")
    echo "$label -> HTTP $status"
}

last_id=""
last_body=""
for ((n = 1; n <= COUNT; n++)); do
    last_id=$(date +%s%N | cut -c1-16)
    last_body=$(webhook_body "$last_id" changed)
    send_webhook "[$n/$COUNT] transaction $last_id changed" "$last_body"
    if ((n < COUNT)); then
        sleep "$DELAY_SECONDS"
    fi
done

if [[ "$DUPLICATE" == true && -n "$last_body" ]]; then
    send_webhook "[duplicate] resend of transaction $last_id" "$last_body"
fi

if [[ "$REMOVED" == true && -n "$last_id" ]]; then
    send_webhook "[removed] transaction $last_id removed" "$(webhook_body "$last_id" removed)"
fi
