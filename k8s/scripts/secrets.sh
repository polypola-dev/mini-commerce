#!/usr/bin/env bash
# Secret `app-secrets` 관리 (G8, ADR-013)
#
#   k8s/scripts/secrets.sh apply         .env → Secret 즉시 적용 (로컬 일상 흐름)
#   k8s/scripts/secrets.sh seal          .env → k8s/secrets/app-secrets.enc.yaml 재생성 (커밋용)
#   k8s/scripts/secrets.sh apply-sealed  enc 파일 복호화 → 적용 (.env 없는 환경/재해 복구)
#
# 원천은 .env 하나다 — enc 파일은 `seal`이 만드는 파생물이며 수동 편집 금지.
# 평문 매니페스트는 디스크에 남기지 않는다(mktemp + trap 삭제).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENC_FILE="$REPO_ROOT/k8s/secrets/app-secrets.enc.yaml"
NAMESPACE=mini-commerce

# macOS의 sops 기본 키 경로는 ~/Library/Application Support/... — 관례 경로를 명시한다.
# CI/다른 머신에서는 이 변수를 밖에서 주입하면 그대로 존중된다.
export SOPS_AGE_KEY_FILE="${SOPS_AGE_KEY_FILE:-$HOME/.config/sops/age/keys.txt}"

render_manifest() {
  # .env의 비밀 + 로컬 고정값(DB/Redis — compose·k8s 공통 계약)을 합쳐 stringData로 렌더
  set -a; source "$REPO_ROOT/.env"; set +a
  : "${BFF_SECRET_KEY:?.env에 BFF_SECRET_KEY가 없습니다}"
  : "${SUPABASE_SERVICE_ROLE_KEY:?.env에 SUPABASE_SERVICE_ROLE_KEY가 없습니다}"
  : "${TOSS_SECRET_KEY:?.env에 TOSS_SECRET_KEY가 없습니다}"
  cat <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: $NAMESPACE
  labels:
    app.kubernetes.io/part-of: mini-commerce
type: Opaque
stringData:
  DATABASE_USERNAME: minicommerce
  DATABASE_PASSWORD: minicommerce
  REDIS_PASSWORD: ""
  BFF_SECRET_KEY: "$BFF_SECRET_KEY"
  SUPABASE_SERVICE_ROLE_KEY: "$SUPABASE_SERVICE_ROLE_KEY"
  TOSS_SECRET_KEY: "$TOSS_SECRET_KEY"
EOF
}

case "${1:-}" in
  apply)
    tmp="$(mktemp)"; trap 'rm -f "$tmp"' EXIT
    render_manifest > "$tmp"
    kubectl apply -f "$tmp"
    ;;
  seal)
    command -v sops >/dev/null || { echo "sops가 없습니다: brew install sops age" >&2; exit 1; }
    mkdir -p "$(dirname "$ENC_FILE")"
    tmp="$(mktemp)"; trap 'rm -f "$tmp"' EXIT
    render_manifest > "$tmp"
    # mktemp 경로는 .sops.yaml의 path_regex와 안 맞으므로 대상 경로로 규칙 매칭
    sops --encrypt --filename-override "$ENC_FILE" "$tmp" > "$ENC_FILE"
    echo "재생성됨: ${ENC_FILE#$REPO_ROOT/} (커밋 대상)"
    ;;
  apply-sealed)
    command -v sops >/dev/null || { echo "sops가 없습니다: brew install sops age" >&2; exit 1; }
    sops --decrypt "$ENC_FILE" | kubectl apply -f -
    ;;
  *)
    echo "사용법: $0 {apply|seal|apply-sealed}" >&2; exit 1
    ;;
esac
