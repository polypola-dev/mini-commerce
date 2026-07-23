#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v k6 >/dev/null 2>&1; then
    echo "k6가 설치돼 있지 않습니다. brew install k6 로 설치하세요." >&2
    exit 1
fi

set -a
source .env
set +a

k6 run \
    -e SUPABASE_URL="${SUPABASE_URL:-https://wzdifkwupleogfixibiv.supabase.co}" \
    -e SUPABASE_SERVICE_ROLE_KEY="${SUPABASE_SERVICE_ROLE_KEY:?SUPABASE_SERVICE_ROLE_KEY missing in .env}" \
    -e BFF_SECRET_KEY="${BFF_SECRET_KEY:?BFF_SECRET_KEY missing in .env}" \
    -e INTERNAL_API_KEY="${INTERNAL_API_KEY:?INTERNAL_API_KEY missing in .env}" \
    "$@" \
    loadtest/reserve-concurrency.js
