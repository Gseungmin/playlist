package com.naver.playlist.domain.service;

import com.naver.playlist.domain.dto.playlist.res.PlayListCreateResponse;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import com.naver.playlist.domain.redis.RedisHashService;
import com.naver.playlist.domain.repository.JdbcBulkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.naver.playlist.domain.constant.CommonConstants.ONE_SECOND;
import static com.naver.playlist.domain.constant.RedisConstants.PLAY_LIST_HASH_NAME;

@Service
@RequiredArgsConstructor
public class PlayListAsyncService {

    private final JdbcBulkRepository bulkRepository;
    private final RedisHashService redisHashService;
    private final Map<String, List<PlayListCreateDto>> fallbackCache = new ConcurrentHashMap<>();

    @Async
    @Transactional
    public void bulkInsertAsync(PlayListCreateResponse dto) {
        try {
            bulkRepository.bulkInsert(dto.getPlayList());
            redisHashService.deleteSnapshot(dto.getKey());
        } catch (DataAccessResourceFailureException e) {
            fallbackCache.put(dto.getKey(), dto.getPlayList());
        }
    }

    @Scheduled(fixedRate = ONE_SECOND)
    @Transactional
    public void syncPlayList() {
        PlayListCreateResponse dto = redisHashService.extractPlayList(PLAY_LIST_HASH_NAME);

        if (dto == null) {
            return;
        }

        try {
            bulkRepository.bulkInsert(dto.getPlayList());
            redisHashService.deleteSnapshot(dto.getKey());
        } catch (DataAccessResourceFailureException e) {
            fallbackCache.put(dto.getKey(), dto.getPlayList());
        }
    }

    @Scheduled(fixedRate = ONE_SECOND * 30)
    @Transactional
    public void retryFallbackCache() {
        List<String> keyList = new ArrayList<>(fallbackCache.keySet());

        for (String key : keyList) {
            List<PlayListCreateDto> list = fallbackCache.get(key);
            try {
                bulkRepository.bulkInsert(list);
                fallbackCache.remove(key);
                redisHashService.deleteSnapshot(key);
            } catch (DataAccessResourceFailureException e) {
                break;
            }
        }
    }
}
