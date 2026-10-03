FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
# Render Free ~512 MB: Spring Boot 4 + JPA estoura Metaspace com 96m; heap menor + metaspace maior.
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -Xmx160m -XX:+UseSerialGC -Xss256k -XX:MaxMetaspaceSize=160m -XX:CompressedClassSpaceSize=80m -XX:ReservedCodeCacheSize=48m -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
