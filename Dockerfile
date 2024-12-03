# Stage 1: Build stage
FROM --platform=$BUILDPLATFORM eclipse-temurin:17 AS builder
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod 700 ./mvnw
# Download dependencies first (cached layer)
RUN ./mvnw dependency:go-offline

# Copy source and build
COPY src ./src/
RUN ./mvnw clean package -DskipTests

# Stage 2: Production stage
FROM --platform=$TARGETPLATFORM gcr.io/distroless/java17-debian12:latest
WORKDIR /app
# Copy only the built jar from builder stage
COPY --from=builder /build/target/project-controller.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]