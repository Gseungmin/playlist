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

    public static final String GET_PLAY_LIST_LUA = """
        -- KEYS[1] : 원본 해시 키
        -- ARGV[1] : 필드 키
        -- ARGV[2] : 스냅샷 TTL
        -- ARGV[3] : 임계치(THRESHOLD)
        
        local hashKey   = KEYS[1]
        local fieldKey  = ARGV[1]
        local ttl       = tonumber(ARGV[2])
        local threshold = tonumber(ARGV[3])
        
        -- 1) 페이지 데이터 조회
        local data = redis.call('HGET', hashKey, fieldKey)
        if not data then
            return { 'false' }
        end
        
        -- 2) 참조 카운팅 증가
        local cnt = redis.call('HINCRBY', hashKey, 'cnt', 1)
        
        -- 3) 임계치 도달 시 TTL 연장 & 참조 카운팅 초기화
        if cnt >= threshold then
            redis.call('HSET', hashKey, 'cnt', 0)
            redis.call('EXPIRE', hashKey, ttl)
        end
        
        -- 4) HIT 결과 반환
        return { 'true', data }
       """;

    public static final String PUT_PLAY_LIST_LUA = """
        -- KEYS[1] : 원본 해시 키
        -- ARGV[1] : 필드 키
        -- ARGV[2] : JSON
        -- ARGV[3] : 스냅샷 TTL
        
        local hashKey  = KEYS[1]
        local fieldKey = ARGV[1]
        local data     = ARGV[2]
        local ttl      = tonumber(ARGV[3])
        
        -- 1) 새 페이지 저장
        redis.call('HSET', hashKey, fieldKey, data)
        
        -- 2) 참조 카운팅 없으면 0으로 초기화
        if redis.call('HEXISTS', hashKey, 'cnt') == 0 then
            redis.call('HSET', hashKey, 'cnt', 0)
        end
        
        -- 3) TTL이 없으면(== -1) 설정
        if redis.call('TTL', hashKey) < 0 then
           redis.call('EXPIRE', hashKey, ttl)
        end
        
        -- 4) 결과 반환
        return { 'OK' }
        """;
}