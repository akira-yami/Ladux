package org.akira.ladux.shared.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Dùng ShedLock để quản lý Scheduled Job, lưu trạng thái lock vào database
// Đây là file cấu hình để kết nối ShedLock đến dự án
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")  // Lock tối đa 30 phút để phòng trường hợp task bị treo
public class ShedLockConfig {
    @Bean
    public LockProvider lockProvider(DataSource dataSource) { // Tạo cầu nối giữa ShedLock và database thông qua DataSource
        return new JdbcTemplateLockProvider(dataSource);
    }
}