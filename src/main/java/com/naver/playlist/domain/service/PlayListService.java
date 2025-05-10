package com.naver.playlist.domain.service;

import com.naver.playlist.domain.dto.playlist.req.PlayListCreateRequest;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import com.naver.playlist.domain.generator.IDGenerator;
import com.naver.playlist.domain.redis.RedisHashService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayListService {

    private final RedisHashService redisHashService;
    private final IDGenerator idGenerator;

    public PlayListCreateDto create(
            PlayListCreateRequest request,
            Long memberId
    ) {
        PlayListCreateDto dto = new PlayListCreateDto(
                request,
                idGenerator.generateId(),
                memberId
        );

        redisHashService.insertPlayList(dto);
        return dto;
    }
}
