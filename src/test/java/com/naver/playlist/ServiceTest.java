package com.naver.playlist;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
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
    void cleanRedis() {
        redisTemplate.execute((RedisCallback) conn -> {
            conn.serverCommands().flushDb(RedisServerCommands.FlushOption.SYNC);
            return "OK";
        });
    }

    @AfterEach
    public void cleanCache() {
        redisTemplate.execute((RedisCallback) conn -> {
            conn.serverCommands().flushDb(RedisServerCommands.FlushOption.SYNC);
            return "OK";
        });
    }

    public void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}