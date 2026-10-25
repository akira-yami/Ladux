package org.akira.ladux.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class VNPayReconciliationConfig {
    // Keep unrelated scheduled jobs off the blocking gateway worker.
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ladux-scheduled-");
        return scheduler;
    }

    @Bean
    public ThreadPoolTaskScheduler vnPayTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("vnpay-reconcile-");
        return scheduler;
    }
}
