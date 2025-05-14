package com.naver.playlist.domain.entity.music;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.naver.playlist.domain.entity.base.BaseEntity;
import com.naver.playlist.domain.entity.playlist.PlayListItem;
import com.naver.playlist.domain.generator.SnowflakeId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static com.naver.playlist.domain.constant.EntityConstants.MAX_MUSIC_TITLE;

@Entity
@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Music extends BaseEntity {

    @Id @SnowflakeId
    @GeneratedValue
    @Column(name = "musicId")
    private Long id;

    @Column(length = MAX_MUSIC_TITLE)
    private String title;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "music")
    private List<PlayListItem> playListItemList = new ArrayList<>();

    public Music(String title) {
        this.title = title;
    }
}
