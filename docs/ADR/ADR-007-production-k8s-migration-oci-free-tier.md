# ADR-007: 운영 환경을 k8s로 이전 — Oracle Cloud Always Free 기반

- 상태: 승인 (2026-07-06)

## 컨텍스트

로드맵 재검토(2026-07-05, ROADMAP.md I1/I2)에서 **현재 코드가 운영에 배포 불가능한 상태**임이
확인됐다:

- `render.yaml`이 MSA 분리 이전의 단일 서비스(`mini-commerce-backend`, 사실상 shop-api만) 정의
  그대로다. 로컬 docker-compose에는 shop-api / order-api / order-admin / order-batch + Kafka가
  전부 기동하지만, 운영(Render)에는 order 계열 서비스와 Kafka가 존재하지 않는다.
- 프론트 `.env.example`이 요구하는 `ORDER_SERVICE_URL` / `ORDER_ADMIN_SERVICE_URL`에 대응할
  운영 서비스가 없다.
- Render 프리티어에는 Kafka + 4개 서비스를 올릴 수 없어, 현행 인프라의 연장선으로는 MSA 구조를
  운영에 반영할 방법이 없다.

동시에 이 프로젝트의 명시적 학습 목표에 k8s 전환(로드맵 F/G/H 트랙)이 있다. 선택지는 세 가지였다:
(a) 현행 유지 + k8s 보류, (b) 하이브리드(운영은 현행 $0 유지, k8s는 로컬 학습 전용),
(c) 운영 자체를 k8s로 이전.

## 결정

**(c) 운영을 k8s로 이전한다. 플랫폼은 Oracle Cloud Always Free 티어.**

1. **플랫폼: OCI Always Free** — Ampere A1.Flex ARM 합계 4 OCPU / 24GB RAM / 200GB 블록볼륨
   한도 내에서 운영한다. 목표 비용 **월 $0 유지**.
2. **클러스터 형태: OKE(Oracle Container Engine) Basic 클러스터 우선** — 컨트롤플레인 무료,
   워커노드는 A1.Flex 무료 한도로 구성. A1 용량 확보 실패(프리티어 용량 경쟁)가 잦으면
   A1 VM 직결 **k3s로 폴백**한다. 둘 다 막히면 소액 PAYG 전환 여부를 별도 재결정.
3. **역할 분담 유지**: DB는 Supabase(운영 Postgres를 k8s에서 직접 운영하지 않음 — G4 선결정),
   Redis는 Upstash, 프론트엔드+BFF는 Vercel 유지(I4 선결정). k8s로 옮기는 것은
   **백엔드 4개 서비스 + Kafka + (필요시) 관측성 스택**이다.
4. **Kafka는 클러스터 내 단일 브로커(KRaft)로 시작** — 프리티어 리소스 한도 고려.
   Strimzi Operator vs 단순 StatefulSet은 G5에서 결정.
5. **Render는 이전 완료 후 폐기**한다. 이전 완료 전까지 운영 배포는 동결(현 운영은 MSA 이전
   구버전 shop-api 단독 상태로 유지)하고, `render.yaml`은 이전 완료 시점에 제거.
6. **ARM 제약이 전체 빌드 파이프라인에 걸린다**: 모든 컨테이너 이미지는 **arm64 필수**
   (개발 머신 Apple Silicon이라 로컬 빌드는 자연 호환, CI가 amd64 러너면 buildx/QEMU 또는
   ARM 러너 필요). E4(이미지 파이프라인)/E5(Dockerfile 최적화)에 multi-arch 요구가 P0급으로
   상향된다.
7. **로컬 학습 클러스터(kind, G1)는 별도로 유지**하고, Kustomize overlays(G2)를
   `local`(kind) / `prod`(OKE)로 분리해 매니페스트를 공유한다.

## 대안

- **(a) 현행 유지 + k8s 보류**: 비용 $0 확실. 그러나 MSA 분리·Kafka 이벤트 아키텍처가 영원히
  로컬 전용으로 남고 I1(배포 불능)이 방치됨 — "정식 서비스 표방"과 정면 충돌. 기각.
- **(b) 하이브리드(운영 현행, k8s는 로컬 학습)**: 비용 $0에 학습도 가능하나, Render에 올릴 수
  있는 것이 shop-api 단독뿐이라 운영과 코드의 괴리가 구조적으로 고착됨. 운영이 걸리지 않은
  k8s 학습은 probe/graceful shutdown/Secret 관리 같은 운영 규율의 긴장감이 떨어짐. 기각.
- **DigitalOcean DOKS / GKE·EKS**: 관리형 경험·이력서 가치는 높으나 월 $36~100+ 고정 지출.
  사이드 프로젝트 지속성(비용 부담으로 인한 중도 포기 리스크)을 우선해 기각. OCI 무료 한도가
  4 OCPU/24GB로 오히려 넉넉한 편.

## 결과

- **G계열(k8s 인프라)이 "학습"이 아니라 "운영 목표"가 된다** — F계열(앱 k8s-ready) 완료 없이
  G계열 착수 금지 원칙은 그대로 유지.
- 착수 순서(ROADMAP)는 불변: E1(CI) → D1/F3(Flyway) → B1/B2(RLS) → B3(internal 인증) →
  A6/F1(기동 결합 제거) → F2(probe) → F계열 잔여 → G계열.
- **리소스 예산 설계 필요**: 4 OCPU/24GB에 Spring 서비스 4개 + Kafka를 배치해야 하므로
  F6(JVM 튜닝, MaxRAMPercentage/requests/limits)이 실측 기반으로 중요해짐. 관측성 스택
  (Tempo/Loki/Prometheus/Grafana)을 클러스터에 동거시킬지, Grafana Cloud 프리티어로 뺄지는
  H계열에서 결정.
- **리스크**: OCI A1 용량 확보 실패(가입 리전 선택 중요), Always Free 계정 회수 정책 변동.
  폴백 경로(k3s → PAYG 소액)를 결정에 포함해 둠.
- 프론트(Vercel BFF) → 백엔드(OCI) 네트워크 경로가 리전 간 왕복이 되므로, 리전 선택 시
  Vercel 함수 리전(icn1 등)과의 지연을 고려한다.

## 참고

- ROADMAP.md — I1(배포 구조 괴리), I2(이 결정), F/G/H 트랙
- [[ADR-005-order-msa-service-group-kafka]] — MSA 서비스그룹 구조(이전 대상 워크로드)
- [[ADR-004-order-hexagonal-multimodule-msa]] — 멀티모듈 구조
- GitHub 이슈: #9(운영 k8s 이전 에픽, 생성 예정)
