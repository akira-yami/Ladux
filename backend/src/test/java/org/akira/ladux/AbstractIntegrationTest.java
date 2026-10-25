package org.akira.ladux;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Lop cha cho moi integration test.
 *
 * <p>Dung <b>Singleton Container pattern</b>: cac container khoi dong MOT LAN trong static block,
 * dung chung cho toan bo JVM va KHONG bi dung giua cac test class. Ly do:
 * Spring cache application context va tai su dung giua cac class co cung cau hinh; neu dung
 * {@code @Testcontainers + @Container} thi container bi dung sau moi class (afterAll), khien context
 * duoc cache van tro vao container da dong -> loi "connection has been closed".
 * Ryuk (container phu cua Testcontainers) se don dep container khi JVM thoat.
 *
 * <ul>
 *   <li>Postgres: nguon du lieu chinh (Flyway dung schema + nap seed).</li>
 *   <li>Redis: can cho rate limiting (bucket4j luu bucket tren Redis).</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg17").withTmpFs(java.util.Map.of("/var/lib/postgresql/data", "rw")).withStartupTimeout(java.time.Duration.ofMinutes(3));

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:alpine").withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    /**
     * Bom thong tin ket noi cua cac container (cong dong) vao Spring truoc khi context khoi dong.
     * Schema do Flyway dung (cau hinh trong application-test.properties).
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
