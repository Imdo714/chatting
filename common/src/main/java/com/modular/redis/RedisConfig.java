package com.modular.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    /**
     * Redis 연결을 위한 'Connection' 생성합니다.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    /**
     * Redis MessageListenerContainer 설정
     * - Redis Pub/Sub 메시지 수신을 담당하는 컨테이너
     * - 스레드 풀, 에러 핸들러, 연결 팩토리 등을 설정함
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // TaskExecutor 설정, Redis 메시지 리스너들이 실행될 스레드 풀 지정
        final ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicLong threadNum = new AtomicLong(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "redis-listener-" + threadNum.getAndIncrement()); // 스레드 이름을 지정하여 모니터링 시 추적하기 쉽게 만듬
                t.setDaemon(true);
                return t;
            }
        };

        container.setTaskExecutor(Executors.newCachedThreadPool(threadFactory));

        // ErrorHandler 설정 (리스너 에러 발생 시)
        container.setErrorHandler(t -> {
            log.error("Redis Message Listener Error: {}", t.getMessage(), t);
        });

        return container;
    }

    /**
     * 조회수, ID 저장 등 단순 String 값을 위한 RedisTemplate
     * - Value Serializer: StringRedisSerializer
     */
    @Bean(name = "redisStringTemplate") // 이름을 "redisStringTemplate"으로 명시
    public RedisTemplate<String, String> stringRedisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        // Key와 Value 모두 StringRedisSerializer를 사용
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }
}
