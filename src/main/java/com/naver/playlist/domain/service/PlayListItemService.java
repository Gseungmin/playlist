package com.naver.playlist.domain.service;

import com.naver.playlist.domain.dto.projection.PlayListItemProjection;
import com.naver.playlist.domain.dto.playlist.req.ReorderPlayListItemsRequest;
import com.naver.playlist.domain.dto.playlist.res.PlayListReOrderResponse;
import com.naver.playlist.domain.dto.projection.PlayListStatProjection;
import com.naver.playlist.domain.entity.music.Music;
import com.naver.playlist.domain.entity.playlist.PlayList;
import com.naver.playlist.domain.entity.playlist.PlayListItem;
import com.naver.playlist.domain.repository.JdbcBulkRepository;
import com.naver.playlist.domain.repository.PlayListItemRepository;
import com.naver.playlist.domain.repository.PlayListRepository;
import com.naver.playlist.web.exception.entity.PlayListException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.naver.playlist.domain.constant.EntityConstants.*;
import static com.naver.playlist.domain.constant.RedisConstants.*;
import static com.naver.playlist.web.exception.ExceptionType.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayListItemService {

    private final JdbcBulkRepository jdbcBulkRepository;
    private final PlayListItemRepository playListItemRepository;
    private final PlayListRepository playListRepository;
    private final RedissonClient redisson;

    /* 플레이리스트에 노래 추가하는 메서드 */
    public void create(
            Long playListId,
            Long memberId,
            Music music) throws InterruptedException {
        String lockKey = LOCK_KEY + playListId;
        RLock lock = redisson.getLock(lockKey);

        boolean acquired = false;
        try {
            // 1️⃣ 분산락 획득
            acquired = getRock(lock);

            // 2️⃣ 단일 쿼리로 현재 카운트 + 마지막 Order 조회
            PlayListStatProjection playListStat =
                    getPlayListStat(playListId, memberId);

            // 3️⃣ 1000개를 넘어서면 안되므로 체크
            validateCount(playListStat.getCount());

            // 4️⃣ 마지막 순서를 기반으로 GAP 추가
            Long lastOrder = playListStat.getLastOrder();

            PlayListItem playListItem = new PlayListItem(
                    lastOrder + GAP,
                    playListStat.getPlayList(),
                    music
            );

            // 5️⃣ 플레이리스트 아이템 저장
            playListItemRepository.save(playListItem);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private boolean getRock(RLock lock) throws InterruptedException {
        boolean acquired = lock.tryLock(LOCK_NO_WAIT, LOCK_MAX_TIME, TimeUnit.SECONDS);

        if (!acquired) {
            throw new PlayListException(
                    PLAY_LIST_NOT_CONCURRENCY.getCode(),
                    PLAY_LIST_NOT_CONCURRENCY.getErrorMessage()
            );
        }

        return true;
    }

    private PlayListStatProjection getPlayListStat(Long playListId, Long memberId) {
        Optional<PlayListStatProjection> optional =
                playListRepository.findPlayListWithStat(playListId, memberId);

        if (optional.isEmpty()) {
            throw new PlayListException(
                    PLAY_LIST_NOT_EXIST.getCode(),
                    PLAY_LIST_NOT_EXIST.getErrorMessage()
            );
        }

        return optional.get();
    }

    private void validateCount(long count) {
        if (count >= MAX_PLAY_LIST_COUNT) {
            throw new PlayListException(
                    PLAY_LIST_EXCEED_LIMIT.getCode(),
                    PLAY_LIST_EXCEED_LIMIT.getErrorMessage()
            );
        }
    }

    /* 플레이리스트 노래 순서 변경 메서드 */
    public PlayListReOrderResponse reorder(
            Long playListId,
            Long memberId,
            List<ReorderPlayListItemsRequest> dto
    ) {
        // 1️⃣ 조회할 아이디 추출
        List<Long> ids = extractIds(dto);

        // 2️⃣ 페치조인 + IN 쿼리 조회 - PlayListItem + PlayList + Member
        List<PlayListItem> playListItems =
                playListItemRepository.findPlayListItemForUpdate(ids);

        // 3️⃣ 개수 불일치 → 일부 곡이 존재하지 않음
        validateExistence(dto, playListItems);

        // 4️⃣ 플레이리스트 ID 검증 (모든 곡이 같은 플레이리스트 소속인지)
        validatePlayList(playListId, playListItems);

        // 5️⃣ 멤버 검증 (해당 플레이리스트 소유자인지)
        validateMember(memberId, playListItems.get(0).getPlayList());

        // 6️⃣ 벌크 업데이트 - 실패시 전체 재오더링, 예외를 던지는 이유는 리렌더링 후 클라이언트에서 재조정해야하기 때문
        return bulkUpdate(dto, playListId);
    }

    private List<Long> extractIds(List<ReorderPlayListItemsRequest> dto) {
        return dto.stream()
                .map(ReorderPlayListItemsRequest::getPlayListItemId)
                .toList();
    }

    private void validateExistence(
            List<ReorderPlayListItemsRequest> dto,
            List<PlayListItem> playListItems
    ) {
        if (dto.size() != playListItems.size()) {
            throw new PlayListException(
                    PLAY_LIST_ITEM_NOT_EXIST.getCode(),
                    PLAY_LIST_ITEM_NOT_EXIST.getErrorMessage()
            );
        }
    }

    private void validatePlayList(
            Long playListId,
            List<PlayListItem> playListItems
    ) {
        boolean hasWrongPlayList = playListItems.stream()
                .anyMatch(item -> !item.getPlayList().getId().equals(playListId));

        if (hasWrongPlayList) {
            throw new PlayListException(
                    PLAY_LIST_NOT_MATCH_ITEM.getCode(),
                    PLAY_LIST_NOT_MATCH_ITEM.getErrorMessage()
            );
        }
    }

    private void validateMember(Long memberId, PlayList playList) {
        if (!playList.getMember().getId().equals(memberId)) {
            throw new PlayListException(
                    PLAY_LIST_AUTH_INVALID.getCode(),
                    PLAY_LIST_AUTH_INVALID.getErrorMessage()
            );
        }
    }

    private PlayListReOrderResponse bulkUpdate(
            List<ReorderPlayListItemsRequest> dto,
            Long playListId
    ) {
        try {
            jdbcBulkRepository.bulkUpdatePosition(dto);
            return new PlayListReOrderResponse(true, dto);
        } catch (DuplicateKeyException ex) {
            List<ReorderPlayListItemsRequest> reorderList = reorderingPositions(playListId);
            jdbcBulkRepository.bulkUpdatePosition(reorderList);
            return new PlayListReOrderResponse(false, reorderList);
        }
    }

    private List<ReorderPlayListItemsRequest> reorderingPositions(Long playListId) {
        List<PlayListItemProjection> playListItemAll =
                playListItemRepository.findPlayListItemAll(playListId);

        List<ReorderPlayListItemsRequest> reorderList = new ArrayList<>(playListItemAll.size());
        long position = GAP;

        for (PlayListItemProjection p : playListItemAll) {
            reorderList.add(new ReorderPlayListItemsRequest(p.getPlayListItemId(), position));
            position += GAP;
        }
        return reorderList;
    }
}
