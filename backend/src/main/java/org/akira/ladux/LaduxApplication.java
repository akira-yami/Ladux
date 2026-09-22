package org.akira.ladux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

// Diem khoi dong backend Ladux — he thong thuong mai dien tu cong nghe.
// Kien truc modular monolith: Controller -> Service -> Repository -> PostgreSQL.
// Redis lam cache; ShedLock dam bao scheduled job chi chay tren mot instance khi scale ngang.
// @EnableJpaAuditing: tu dong ghi createdAt qua @CreatedDate tren entity.
// @EnableScheduling: bat job dinh ky (vd: huy don PENDING qua han thanh toan).
// @EnableCaching: bat Redis cache cho truy van doc nhieu (products, orders...).
@EnableJpaAuditing
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class LaduxApplication {

    public static void main(String[] args) {
        SpringApplication.run(LaduxApplication.class, args);
    }
}
