package com.naver.playlist.domain.script;

public class LUA_SCRIPT {

    public static final String INSERT_PLAY_LIST_LUA = """
        -- KEYS[1]  : 원본 해시 키
        -- ARGV[1]  : 필드 키
        -- ARGV[2]  : 필드 값
        -- ARGV[3]  : 스냅샷 TTL(sec)

        local hashKey   = KEYS[1]
        local fieldKey  = ARGV[1]
        local fieldVal  = ARGV[2]
        local ttl       = tonumber(ARGV[3])

        -- 1) 새 데이터 삽입
        redis.call('HSET', hashKey, fieldKey, fieldVal)

        -- 2) 50개 미만이면 즉시 반환
        local size = redis.call('HLEN', hashKey)
        if size < 50 then
            return { 'false' }
        end

        -- 3) 스냅샷 생성 및 TTL 지정
        local snapshotKey = hashKey .. ':' .. redis.call('TIME')[1]
        redis.call('COPY',   hashKey, snapshotKey)
        redis.call('EXPIRE', snapshotKey, ttl)

        -- 4) 원본 해시 초기화
        redis.call('DEL', hashKey)

        -- 5) 결과 반환
        local snapshotData = redis.call('HGETALL', snapshotKey)
        return { 'true', snapshotKey, snapshotData }
        """;

    public static final String EXTRACT_PLAY_LIST_LUA = """
        -- KEYS[1]  : 원본 해시 키
        -- ARGV[1]  : 스냅샷 TTL(sec)

        local hashKey   = KEYS[1]
        local ttl       = tonumber(ARGV[1])

        -- 1) 데이터가 없으면 빈 결과
        if redis.call('HLEN', hashKey) == 0 then
            return { 'false' }
        end

        -- 2) 스냅샷 키 생성 (타임스탬프 붙임)
        local snapshotKey = hashKey .. ':' .. redis.call('TIME')[1]

        -- 3) 해시 복사 → TTL 설정
        redis.call('COPY',   hashKey, snapshotKey)
        redis.call('EXPIRE', snapshotKey, ttl)

        -- 4) 원본 해시 제거
        redis.call('DEL', hashKey)

        -- 5) 스냅샷 데이터 반환
        local snapshotData = redis.call('HGETALL', snapshotKey)
        return { 'true', snapshotKey, snapshotData }
        """;
}