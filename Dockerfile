FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar

# Also can copy out this line as an example of file generation in Docker only. This will not Copy the locally
# Generated file into the container. Instead, it will generate its own on the container.
# COPY src/main/resources/data.sql /app/resources/data.sql
ENTRYPOINT ["java", "-jar", "app.jar"]

# Expose port 8082 to connect to H2 Console.
EXPOSE 8082

# Expose port 7772 for debugging
EXPOSE 7772