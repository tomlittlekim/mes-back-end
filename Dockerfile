# 1단계: 빌드 스테이지
FROM amazoncorretto:21 AS builder
WORKDIR /app

# Gradle Wrapper와 빌드 관련 파일들을 먼저 복사하여 의존성 캐싱 활용
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/

# 의존성 캐싱을 위해 초기 Gradle 작업 실행 (필요 시 --no-daemon 옵션 추가)
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

# 소스 코드를 복사하고 애플리케이션 빌드
COPY src/ src/
RUN ./gradlew --no-daemon bootJar

# 2단계: 런타임 이미지
FROM amazoncorretto:21
WORKDIR /app

# 빌드 스테이지에서 생성된 jar 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]