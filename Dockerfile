# ---- first phase: compile ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# ---- Second phase: only the necessary runtime components ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Sentinel writes its logs under ~/logs/csp, so the user needs a home directory
RUN useradd --system --create-home app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
