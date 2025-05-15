package com.naver.playlist.domain.service;

import com.naver.playlist.domain.dto.playlist.req.PlayListCreateRequest;
import com.naver.playlist.domain.dto.playlist.res.PlayListCreateResponse;
import com.naver.playlist.domain.dto.playlist.res.PlayListResponse;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import com.naver.playlist.domain.entity.playlist.PlayListItem;
import com.naver.playlist.domain.generator.IDGenerator;
import com.naver.playlist.domain.redis.RedisHashService;
import com.naver.playlist.domain.repository.PaginationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayListService {

    private final RedisHashService redisHashService;
    private final PlayListAsyncService asyncService;
    private final PaginationRepository paginationRepository;
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

        PlayListCreateResponse response =
                redisHashService.insertPlayList(dto);

        if (response == null) {
            return dto;
        }

        asyncService.bulkInsertAsync(response);
        return dto;
    }

    public PlayListResponse get(
            Long playListId,
            Long cursor
    ) {
        // 1️⃣ 페이지네이션을 통해 플레이리스트 아이템 조회
        List<PlayListItem> dtoList = paginationRepository.getPlayListItemByCursor(
                cursor,
                playListId
        );
        return new PlayListResponse(dtoList);
    }
}
