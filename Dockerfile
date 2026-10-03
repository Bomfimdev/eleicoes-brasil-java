FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
# Render Free ~512 MB — limitar heap para evitar OOM (exit 137).
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=45.0 -Xmx192m -XX:+UseSerialGC -Xss256k -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=32m"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
