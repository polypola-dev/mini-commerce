#!/usr/bin/env bash
# 로컬 앱 이미지 빌드(compose) + kind 클러스터 주입.
# 이미지 목록은 k8s/overlays/local/kustomization.yaml의 images.newName을 단일 출처로 읽는다 —
# 서비스 추가/제거 시 그 파일만 고치면 이 스크립트가 자동으로 따라간다(k8s/README.md 참고).
set -euo pipefail
cd "$(dirname "$0")/../.."

kustomization="k8s/overlays/local/kustomization.yaml"
images=$(grep '    newName:' "$kustomization" | awk '{print $2}')
services=$(echo "$images" | sed 's/^mini-commerce-//')

echo "빌드 대상: $(echo "$services" | tr '\n' ' ')"
docker compose build $services

for img in $images; do
  echo "kind load: $img:latest"
  kind load docker-image "$img:latest" --name mini-commerce
done
