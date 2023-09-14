FROM openjdk:17
VOLUME /tmp
EXPOSE 8080
COPY target/demo-0.0.1-SNAPSHOT.jar e-commerce.jar
ENTRYPOINT ["java","-jar","/e-commerce.jar"]