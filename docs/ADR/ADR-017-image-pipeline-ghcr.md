# ADR-017: 이미지 파이프라인 — Dockerfile 캐시 구조(E5) + GHCR multi-arch 푸시(E4)

- 상태: 승인 (2026-07-14)
- 관련: ROADMAP E4/E5, GH #9(k8s 이전 에픽), ADR-007(운영 OCI Ampere — arm64 필수),
  ADR-016(CD 1단계 — 빌드 스텝 개선의 선행 조건으로 E4/E5 지목), F5/F6(설정·JVM 계약)

## 컨텍스트

배포 이미지 파이프라인이 없다 — 이미지는 로컬/CI에서 compose로 빌드해 쓰고 버리는
구조라, 운영 클러스터(OKE)가 pull할 레지스트리 좌표 자체가 존재하지 않는다. G11
2단계(Argo CD)의 선행 조건. 두 가지 문제가 겹쳐 있었다:

1. **Dockerfile 캐시 전멸(E5)**: `COPY . .` 후 `gradle :모듈:bootJar` 구조라 소스
   한 줄 변경에도 의존성 전체를 다시 받고, 서비스 4개가 각자 gradle 풀컴파일을
   반복(공유 모듈 4중 컴파일). deploy-kind 워크플로가 30분±인 주범(ADR-016).
   빌드 gradle 버전(이미지 8.14)과 리포 wrapper(9.5.1)의 이원화도 잠복 리스크.
2. **레지스트리 부재(E4)**: 운영이 arm64(OCI Ampere)라 multi-arch 이미지가 필수인데
   (ADR-007), 빌드·태그·푸시 파이프라인이 없다.

제약: 리포는 private(러너 2코어, 무료 분수 한정), 목표 비용 $0(ADR-007).

## 결정

### E5 — Dockerfile 구조 (backend/Dockerfile + .dockerignore 신설)

| 결정 | 근거 |
|---|---|
| 빌드 스테이지는 `--platform=$BUILDPLATFORM` 고정 | jar는 아키텍처 독립 — 컴파일을 타깃별로 반복할 이유가 없다. multi-arch 빌드에서 QEMU 에뮬레이션 아래 gradle이 도는 참사 회피(QEMU는 런타임 스테이지 cp 한 줄만) |
| gradle 이미지 대신 리포 wrapper(`./gradlew`) 사용 | CI(gradlew 9.5.1)와 빌드 버전 단일 원천. 베이스는 `eclipse-temurin:21-jdk` |
| build.gradle/settings.gradle/wrapper만 선복사 → `dependencies` 태스크로 의존성 레이어 분리 | 소스 변경이 의존성 다운로드를 무효화하지 않게. **cache mount(/root/.gradle)를 쓰지 않은 이유**: CI의 `type=gha` 캐시는 레이어만 저장하고 cache mount 내용물은 저장하지 않아, mount 방식은 CI에서 무용지물 — 평범한 레이어가 로컬·CI 양쪽에서 동작하는 유일한 교집합 |
| 빌드 스테이지가 부팅 모듈 4개 bootJar를 한 번에 빌드, `MODULE` 인자는 런타임 스테이지 전용 | 인자가 빌드 스테이지에 없어야 4개 서비스 이미지가 빌드 레이어를 통째로 공유 — compose/CI에서 4번 빌드해도 gradle 실행은 1회(구조 자체가 공유 모듈 4중 컴파일 제거) |
| `.dockerignore` 신설(build/·bin/·.gradle/ 등 제외) | 빌드 산출물이 `COPY . .`에 섞이면 소스가 안 바뀌어도 캐시가 매번 깨진다 |

compose 인터페이스(context `./backend`, args `MODULE`/`PORT`)는 불변 — compose·
deploy-kind·k8s README의 빌드 계약에 영향 없음.

### E4 — GHCR 푸시 워크플로 (.github/workflows/build-push.yml)

