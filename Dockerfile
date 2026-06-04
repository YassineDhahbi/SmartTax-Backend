# Backend Spring Boot + SWIN (DJL/PyTorch necessite glibc, pas Alpine)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends libgomp1 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r spring && useradd -r -g spring spring
RUN mkdir -p /app/uploads/download-documents /app/uploads/users /app/uploads/publications /app/logs /app/models \
    && chown -R spring:spring /app
COPY --from=build /build/target/*.jar /app/app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN sed -i 's/\r$//' /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["/docker-entrypoint.sh"]
