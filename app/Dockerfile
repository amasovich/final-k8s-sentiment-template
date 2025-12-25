FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY SentimentApplication.java .
RUN javac SentimentApplication.java && jar cfe app.jar SentimentApplication SentimentApplication.class

# Create minimal JRE for runtime (smaller image)
RUN jlink   --module-path /opt/java/openjdk/jmods   --add-modules java.base,java.logging,jdk.httpserver   --output /opt/jre-min   --compress=2 --no-header-files --no-man-pages

FROM alpine:3.20
WORKDIR /app

COPY --from=builder /opt/jre-min /opt/jre-min
COPY --from=builder /app/app.jar /app/app.jar

RUN addgroup -S appgroup && adduser -S appuser -G appgroup && chown -R appuser:appgroup /app
USER appuser

ENV PATH="/opt/jre-min/bin:${PATH}"
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
