package com.mysticmart.config;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration @EnableJpaAuditing
public class AppConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        var exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4); exec.setMaxPoolSize(10);
        exec.setQueueCapacity(100); exec.setThreadNamePrefix("MysticAsync-");
        exec.initialize(); return exec;
    }
}
