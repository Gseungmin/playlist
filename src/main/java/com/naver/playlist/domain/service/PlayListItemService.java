package com.naver.playlist.domain.service;

import com.naver.playlist.domain.dto.playlist.req.PlayListItemProjection;
import com.naver.playlist.domain.dto.playlist.req.ReorderPlayListItemsRequest;
import com.naver.playlist.domain.dto.playlist.res.PlayListReOrderResponse;
import com.naver.playlist.domain.entity.playlist.PlayList;
import com.naver.playlist.domain.entity.playlist.PlayListItem;
import com.naver.playlist.domain.repository.JdbcBulkRepository;
import com.naver.playlist.domain.repository.PlayListItemRepository;
import com.naver.playlist.web.exception.entity.PlayListException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.naver.playlist.domain.constant.EntityConstants.GAP;
import static com.naver.playlist.web.exception.ExceptionType.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayListItemService {

    private final JdbcBulkRepository jdbcBulkRepository;
    private final PlayListItemRepository playListItemRepository;

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
