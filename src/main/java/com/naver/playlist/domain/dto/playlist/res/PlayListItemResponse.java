package com.naver.playlist.domain.dto.playlist.res;

import com.naver.playlist.domain.entity.playlist.PlayListItem;
import lombok.*;

@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PlayListItemResponse {
    private Long id;
    private Long order;
    private String title;

    public PlayListItemResponse(PlayListItem playListItem) {
        this.id = playListItem.getId();
        this.order = playListItem.getPosition();
        this.title = playListItem.getMusic().getTitle();
    }
}
