# IMOS Backend

IMOS 백엔드 애플리케이션입니다. Spring Boot, Kotlin, GraphQL을 사용하여 구현되었습니다.

## 주요 기능

*   GraphQL API 제공 (Netflix DGS 프레임워크 사용)
*   데이터베이스 연동 (JPA, MySQL, MongoDB, QueryDSL)
*   Spring Security를 이용한 인증 및 인가
*   JWT 기반 토큰 인증
*   문서 변환 기능 (LibreOffice 활용)

## 기술 스택

*   **언어**: Kotlin 1.9.25
*   **프레임워크**: Spring Boot 3.4.3
*   **데이터베이스**:
    *   Spring Data JPA
    *   MySQL
    *   Spring Data MongoDB
    *   QueryDSL
*   **API**:
    *   Spring Web
    *   GraphQL (Netflix DGS)
*   **보안**:
    *   Spring Security
    *   JWT (JSON Web Token)
*   **로깅**:
    *   Spring Boot Starter Logging
    *   p6spy (SQL 로깅)
*   **빌드 도구**: Gradle
*   **기타 라이브러리**:
    *   Lombok
    *   Jackson (Kotlin module)
    *   JODConverter (문서 변환)
    *   Janino

## 실행 방법

### IntelliJ IDEA에서 실행하기

1.  프로젝트를 IntelliJ IDEA에서 엽니다.
2.  `src/main/kotlin/kr/co/imoscloud/ImosBackEndApplication.kt` 파일을 엽니다.
3.  `main` 함수 왼쪽의 초록색 실행 버튼을 클릭하거나, 상단 메뉴에서 `Run > Run 'ImosBackEndApplicationKt'`을 선택합니다. (클래스 이름이 `ImosBackEndApplicationKt`로 변경될 수 있습니다.)

### Gradle을 사용하여 실행하기

터미널에서 다음 명령어를 실행합니다:

```bash
./gradlew bootRun
```

## 프로젝트 구조

주요 소스 코드는 `src/main/kotlin/kr/co/imoscloud` 디렉토리 내에 위치하며, 일반적인 Spring Boot 애플리케이션의 패키지 구조를 따릅니다.

*   `ImosBackEndApplication.kt`: 애플리케이션의 메인 진입점
*   `controller/`: API 요청을 처리하는 컨트롤러
*   `service/`: 비즈니스 로직을 담당하는 서비스
*   `entity/` 또는 `model/`: 데이터베이스 엔티티 또는 데이터 모델
*   `repository/`: 데이터베이스 접근을 위한 리포지토리
*   `config/`: 애플리케이션 설정 관련 클래스
*   `security/`: 보안 관련 설정 및 로직
*   `fetcher/`: GraphQL 데이터 페처
*   `dto/`: 데이터 전송 객체 (Data Transfer Objects)

## Docker를 사용하여 실행하기

프로젝트 루트 디렉토리에서 다음 명령어를 사용하여 Docker 이미지를 빌드하고 컨테이너를 실행할 수 있습니다.

### 1. Docker 이미지 빌드

```bash
docker build -t imos-backend .
```

(`imos-backend`는 원하는 이미지 이름으로 변경 가능합니다.)

### 2. Docker 컨테이너 실행

```bash
docker run -d -p 8080:8080 -p 5005:5005 --name imos-app imos-backend
```

*   `-d`: 컨테이너를 백그라운드에서 실행합니다.
*   `-p 8080:8080`: 호스트의 8080 포트를 컨테이너의 8080 포트(애플리케이션 포트)와 매핑합니다.
*   `-p 5005:5005`: 호스트의 5005 포트를 컨테이너의 5005 포트(디버그 포트)와 매핑합니다. (디버깅이 필요 없는 경우 이 부분은 생략 가능)
*   `--name imos-app`: 컨테이너의 이름을 `imos-app`으로 지정합니다.
*   `imos-backend`: 실행할 Docker 이미지 이름입니다. (1단계에서 지정한 이름 사용)

애플리케이션은 `http://localhost:8080` 에서 접근할 수 있습니다.

### 컨테이너 로그 확인

```bash
docker logs imos-app
```

### 실행 중인 컨테이너 중지 및 삭제

```bash
docker stop imos-app
docker rm imos-app
```