| 결정 | 근거 |
|---|---|
| 레지스트리 GHCR, 이미지 `ghcr.io/<owner>/mini-commerce/<service>` | GITHUB_TOKEN만으로 push(별도 크리덴셜 0), Actions와 동일 생태계 |
| **패키지는 public 운용** (2026-07-14 사용자 결정) | private 패키지는 Free 플랜 스토리지 500MB 제한 — JVM 이미지 4종×2아키텍처(1.5GB+)로 즉시 초과, $0 목표(ADR-007)와 충돌. 노출되는 건 컴파일된 jar뿐(시크릿은 전부 env 주입 계약 — F5/G8). 리포 public 전환은 별도 결정(선행: 커밋 히스토리 시크릿 스캔 + B4) |
| multi-arch `linux/amd64,linux/arm64` (buildx+QEMU) | 운영 OKE가 Ampere(arm64), 로컬 kind·CI가 amd64/arm64 혼재. E5의 BUILDPLATFORM 구조로 QEMU 비용은 런타임 스테이지에 국한 |
| 태그: `sha-<short>`(불변 좌표) + `main`(브랜치 최신) + semver(`v*` 태그 시) | k8s/Argo CD는 불변 sha 태그를 참조(2단계), `main`은 편의용. semver는 릴리스 절차 도입 시 자동 활성 |
| 단일 잡 순차 빌드(매트릭스 배제) | 같은 buildx 빌더에서 빌드 스테이지 레이어 공유 → gradle 전체 1회. 매트릭스는 콜드 캐시에서 4번 풀컴파일 — private 무료 분수 낭비 |
| 캐시 `type=gha,mode=max` | 의존성 레이어가 GHA 캐시에 살아 소스만 바뀐 빌드는 다운로드 생략 |
| provenance 비활성 | attestation 매니페스트가 이미지 인덱스에 unknown/unknown 플랫폼으로 섞이는 노이즈 제거 |
| 트리거: main push(backend/**·compose·워크플로) + `v*` 태그 + 수동 | 이미지가 필요한 변경에만. paths 필터는 태그 push엔 적용 안 됨(Actions 문서) |

### 보류 결정

- **deploy-kind의 빌드 스텝 GHCR pull 전환 보류**: 같은 push에 build-push와
  deploy-kind가 동시 실행되면 pull이 stale 이미지를 집는 경쟁 조건 — workflow_run
  체이닝 없이는 정합이 안 맞는다. E5만으로 deploy-kind 빌드가 구조적으로 단축
  (gradle 4회→1회)되므로, pull 전환은 G11 2단계(Argo CD)에서 재설계.
- **GHCR 패키지 정리(retention) 정책**: sha 태그가 무한 누적되지만 public이라 비용
  0. 목록이 지저분해지면 `actions/delete-package-versions` 도입 재평가.

## 결과 및 트레이드오프

- 운영 클러스터가 pull할 불변 이미지 좌표가 생겼다 — G11 2단계(Argo CD)의 선행
  조건 충족. 남은 선행은 OKE 구축.
- 소스만 바뀐 빌드는 의존성 재다운로드 없이 컴파일부터 시작(로컬 레이어 캐시·CI
  gha 캐시 공통). 단 gradle 증분 컴파일은 없음 — 컨테이너 빌드는 매번 클린 컴파일
  (트레이드오프 수용: 정합성 > 속도).
- 빌드 파일(build.gradle 등) 변경 시엔 의존성 레이어부터 재실행 — 새 모듈 추가 시
  Dockerfile의 선복사 목록에 build.gradle 한 줄 추가 필요(Dockerfile 주석에 명시).
- 첫 push 후 4개 패키지의 visibility를 GitHub UI에서 public으로 전환 필요(REST
  API 미지원 — 패키지 Settings > Danger Zone). 전환 전까지는 private 500MB 한도에
  걸릴 수 있어, 초과로 push가 거부되면 전환을 먼저 하고 재실행한다.
