FROM amazoncorretto:21-alpine

LABEL app="BookVault" version="1.0" maintainer="Viswapriya"

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]