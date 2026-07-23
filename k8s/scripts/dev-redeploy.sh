#!/usr/bin/env bash
# 로컬 개발 inner-loop — 코드 수정 후 "이미지 재빌드 → kind 주입 → 파드 롤아웃"을 한 번에.
#
# 인프라(kind/Kafka/DB/ingress)와 앱이 이미 떠 있는 상태 전용이다. 매니페스트(YAML)가 아니라
# 이미지 내용만 바뀌는 경우, `kubectl apply -k`는 스펙이 그대로라 파드를 재생성하지 않는다 —
# 그래서 rollout restart로 파드를 강제 교체해 새 :latest 이미지를 물게 한다.
# (최초 세팅/인프라 설치는 k8s/README.md "로컬 배포 (전체 순서)"를 따른다.)
#
# 사용법:
#   k8s/scripts/dev-redeploy.sh                         # 전체 서비스 재빌드+주입+롤아웃
#   k8s/scripts/dev-redeploy.sh order-api shop-api      # 지정 서비스만 (바뀐 것만 → 빠름)
#
# 대상 서비스 목록은 build-and-load.sh와 동일하게 overlays/local/kustomization.yaml의
# images.newName을 단일 출처로 읽는다 — 서비스 추가/제거 시 그 파일만 고치면 된다.
set -euo pipefail
cd "$(dirname "$0")/../.."

NAMESPACE=mini-commerce
CLUSTER=mini-commerce
KUSTOMIZATION="k8s/overlays/local/kustomization.yaml"

# kustomization의 newName(mini-commerce-<svc>)에서 서비스 이름을 뽑는다.
ALL_SERVICES=$(grep '    newName:' "$KUSTOMIZATION" | awk '{print $2}' | sed 's/^mini-commerce-//')

# 인자가 있으면 그 서비스만, 없으면 전체. 인자는 알려진 서비스인지 검증한다.
if [ "$#" -gt 0 ]; then
  targets=()
  for svc in "$@"; do
    if ! echo "$ALL_SERVICES" | grep -qx "$svc"; then
      echo "알 수 없는 서비스: $svc" >&2
      echo "가능한 값: $(echo "$ALL_SERVICES" | tr '\n' ' ')" >&2
      exit 1
    fi
    targets+=("$svc")
  done
else
  # shellcheck disable=SC2206
  targets=($ALL_SERVICES)
fi

echo "==> 대상: ${targets[*]}"

# 1) 이미지 빌드(compose가 빌드 계약 소유) + kind 주입
echo "==> 이미지 빌드"
docker compose build "${targets[@]}"
for svc in "${targets[@]}"; do
  echo "==> kind load: mini-commerce-$svc:latest"
  kind load docker-image "mini-commerce-$svc:latest" --name "$CLUSTER"
done

# rollout restart + status 헬퍼
restart() { kubectl rollout restart "deploy/$1" -n "$NAMESPACE"; }
wait_ready() { kubectl rollout status "deploy/$1" -n "$NAMESPACE" --timeout=300s; }

# 2) order-api가 대상에 있으면 먼저 롤아웃한다 — Flyway가 orderdb 스키마를 소유하므로,
#    validate-only인 order-admin/order-batch보다 반드시 먼저 떠서 마이그레이션을 적용해야 한다.
rest=()
if printf '%s\n' "${targets[@]}" | grep -qx "order-api"; then
  echo "==> order-api 먼저 롤아웃 (Flyway 마이그레이션 선행)"
  restart order-api
  wait_ready order-api
  for svc in "${targets[@]}"; do
    [ "$svc" = "order-api" ] || rest+=("$svc")
  done
else
  rest=("${targets[@]}")
fi

# 3) 나머지 서비스 롤아웃 — 재시작을 먼저 다 걸고 나서 한꺼번에 준비 대기.
if [ "${#rest[@]}" -gt 0 ]; then
  echo "==> 롤아웃: ${rest[*]}"
  for svc in "${rest[@]}"; do restart "$svc"; done
  for svc in "${rest[@]}"; do wait_ready "$svc"; done
fi

echo "==> 완료. 상태:"
kubectl get pods -n "$NAMESPACE" -o wide
