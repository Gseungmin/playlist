package com.naver.playlist.domain.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import com.naver.playlist.web.exception.CommonException;
import com.naver.playlist.web.exception.infra.InfraException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

import static com.naver.playlist.domain.constant.RedisConstants.DUPLICATION_TIME_OUT_DAY;
import static com.naver.playlist.domain.constant.RedisConstants.PLAY_LIST_HASH_NAME;
import static com.naver.playlist.domain.script.LUA_SCRIPT.INSERT_PLAY_LIST_LUA;
import static com.naver.playlist.web.exception.ExceptionType.*;

@Service
@RequiredArgsConstructor
public class RedisHashService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final RedisScript<List> INSERT_PLAYLIST_SCRIPT =
            RedisScript.of(INSERT_PLAY_LIST_LUA, List.class);

    /*
     * 플레이리스트 생성 정보를 Lua 스크립트로 원자적으로 삽입하고
     * 50개 단위 스냅샷이 만들어졌을 때만 전체 스냅샷을 반환한다.
     */
    public List<PlayListCreateDto> insertPlayList(PlayListCreateDto dto) {
        // 1️⃣ 필드 키·값 준비
        String fieldKey = dto.getMemberId() + ":" + dto.getTitle();
        String fieldVal = convertToString(dto);

        // 2️⃣ Lua 스크립트 실행
        List<Object> result = executeInsertPlayListScript(fieldKey, fieldVal);
        validateLuaResult(result);

        // 3️⃣ 스냅샷 여부 체크
        boolean snapshotCreated = String.valueOf(result.get(0)).equals("true");
        if (!snapshotCreated) {
            return new ArrayList<>();
        }

        // 4️⃣ 스냅샷 데이터 파싱
        return buildSnapshot(result);
    }

    /* Lua 스크립트를 실행하고 결과를 그대로 반환 */
    private List<Object> executeInsertPlayListScript(String fieldKey, String fieldVal) {
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) redisTemplate.execute(
                INSERT_PLAYLIST_SCRIPT,
                Collections.singletonList(PLAY_LIST_HASH_NAME),
                fieldKey,
                fieldVal,
                String.valueOf(Duration.ofDays(DUPLICATION_TIME_OUT_DAY).getSeconds())
        );
        return result;
    }

    /* Lua 결과가 유효한지 검증 */
    private void validateLuaResult(List<Object> result) {
        if (result == null || result.isEmpty()) {
            throw new InfraException(
                    LUA_SCRIPT_RETURN_INVALID.getCode(),
                    LUA_SCRIPT_RETURN_INVALID.getErrorMessage()
            );
        }
    }

    /* Lua 결과로부터 스냅샷(Map<fieldKey, PlayListCreateDto>) 생성 */
    private List<PlayListCreateDto> buildSnapshot(List<Object> result) {
        if (result.size() < 3) {
            throw new InfraException(
                    LUA_SCRIPT_RETURN_INVALID.getCode(),
                    LUA_SCRIPT_RETURN_INVALID.getErrorMessage()
            );
        }

        String snapshotKey = String.valueOf(result.get(1));

        @SuppressWarnings("unchecked")
        List<String> dataList = (List<String>) result.get(2);

        List<PlayListCreateDto> snapshot = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i += 2) {
            String value = dataList.get(i + 1);
            snapshot.add(convertToJson(value));
        }
        return snapshot;
    }

    private String convertToString(PlayListCreateDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new CommonException(
                    SERIALIZABLE_EXCEPTION.getCode(),
                    SERIALIZABLE_EXCEPTION.getErrorMessage()
            );
        }
    }

    private PlayListCreateDto convertToJson(String value) {
        try {
            return objectMapper.readValue(value, PlayListCreateDto.class);
        } catch (JsonProcessingException e) {
            throw new CommonException(
                    UN_SERIALIZABLE_EXCEPTION.getCode(),
                    UN_SERIALIZABLE_EXCEPTION.getErrorMessage()
            );
        }
    }
}
