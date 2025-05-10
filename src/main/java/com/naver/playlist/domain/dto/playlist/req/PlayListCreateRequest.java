package com.naver.playlist.domain.dto.playlist.req;

import lombok.*;

@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PlayListCreateRequest {
    private String title;
    private String description;
}
