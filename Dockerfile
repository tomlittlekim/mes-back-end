# 1단계: 빌드 스테이지
FROM amazoncorretto:21 AS builder
WORKDIR /app

# Gradle 캐시용 의존성은 미리 설치하되,
# 빌드는 소스 복사 후에만 실행되도록 순서 조정

COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

# 이 타이밍에 소스코드 복사
COPY src/ src/

# 여기서 빌드
RUN ./gradlew --no-daemon clean bootJar
# 2단계: 런타임 이미지
FROM amazoncorretto:21
WORKDIR /app

ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 빌드 스테이지에서 생성된 jar 파일 복사
COPY --from=builder /app/build/libs/app.jar app.jar

EXPOSE 8080 5005

# 디버그 모드 활성화 옵션 추가
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "app.jar"]