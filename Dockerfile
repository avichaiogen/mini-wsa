FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src/ src/
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

RUN rm -rf /var/cache/apk/* /tmp/* /var/tmp/* \
           /opt/java/openjdk/lib/src.zip \
           /opt/java/openjdk/lib/security/cacerts.bck \
    && adduser -D -H appuser

WORKDIR /app

COPY --from=builder /build/target/mini-wsa.jar mini-wsa.jar
RUN mkdir logs && chown appuser:appuser logs

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "mini-wsa.jar"]
