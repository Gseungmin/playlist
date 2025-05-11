package com.naver.playlist.domain.service;

import com.naver.playlist.domain.dto.playlist.res.PlayListCreateResponse;
import com.naver.playlist.domain.redis.RedisHashService;
import com.naver.playlist.domain.repository.JdbcBulkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.naver.playlist.domain.constant.RedisConstants.PLAY_LIST_HASH_NAME;

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

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void syncPlayList() {
        PlayListCreateResponse dto = redisHashService.extractPlayList(PLAY_LIST_HASH_NAME);

        if (dto == null) {
            return;
        }

        bulkRepository.bulkInsert(dto.getPlayList());
        redisHashService.deleteSnapshot(dto.getKey());
    }
}
