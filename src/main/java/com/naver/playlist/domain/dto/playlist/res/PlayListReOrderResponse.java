package com.naver.playlist.domain.dto.playlist.res;

import com.naver.playlist.domain.dto.playlist.req.ReorderPlayListItemsRequest;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import lombok.*;

import java.util.List;

@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PlayListReOrderResponse {
    private boolean isSuccess;
    private List<ReorderPlayListItemsRequest> playListItems;
}
