# 1단계: jar 빌드를 위한 빌더 이미지 (선택적)
FROM amazoncorretto:21 AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew bootJar

# 2단계: 실제 실행 환경 이미지
FROM amazoncorretto:21
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]