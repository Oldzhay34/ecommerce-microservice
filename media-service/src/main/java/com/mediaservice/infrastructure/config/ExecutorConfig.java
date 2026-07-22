package com.mediaservice.infrastructure.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebP donusumu CPU-yogundur; sabit boyutlu havuz kullanilir.
 */
@Configuration
public class ExecutorConfig {

    private static final Logger log = LoggerFactory.getLogger(ExecutorConfig.class);

    private final int poolSize;
    private ExecutorService conversionExecutor;

    public ExecutorConfig(@Value("${media.conversion.pool-size}") int poolSize) {
        this.poolSize = poolSize;
    }

    @Bean(name = "conversionExecutor")
    public ExecutorService conversionExecutor() {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "webp-conv-" + counter.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        };
        this.conversionExecutor = Executors.newFixedThreadPool(poolSize, factory);
        log.info("WebP donusum havuzu olusturuldu. poolSize={}", poolSize);
        return this.conversionExecutor;
    }

    @PreDestroy
    public void shutdown() {
        if (conversionExecutor == null) {
            return;
        }
        log.info("WebP donusum havuzu kapatiliyor...");
        conversionExecutor.shutdown();
        try {
            if (!conversionExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                conversionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            conversionExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}