FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/*.jar app.jar
COPY src/main/resources/data.sql /app/resources/data.sql
ENTRYPOINT ["java", "-jar", "app.jar"]