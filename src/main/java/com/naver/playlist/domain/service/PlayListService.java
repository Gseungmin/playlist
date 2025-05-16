package com.naver.playlist.domain.service;

import com.naver.playlist.domain.dto.playlist.req.PlayListCreateRequest;
import com.naver.playlist.domain.dto.playlist.res.PlayListCreateResponse;
import com.naver.playlist.domain.dto.playlist.res.PlayListResponse;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import com.naver.playlist.domain.entity.playlist.PlayListItem;
import com.naver.playlist.domain.generator.IDGenerator;
import com.naver.playlist.domain.redis.RedisHashService;
import com.naver.playlist.domain.redis.RedisPlayListService;
import com.naver.playlist.domain.repository.PaginationRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.naver.playlist.domain.constant.RedisConstants.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayListService {

    private final RedisHashService redisHashService;
    private final RedisPlayListService redisPlayListService;
    private final PlayListAsyncService asyncService;
    private final PaginationRepository paginationRepository;
    private final IDGenerator idGenerator;
    private final RedissonClient redisson;

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
    ) throws InterruptedException {

        // 1️⃣LUA를 캐시 통한 조회
        PlayListResponse cached = getFromCache(playListId, cursor);
        if (cached != null) {
            return cached;
        }

        // 2️⃣캐시 미스시 분산락을 통한 캐시 스템피드 제어
        String lockKey = PLAY_LIST_ITEM_LOCK_KEY + playListId + ":" + cursor;
        RLock lock = redisson.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = getRock(lock);

            // 3️⃣락 획득 실패 시 캐시 재조회
            if (!acquired) {
                return getFromCache(playListId, cursor);
            }

            // 4️⃣다시 한번 재조회, 락을 획득한 사이 업데이트 되었을 수 있다.
            cached = getFromCache(playListId, cursor);
            if (cached != null) {
                return cached;
            }

            // 5️⃣페이지네이션을 통해 플레이리스트 아이템 조회
            List<PlayListItem> dtoList =
                    paginationRepository.getPlayListItemByCursor(cursor, playListId);

            // 6️⃣플레이리스트 캐시 업데이트
            PlayListResponse response = new PlayListResponse(dtoList);
            redisPlayListService.insertPlayListCache(
                    PLAY_LIST_ITEM_KEY + playListId,
                    String.valueOf(cursor == null ? 0 : cursor),
                    response
            );

            return response;
        } finally {
            releaseLock(lock, acquired);
        }
    }

    private PlayListResponse getFromCache(Long playListId, Long cursor) {
        return redisPlayListService.getPlayList(
                playListId,
                cursor
        );
    }

    private boolean getRock(RLock lock) throws InterruptedException {
        return lock.tryLock(
                PLAY_LIST_TIME_LOCK_WAIT,
                PLAY_LIST_TIME_LOCK_MAX_TIME,
                TimeUnit.SECONDS
        );
    }

    private void releaseLock(RLock lock, boolean acquired) {
        if (acquired && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
