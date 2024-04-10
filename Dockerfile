FROM openjdk:21-jdk
COPY build/libs/accounting-0.0.1-SNAPSHOT.jar /app/
WORKDIR /app
ENTRYPOINT ["java"]
CMD ["-Dprofile=prod", "-jar", "accounting-0.0.1-SNAPSHOT.jar"]
