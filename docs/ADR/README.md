# ADR 인덱스

이 리포의 아키텍처 의사결정 기록(ADR) 전체 목록. ADR은 **append-only 결정 로그**로 관리한다 —
결정이 바뀌어도 기존 파일의 본문은 고치지 않고, 새 ADR을 작성한 뒤 대체되는 쪽의 상태 줄만
갱신한다(아래 컨벤션 참고). 삭제하지 않는 이유는 "왜 그때 그렇게 결정했는지"가 나중에도
값어치가 있기 때문 — 지금 상태만 보고 싶으면 `backend/doc/ARCHITECTURE.md` /
`backend/doc/CONFIGURATION.md` / `k8s/README.md` 같은 현재 상태 문서를 본다.

| # | 제목 | 상태 |
|---|---|---|
| [ADR-001](ADR-001-architecture-decisions.md) | 초기 아키텍처 결정 — 선택적 헥사고날, 보안/리소스 설계 | 승인 (2026-06-01) |
| [ADR-002](ADR-002-social-login-via-supabase-custom-oidc-provider.md) | 소셜 로그인 확장 — Supabase Custom OIDC Provider | 승인 (2026-06-19) |
| [ADR-003](ADR-003-multi-account-abuse-prevention-dedup-layer.md) | 신규가입 어뷰징 방지 — 본인 식별 Dedup 레이어 | 제안 (2026-06-19) |
| [ADR-004](ADR-004-order-hexagonal-multimodule-msa.md) | order 헥사고날 + 멀티모듈 전환 및 MSA 대비 | 승인 (2026-06-30) — 단일 DB/모듈러 모놀리스 부분은 ADR-005로 대체 |
| [ADR-005](ADR-005-order-msa-service-group-kafka.md) | order MSA 전환 — 서비스그룹(b) + Kafka + 하이브리드 재고 사가 | 승인 (2026-07-02) — 재고 전략 부분은 ADR-019(전략 c)로 대체 |
| [ADR-006](ADR-006-observability-otel-unified-stack.md) | 관측성 3대 축 — OpenTelemetry 공식 스타터 통일 | 승인 (2026-07-04) |
| [ADR-007](ADR-007-production-k8s-migration-oci-free-tier.md) | 운영 환경 k8s 이전 — OCI Always Free | 승인 (2026-07-06) |
| [ADR-008](ADR-008-local-cluster-kind.md) | 로컬 k8s 클러스터 — kind 선택 | 승인 (2026-07-13) |
| [ADR-009](ADR-009-manifest-kustomize.md) | 매니페스트 관리 — Kustomize base/overlays | 승인 (2026-07-13) |
| [ADR-010](ADR-010-local-postgres-redis-statefulset.md) | 로컬 Postgres/Redis — StatefulSet + PVC | 승인 (2026-07-14) |
| [ADR-011](ADR-011-kafka-strimzi.md) | Kafka on k8s — Strimzi Operator | 승인 (2026-07-14) |
| [ADR-012](ADR-012-ingress-nginx.md) | API 단일 진입점 — ingress-nginx 경로 라우팅 | 승인 (2026-07-14) |
| [ADR-013](ADR-013-secret-sops-age.md) | Secret 관리 — .env 원천 + SOPS/age 암호화 커밋 | 승인 (2026-07-14) |
| [ADR-014](ADR-014-network-policy.md) | NetworkPolicy — default-deny(Ingress) + 명시 허용 | 승인 (2026-07-14) |
| [ADR-015](ADR-015-db-init-job.md) | orderdb 초기화 — k8s Job 이관 | 승인 (2026-07-14) |
| [ADR-016](ADR-016-cd-stage1-kind-actions.md) | CD 1단계 — GitHub Actions kind 배포 검증 | 승인 (2026-07-14) |
| [ADR-017](ADR-017-image-pipeline-ghcr.md) | 이미지 파이프라인 — Dockerfile 캐시(E5) + GHCR multi-arch(E4) | 승인 (2026-07-14) |
| [ADR-018](ADR-018-observability-k8s-integration.md) | 관측성 k8s 통합 — kube-prometheus-stack + Tempo/Loki + OTel Collector | 승인 (2026-07-15) |
| [ADR-019](ADR-019-inventory-service-extraction.md) | inventory 완전분리 — 별도 서비스+DB, 하이브리드 분산 사가(전략 c) | 승인 (2026-07-20), 경합 정책 개정 (2026-07-21) |

## 컨벤션

- **상태값**: 제안 / 승인 / 대체됨(→ADR-XXX로 이동) / 폐기
- **번호**: 발행 순서대로 계속 이어서 부여. 재사용하거나 앞자리를 비우지 않는다.
- **결정이 바뀔 때**: 옛 ADR 본문은 그대로 두고 새 ADR을 작성한다. 옛 ADR의 상태 줄에
  "→ ADR-XXX로 대체"만 추가(위 ADR-004/ADR-005 사례 참고). 이 인덱스도 같이 갱신한다.
- **새 ADR 추가 시**: 이 표에 한 줄 추가 + 상태값 기입. 관련/대체 관계가 있으면 상태 칸에 메모.
