package com.naver.playlist;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public abstract class ServiceTest {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    public void cleanUp() {
        redisTemplate.execute((RedisConnection connection) -> {
            connection.flushDb();
            return null;
        });
    }

    @AfterEach
    public void cleanCache() {
    }

    public void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}