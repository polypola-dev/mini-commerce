# 빌드 스테이지는 --platform=$BUILDPLATFORM 으로 항상 빌드 머신 네이티브로 돈다(E5).
# jar는 아키텍처 독립이라 컴파일을 타깃 아키텍처별로 반복할 이유가 없고, multi-arch
# 빌드(E4, 운영 OCI Ampere가 arm64 — ADR-007)에서 QEMU 에뮬레이션 아래 gradle이 도는
# 참사를 피한다. QEMU를 타는 건 런타임 스테이지의 사소한 cp 한 줄뿐이다.
# gradle 배포판은 별도 이미지(구 gradle:8.14) 대신 리포 wrapper(gradle/wrapper/*.properties)를
# 써서 CI(gradlew)와 버전을 단일 원천으로 통일한다.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 의존성 해석에 필요한 파일만 선복사 — 소스 변경이 의존성 다운로드 레이어를 무효화하지
# 않게 한다(E5). 새 모듈 추가 시 여기에 build.gradle 한 줄을 같이 추가할 것.
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
COPY shared-core/build.gradle shared-core/
COPY shared-web/build.gradle shared-web/
COPY catalog/build.gradle catalog/
COPY inventory/inventory-events/build.gradle inventory/inventory-events/
COPY inventory/inventory-core/build.gradle inventory/inventory-core/
COPY inventory/inventory-api/build.gradle inventory/inventory-api/
COPY shop-api/build.gradle shop-api/
COPY order/order-events/build.gradle order/order-events/
COPY order/order-domain/build.gradle order/order-domain/
COPY order/order-infra/build.gradle order/order-infra/
COPY order/order-api/build.gradle order/order-api/
COPY order/order-admin/build.gradle order/order-admin/
COPY order/order-batch/build.gradle order/order-batch/
# 부팅 모듈 4개의 dependencies 태스크로 wrapper 배포판 + 외부 의존성을 이 레이어에
# 내려받아 둔다. cache mount(/root/.gradle) 대신 평범한 레이어를 쓰는 이유: CI의
# type=gha 캐시는 레이어만 저장하고 cache mount 내용물은 저장하지 않아서, cache mount로는
# 소스만 바뀐 CI 빌드가 매번 의존성을 처음부터 다시 받는다.
RUN ./gradlew --no-daemon \
    :shop-api:dependencies \
    :order:order-api:dependencies \
    :order:order-admin:dependencies \
    :order:order-batch:dependencies \
    :inventory:inventory-api:dependencies > /dev/null

COPY . .
# 부팅 모듈 4개의 bootJar를 한 번에 빌드한다. MODULE 인자를 이 스테이지에서 쓰지 않아야
# 4개 서비스 이미지가 빌드 스테이지 레이어를 통째로 공유한다 — compose/CI에서 4번 빌드해도
# gradle 실행은 1회(공유 모듈 중복 컴파일 제거).
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
# MODULE: 디렉토리 경로 형식(예: shop-api, order/order-api). 멀티모듈 구조(Phase 2, 2026-07-01)
# 이후 부팅 모듈이 여러 개라 어떤 모듈의 jar를 담을지 이미지 인자로 받는다(런타임 스테이지 전용).
ARG MODULE=shop-api
ARG PORT=8080
# bootJar가 실행가능 fat jar와 -plain.jar(비실행) 둘 다 만들기 때문에 plain은 제외하고 골라온다.
COPY --from=build /workspace/${MODULE}/build/libs/ /tmp/libs/
RUN find /tmp/libs -maxdepth 1 -name "*.jar" ! -name "*-plain.jar" -exec cp {} /app/app.jar \; && rm -rf /tmp/libs
EXPOSE ${PORT}
# JVM 컨테이너 튜닝(F6). 기본 MaxHeapSize는 컨테이너 메모리의 25%라 limit을 크게 낭비하고,
# 무설정 배포는 k8s에서 OOMKill 단골이다. MaxRAMPercentage로 힙을 limit에 비례시키고
# (limit 1GiB 기준 힙 최대 768MiB, 나머지는 metaspace/코드캐시/스레드 스택 몫),
# 절대값(-Xmx)이 아닌 비율로 둬서 서비스별 limit 조정 시 이미지 재빌드가 필요 없게 한다.
# JAVA_OPTS는 compose/k8s env로 통째로 교체 가능(설정 외부화 계약 — backend/doc/CONFIGURATION.md).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
# exec가 없으면 sh가 PID 1로 남아 SIGTERM을 java에 전달하지 않는다 — graceful shutdown(F4)이
# 컨테이너 유예 후 SIGKILL로 무효화되므로 exec 유지가 필수다.
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]
