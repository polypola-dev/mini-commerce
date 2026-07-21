#!/usr/bin/env bash
# Database Client 확장의 "Custom Terminal Command" 같은 pre-connect 훅용.
# 매 연결 시도마다 실행돼도 안전하도록(idempotent) 설계했다:
#   - 이미 localhost:$LOCAL_PORT가 리슨 중이면 아무것도 안 하고 즉시 종료
#   - 없으면 kubectl port-forward를 백그라운드로 띄우고, 실제 리슨될 때까지
#     최대 5초 대기한 뒤 종료
# 훅이 "명령 종료를 기다린 뒤 연결"하든 "그냥 터미널만 띄우고 바로 연결 시도"하든
# 둘 다 커버하기 위해 리슨 확인 후에 exit 하는 구조다.
set -uo pipefail

NAMESPACE="mini-commerce"
SERVICE="svc/postgres"
LOCAL_PORT="${1:-15432}"
REMOTE_PORT=5432
LOG="/tmp/pg-port-forward-${LOCAL_PORT}.log"

is_listening() {
  lsof -i ":${LOCAL_PORT}" -sTCP:LISTEN >/dev/null 2>&1
}

if is_listening; then
  echo "이미 localhost:${LOCAL_PORT} 터널이 떠 있습니다."
  exit 0
fi

echo "터널 없음 — 백그라운드로 시작 (로그: ${LOG})"
nohup kubectl port-forward -n "$NAMESPACE" "$SERVICE" "${LOCAL_PORT}:${REMOTE_PORT}" \
  > "$LOG" 2>&1 &
disown

for _ in $(seq 1 25); do
  is_listening && { echo "터널 준비 완료 — localhost:${LOCAL_PORT}"; exit 0; }
  sleep 0.2
done

echo "5초 내 터널이 뜨지 않았습니다 — 로그 확인: ${LOG}"
exit 1
