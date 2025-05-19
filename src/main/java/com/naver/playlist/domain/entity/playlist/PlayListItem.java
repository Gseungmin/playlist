package com.naver.playlist.domain.entity.playlist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.naver.playlist.domain.entity.base.BaseEntity;
import com.naver.playlist.domain.entity.music.Music;
import com.naver.playlist.domain.generator.SnowflakeId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_playlist_music",
                        columnNames = { "playListId", "musicId" }
                ),
                @UniqueConstraint(
                        name = "uk_playlist_position",
                        columnNames = { "playListId", "position" }
                )
        }
)
public class PlayListItem extends BaseEntity {

    @Id @SnowflakeId
    @GeneratedValue
    @Column(name = "playListItemId")
    private Long id;

    @Column(nullable = false)
    @Comment("정렬용 숫자 값으로 같은 플레이리스트 내에서 유일해야 한다")
    private Long position;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playListId")
    private PlayList playList;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "musicId")
    private Music music;

    public PlayListItem(Long position, PlayList playList, Music music) {
        this.position = position;
        this.playList = playList;
        this.music = music;
    }
}
