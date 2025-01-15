FROM openjdk:17-jdk
LABEL maintainer="dndqlsrnt@gmail.com"
WORKDIR /spring-boot
COPY build/libs/ballBin-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/spring-boot/app.jar"]