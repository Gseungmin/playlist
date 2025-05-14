package com.naver.playlist.domain.dto.projection;

import com.naver.playlist.domain.entity.playlist.PlayList;

public interface PlayListStatProjection {
    PlayList getPlayList();
    Long getCount();
    Long getLastOrder();
}