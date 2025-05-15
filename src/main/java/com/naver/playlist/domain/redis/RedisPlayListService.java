package com.naver.playlist.domain.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naver.playlist.domain.dto.playlist.res.PlayListResponse;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import com.naver.playlist.web.exception.CommonException;
import com.naver.playlist.web.exception.infra.InfraException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.naver.playlist.domain.constant.RedisConstants.*;
import static com.naver.playlist.domain.script.LUA_SCRIPT.*;
import static com.naver.playlist.web.exception.ExceptionType.*;

@Service
@RequiredArgsConstructor
public class RedisPlayListService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final RedisScript<List> GET_PLAYLIST_SCRIPT =
            RedisScript.of(GET_PLAY_LIST_LUA, List.class);

    private static final RedisScript<List> PUT_PLAYLIST_SCRIPT =
            RedisScript.of(PUT_PLAY_LIST_LUA, List.class);

    /*
     * 해시안에 순서 정보를 key, 데이터 자체를 value로 가지는 캐시 존재
     * 플레이리스트 페이지 삽입 Lua 스크립트로 원자적으로 삽입하고
     * 참조 카운팅이 없다면 초기화, 만약 TTL이 없다면 초기화
     */
    public void insertPlayListCache(
            String hashKey,
            String fieldKey,
            PlayListResponse response
    ) {
        String json = convertToString(response);

        redisTemplate.execute(
                PUT_PLAYLIST_SCRIPT,
                Collections.singletonList(hashKey),
                fieldKey,
                json,
                String.valueOf(PLAY_LIST_ITEM_TTL)
        );
    }

    /*
     * 플레이리스트 조회를 Lua 스크립트로 원자적으로 조회하고
     * 참조 카운팅 증가, 만약 참조카운팅이 특정 임계를 넘어서면 TTL 초기화
     * 이를 통해 인기게시글에 대한 캐시 유지
     */
    public PlayListResponse getPlayList(
            Long playListId,
            Long cursor
    ) {
        String hashKey  = PLAY_LIST_ITEM_KEY + playListId;
        String fieldKey = String.valueOf(cursor == null ? 0 : cursor);

        List<Object> result = executeGetPlayListScript(hashKey, fieldKey);
        validateLuaResult(result);

        if (!String.valueOf(result.get(0)).equals("true")) {
            return null;
        }

        return convertToJson(String.valueOf(result.get(1)));
    }

    /* Lua 스크립트를 실행하고 결과를 그대로 반환 */
    private List<Object> executeGetPlayListScript(String hashKey, String fieldKey) {
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) redisTemplate.execute(
                GET_PLAYLIST_SCRIPT,
                Collections.singletonList(hashKey),
                fieldKey,
                String.valueOf(PLAY_LIST_ITEM_TTL),
                String.valueOf(THRESHOLD)
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

    private String convertToString(PlayListResponse dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new CommonException(
                    SERIALIZABLE_EXCEPTION.getCode(),
                    SERIALIZABLE_EXCEPTION.getErrorMessage()
            );
        }
    }

    private PlayListResponse convertToJson(String value) {
        try {
            return objectMapper.readValue(value, PlayListResponse.class);
        } catch (JsonProcessingException e) {
            throw new CommonException(
                    UN_SERIALIZABLE_EXCEPTION.getCode(),
                    UN_SERIALIZABLE_EXCEPTION.getErrorMessage()
            );
        }
    }
}
