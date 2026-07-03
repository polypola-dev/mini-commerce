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
ENTRYPOINT ["java", "-jar", "app.jar"]
