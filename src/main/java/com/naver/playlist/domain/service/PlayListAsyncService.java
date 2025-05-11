package com.naver.playlist.domain.service;

import com.naver.playlist.domain.dto.playlist.res.PlayListCreateResponse;
import com.naver.playlist.domain.redis.RedisHashService;
import com.naver.playlist.domain.repository.JdbcBulkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayListAsyncService {

    private final JdbcBulkRepository bulkRepository;
    private final RedisHashService redisHashService;

    @Async
    @Transactional
    public void bulkInsertAsync(PlayListCreateResponse dto) {
        bulkRepository.bulkInsert(dto.getPlayList());
        redisHashService.deleteSnapshot(dto.getKey());
    }
}
