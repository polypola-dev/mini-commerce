FROM gradle:8.14-jdk21 AS build
WORKDIR /workspace
COPY . .
# MODULE: 디렉토리 경로 형식(예: shop-api, order/order-api). 멀티모듈 구조(Phase 2, 2026-07-01)
# 이후 부팅 모듈이 여러 개라 어떤 모듈의 bootJar를 만들지 빌드 인자로 받는다.
ARG MODULE=shop-api
RUN GRADLE_PATH=$(echo "${MODULE}" | tr '/' ':') && gradle ":${GRADLE_PATH}:bootJar" --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
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
