# 1단계: 빌드 스테이지
FROM amazoncorretto:21 AS builder

WORKDIR /app
COPY src/main .
RUN chmod +x ./gradlew
RUN ./gradlew bootJar

# 2단계: 런타임 이미지
FROM amazoncorretto:21

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]