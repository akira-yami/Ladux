FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .

# Tách riêng tầng tải dependencies để Docker cache lại, có cấu hình retry khi mạng rớt
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B dependency:go-offline \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.http.retryHandler.requestSentKeepAliveRuntime=true \
    -Dmaven.wagon.http.connectionTimeout=120000 \
    -Dmaven.wagon.http.readTimeout=120000 || true

COPY src ./src

# Build JAR sử dụng cache .m2 và tự động retry nếu tải bổ sung plugin bị đứt kết nối
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B clean package -DskipTests \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.http.retryHandler.requestSentKeepAliveRuntime=true \
    -Dmaven.wagon.http.connectionTimeout=120000 \
    -Dmaven.wagon.http.readTimeout=120000

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S ladux && adduser -S ladux -G ladux \
    && mkdir -p /app/uploads && chown -R ladux:ladux /app

COPY --from=builder /build/target/ladux.jar /app/ladux.jar

# Đảm bảo non-root user có quyền đọc jar và ghi uploads (volume có thể override owner)
RUN chown ladux:ladux /app/ladux.jar && chmod 755 /app/uploads

USER ladux
EXPOSE 8080

# Heap 65% de chua cho metaspace, thread stack, direct buffer (Lettuce/HTTP client AI) trong mem_limit.
# ExitOnOutOfMemoryError: de Docker restart container thay vi chay tiep trong trang thai hong.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/ladux.jar"]
