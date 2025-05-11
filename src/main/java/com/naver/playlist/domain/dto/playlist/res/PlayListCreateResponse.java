package com.naver.playlist.domain.dto.playlist.res;

import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import lombok.*;

import java.util.List;

@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PlayListCreateResponse {
    private String key;
    private List<PlayListCreateDto> playList;
}
