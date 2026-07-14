# ADR-013: Secret 관리 — .env 원천 스크립트 + SOPS/age 암호화 커밋

- 상태: 승인 (2026-07-14)
- 관련: ADR-009(Kustomize — Secret 실물은 오버레이에 두지 않음), ROADMAP G8(B4 승계),
  GH #9(k8s 이전 에픽), backend/doc/CONFIGURATION.md(SEC 분류)

## 컨텍스트

Secret `app-secrets`(DATABASE_USERNAME/PASSWORD, REDIS_PASSWORD, BFF_SECRET_KEY,
SUPABASE_SERVICE_ROLE_KEY)를 지금까지 README의 수동 `kubectl create secret` 블록으로
만들어왔다. 필요한 것: (1) 반복 가능한 생성 절차, (2) 평문 커밋 금지를 지키면서
Git으로 관리 가능한 형태(클러스터 재생성·새 머신 복원·G11 CD 대비).

후보: 생성 스크립트만 / + SealedSecrets / + SOPS(age).

## 결정

**`.env`를 단일 원천으로 하는 스크립트(`k8s/scripts/secrets.sh`) + SOPS/age로
암호화한 Secret 매니페스트(`k8s/secrets/app-secrets.enc.yaml`) 커밋**을 채택한다.
SealedSecrets는 배제.

### 흐름

| 명령 | 용도 |
|---|---|
| `secrets.sh apply` | `.env` → Secret 즉시 적용 (로컬 일상 흐름) |
| `secrets.sh seal` | `.env` → enc 파일 재생성 (비밀값 변경 시, 커밋 대상) |
| `secrets.sh apply-sealed` | enc 복호화 → 적용 (.env 없는 환경/재해 복구) |

원천은 `.env` 하나 — enc 파일은 `seal`이 만드는 **파생물**이며 수동 편집 금지.
평문 매니페스트는 디스크에 남기지 않는다(mktemp + trap).

### 근거 (vs SealedSecrets)

- **SealedSecrets 배제 결정타**: 암호화 키가 클러스터 내 컨트롤러에 산다 —
  **kind를 재생성하면 기존 봉인 파일이 전부 무효**(키 백업/복원 절차 필요).
  클러스터를 부담 없이 부수고 다시 만드는 로컬 워크플로와 정면충돌하고,
  안정적 운영 클러스터도 아직 없다(OKE 미구축).
- **SOPS/age**: 클러스터 독립적 파일 암호화라 재생성 무풍. age 키 1개로 솔로
  개발자 키 관리 최소화. G11 Argo CD에서 ksops 플러그인으로 소비 가능.
  값 필드만 암호화(`encrypted_regex: ^(data|stringData)$`)해 diff에서 어떤
  키가 바뀌었는지는 보인다.
- 인클러스터 컨트롤러가 하나 줄어드는 것도 로컬 리소스상 이점.

### 키 관리

- age 개인키: `~/.config/sops/age/keys.txt` (커밋 금지). **macOS 주의**: sops
  기본 탐색 경로가 `~/Library/Application Support/sops/...`라서 스크립트가
  `SOPS_AGE_KEY_FILE`을 명시한다(CI/타 머신은 이 변수를 밖에서 주입).
- 공개키(recipient)만 `.sops.yaml`에 커밋. 협업자/CI 추가 시 recipient 추가 후
  재암호화(`seal`).
- 키 유실 시나리오: enc 파일 복호화만 불가 — `.env`가 살아있으면 새 키로 `seal`
  재생성, 둘 다 잃으면 Supabase 콘솔에서 키 회전 후 재구성. **개인키는 리포 밖
  백업 권장**(패스워드 매니저 등).

### 운영(OKE) 방향

운영 비밀은 로컬과 별개 파일(`k8s/secrets/prod-*.enc.yaml` 등)로 분리해 같은
체계로 관리하되, G11(Argo CD) 구축 시 ksops 통합 또는 External Secrets(OCI
Vault) 재평가 — 그때 결정.

## 검증 (2026-07-14)

- `apply` 멱등 적용, `seal` 생성본에서 값 필드 전부 `ENC[AES256_GCM,...]`·metadata
  평문 확인, 평문 잔존 grep 검사 통과.
- 왕복 실증: `apply-sealed` 복호화 적용 후 클러스터 Secret의 BFF_SECRET_KEY가
  `.env` 원본과 바이트 일치.

## 결과 및 트레이드오프

- `.env`(compose)와 k8s Secret이 자동 동기화되지는 않는다 — 비밀값 변경 시
  `apply`+`seal` 재실행은 사람 몫(README 명시). enc 파일이 원천이 아니라 파생물인
  대가로, 이중 원천(drift) 문제를 차단.
- DB/Redis 자격증명은 로컬 고정값이라 스크립트에 하드코딩 — 진짜 비밀이 되는
  운영 분리 시점에 prod enc 파일로 이동.
- 암호화 커밋본은 공개 리포에서도 안전하나, age 공개키로 누구나 **새** 값을
  암호화할 수는 있다(복호화만 불가) — enc 파일 변조는 PR 리뷰로 걸러야 한다.
- sops는 **빈 문자열을 암호화하지 않는다** — `REDIS_PASSWORD: ""`가 평문으로
  남는데, 드러나는 정보가 "로컬 Redis 무비밀번호"(compose에 이미 공개된 로컬
  고정값)뿐이라 수용. 운영에서 실제 비밀번호가 생기면 자연히 암호화된다.
