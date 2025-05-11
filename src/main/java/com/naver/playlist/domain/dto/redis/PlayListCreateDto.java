package com.naver.playlist.domain.dto.redis;

import com.naver.playlist.domain.dto.playlist.req.PlayListCreateRequest;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PlayListCreateDto {
    private Long playlistId;
    private Long memberId;
    private String title;
    private String description;
    private String createdDate;

    public PlayListCreateDto(
            PlayListCreateRequest dto,
            Long playlistId,
            Long memberId)
    {
        this.playlistId = playlistId;
        this.memberId = memberId;
        this.title = dto.getTitle();
        this.description = dto.getDescription();
        this.createdDate = LocalDateTime.now().toString();
    }
}
