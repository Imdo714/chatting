# Build stage
FROM gradle:8.4-jdk17 AS build
WORKDIR /app
COPY . .

# api 모듈 빌드
RUN gradle :api:clean :api:build -x test --no-daemon

# Runtime stage
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app

# api 모듈 JAR 복사
COPY --from=build /app/api/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
