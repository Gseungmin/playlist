package com.naver.playlist.domain.entity.playlist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.naver.playlist.domain.entity.base.BaseEntity;
import com.naver.playlist.domain.entity.music.Music;
import com.naver.playlist.generator.SnowflakeId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayListItem extends BaseEntity {

    @Id @SnowflakeId
    @GeneratedValue
    @Column(name = "playListItemId")
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playListId")
    private PlayList playList;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "musicId")
    private Music music;
}
