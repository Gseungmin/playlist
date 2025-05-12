package com.naver.playlist.domain.service;

import com.naver.playlist.domain.dto.playlist.req.ReorderPlayListItemsRequest;
import com.naver.playlist.domain.entity.playlist.PlayList;
import com.naver.playlist.domain.entity.playlist.PlayListItem;
import com.naver.playlist.domain.repository.PlayListItemRepository;
import com.naver.playlist.web.exception.entity.PlayListException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.naver.playlist.web.exception.ExceptionType.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayListItemService {

    private final PlayListItemRepository playListItemRepository;

    public void reorder(
            Long playListId,
            Long memberId,
            List<ReorderPlayListItemsRequest> dto
    ) {
        // 1️⃣ 조회할 아이디 추출
        List<Long> ids = dto.stream()
                .map(ReorderPlayListItemsRequest::getPlayListItemId)
                .toList();

        // 2️⃣ 페치조인 + IN 쿼리 조회 - PlayListItem + PlayList + Member
        List<PlayListItem> playListItems =
                playListItemRepository.findPlayListItemForUpdate(ids);

        // 3️⃣ 개수 불일치 → 일부 곡이 존재하지 않음
        if (dto.size() != playListItems.size()) {
            throw new PlayListException(
                    PLAY_LIST_ITEM_NOT_EXIST.getCode(),
                    PLAY_LIST_ITEM_NOT_EXIST.getErrorMessage()
            );
        }

        // 4️⃣ 플레이리스트 ID 검증 (모든 곡이 같은 플레이리스트 소속인지)
        boolean hasWrongPlayList =
                playListItems.stream()
                        .anyMatch(playListItem -> !playListItem.getPlayList().getId().equals(playListId));

        if (hasWrongPlayList) {
            throw new PlayListException(
                    PLAY_LIST_NOT_MATCH_ITEM.getCode(),
                    PLAY_LIST_NOT_MATCH_ITEM.getErrorMessage());
        }

        // 5️⃣ 멤버 검증 (해당 플레이리스트 소유자인지)
        PlayList firstPlayList = playListItems.get(0).getPlayList();
        if (!firstPlayList.getMember().getId().equals(memberId)) {
            throw new PlayListException(
                    PLAY_LIST_AUTH_INVALID.getCode(),
                    PLAY_LIST_AUTH_INVALID.getErrorMessage());
        }
    }
}
