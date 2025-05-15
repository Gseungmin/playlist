package com.naver.playlist.domain.dto.playlist.res;

import com.naver.playlist.domain.entity.playlist.PlayListItem;
import lombok.*;

import java.util.List;

import static com.naver.playlist.domain.constant.EntityConstants.MAX_PLAY_PAGE_SIZE;

@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PlayListResponse {
    private List<PlayListItemResponse> playListItems;
    private boolean hasNext;

    public PlayListResponse(List<PlayListItem> dtoList) {
        this.hasNext = dtoList.size() > MAX_PLAY_PAGE_SIZE;

        this.playListItems = dtoList.stream()
                .limit(MAX_PLAY_PAGE_SIZE)
                .map(PlayListItemResponse::new)
                .toList();
    }
}
