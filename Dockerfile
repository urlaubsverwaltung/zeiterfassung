FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/zeiterfassung-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
